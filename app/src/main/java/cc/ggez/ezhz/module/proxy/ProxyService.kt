package cc.ggez.ezhz.module.proxy

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import cc.ggez.ezhz.MainActivity
import cc.ggez.ezhz.R
import cc.ggez.ezhz.module.frida.FridaService
import com.topjohnwu.superuser.Shell
import java.lang.ref.WeakReference

class ProxyService : Service() {

    companion object {
        private const val TAG = "ProxyService"

        private const val MSG_CONNECT_START = 0
        private const val MSG_CONNECT_FINISH = 1
        private const val MSG_CONNECT_SUCCESS = 2
        private const val MSG_CONNECT_FAIL = 3
        private const val MSG_CONNECT_PAC_ERROR = 4
        private const val MSG_CONNECT_RESOLVE_ERROR = 5

        const val CMD_IPTABLES_RETURN = "iptables -t nat -A OUTPUT -p tcp -d 0.0.0.0 -j RETURN\n"

        const val CMD_IPTABLES_REDIRECT_ADD_HTTP =
            ("iptables -t nat -A OUTPUT -p tcp --dport 80 -j REDIRECT --to 8123\n"
                    + "iptables -t nat -A OUTPUT -p tcp --dport 443 -j REDIRECT --to 8123\n"
                    + "iptables -t nat -A OUTPUT -p tcp --dport 8443 -j REDIRECT --to 8123\n"
                    + "iptables -t nat -A OUTPUT -p tcp --dport 5228 -j REDIRECT --to 8123\n"
                    + "iptables -t nat -A OUTPUT -p udp --dport 443 -j REDIRECT --to 8124\n"
                    + "iptables -t nat -A OUTPUT -p udp --dport 8443 -j REDIRECT --to 8124\n"
                    + "iptables -t nat -A OUTPUT -p udp --dport 5228 -j REDIRECT --to 8124\n")

        const val CMD_IPTABLES_DNAT_ADD_HTTP =
            ("iptables -t nat -A OUTPUT -p tcp --dport 80 -j DNAT --to-destination 127.0.0.1:8123\n"
                    + "iptables -t nat -A OUTPUT -p tcp --dport 443 -j DNAT --to-destination 127.0.0.1:8123\n"
                    + "iptables -t nat -A OUTPUT -p tcp --dport 8443 -j DNAT --to-destination 127.0.0.1:8123\n"
                    + "iptables -t nat -A OUTPUT -p tcp --dport 5228 -j DNAT --to-destination 127.0.0.1:8123\n"
                    + "iptables -t nat -A OUTPUT -p udp --dport 443 -j DNAT --to-destination 127.0.0.1:8124\n"
                    + "iptables -t nat -A OUTPUT -p udp --dport 8443 -j DNAT --to-destination 127.0.0.1:8124\n"
                    + "iptables -t nat -A OUTPUT -p udp --dport 5228 -j DNAT --to-destination 127.0.0.1:8124\n")

        const val CMD_IPTABLES_REDIRECT_ADD_SOCKS =
            "iptables -t nat -A OUTPUT -p tcp -j REDIRECT --to 8123\n"

        const val CMD_IPTABLES_DNAT_ADD_SOCKS =
            "iptables -t nat -A OUTPUT -p tcp -j DNAT --to-destination 127.0.0.1:8123\n"

        private var sRunningInstance: WeakReference<ProxyService>? = null
        fun isServiceStarted(): Boolean {
            val isServiceStarted: Boolean
            if (sRunningInstance == null) {
                isServiceStarted = false
            } else if (sRunningInstance!!.get() == null) {
                isServiceStarted = false
                sRunningInstance = null
            } else {
                isServiceStarted = true
            }
            return isServiceStarted
        }
    }

    lateinit var basePath: String
    private val notificationManager: NotificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }
    private lateinit var settings: SharedPreferences
    private lateinit var pendIntent: PendingIntent
    private lateinit var pStopSelf: PendingIntent
    private lateinit var proxyProfile: ProxyProfile

    val handler = Handler(Looper.getMainLooper()) { msg ->
        val ed = settings.edit()
        when (msg.what) {
            MSG_CONNECT_START -> {
                ed.putBoolean("is_connecting", true)
                ProxySingleton.isConnecting = true
            }

            MSG_CONNECT_FINISH -> {
                ed.putBoolean("is_connecting", false)
                ProxySingleton.isConnecting = false
            }

            MSG_CONNECT_SUCCESS -> ed.putBoolean("proxy_running", true)
            MSG_CONNECT_FAIL -> ed.putBoolean("proxy_running", false)
            MSG_CONNECT_PAC_ERROR -> Toast.makeText(
                this@ProxyService,
                R.string.msg_pac_error,
                Toast.LENGTH_SHORT
            ).show()

            MSG_CONNECT_RESOLVE_ERROR -> Toast.makeText(
                this@ProxyService, R.string.msg_resolve_error,
                Toast.LENGTH_SHORT
            ).show()
        }
        ed.apply()
        true
    }

    private fun markServiceStarted() {
        sRunningInstance = WeakReference(this)
    }

    private fun markServiceStopped() {
        sRunningInstance = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        Log.d(TAG, "Service Proxy Create");
        super.onCreate()
        basePath = filesDir.absolutePath;
        settings = PreferenceManager.getDefaultSharedPreferences(this)

        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra("menu", "proxy")
        pendIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val stopIntent = Intent(this, ProxyService::class.java)
        stopIntent.action = "STOP"
        pStopSelf = PendingIntent.getForegroundService(this, 0, stopIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        notifyAlert(
            getString(R.string.app_name) + " | Proxy Module",
            getString(R.string.service_running)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("xxx", intent?.action.toString())
        if(intent?.action.equals("STOP")) {
            stopSelf()
            return super.onStartCommand(intent, flags, startId)
        }

        if (intent?.extras == null) {
            return super.onStartCommand(intent, flags, startId)
        }

        Log.d(TAG, "Service Start");

        proxyProfile = ProxyProfile.fromBundle(intent.extras!!)

        Thread {
            handler.sendEmptyMessage(MSG_CONNECT_START)
            if (handleCommand()) {
                // Connection and forward successful
                handler.sendEmptyMessage(MSG_CONNECT_SUCCESS)
            } else {
                // Connection or forward unsuccessful
                stopSelf()
                handler.sendEmptyMessage(MSG_CONNECT_FAIL)
            }
            handler.sendEmptyMessage(MSG_CONNECT_FINISH)
        }.start()

        markServiceStarted()

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Log.d(TAG, "Service Stop");
        ProxySingleton.isConnecting = true
        notificationManager.cancelAll();
        stopForeground(STOP_FOREGROUND_DETACH);
        super.onDestroy()

        onDisconnect()

        val ed: Editor = settings.edit()
        ed.putBoolean("proxy_running", false)
        ed.apply()

        markServiceStopped()
        ProxySingleton.isConnecting = false
    }

    private fun onDisconnect() {
        val sb = StringBuilder()
        sb.append("iptables -t nat -F OUTPUT\n")
        sb.append("kill -9 `cat ${basePath}/gost_tcp.pid`\n")
        sb.append("kill -9 `cat ${basePath}/gost_udp.pid`\n")
        sb.append("kill -9 `cat ${basePath}/gost_dns.pid`\n")
        Shell.cmd(sb.toString()).exec()
    }

    private fun handleCommand(): Boolean {
        Shell.cmd("chmod +x $basePath/gost").exec()

        try {
            val u: String = ProxySingleton.preserve(proxyProfile.username)
            val p: String = ProxySingleton.preserve(proxyProfile.password)
            val srcTcp = "-L=red://127.0.0.1:8123?sniffing=true"
            val srcUdp = "-L=redu://127.0.0.1:8124?ttl=30s"
            val srcDns = "-L=dns://:53/${proxyProfile.dns}"
            var auth = ""
            if (u.isNotEmpty() && p.isNotEmpty()) {
                auth = "$u:$p"
            }
            val dstTcp = "-F=${proxyProfile.proxyType}://$auth@${proxyProfile.host}:${proxyProfile.port}"
            val dstUdp = "-F=relay://${proxyProfile.host}:${proxyProfile.port}"

            // Start gost tcp here
            Shell.cmd("$basePath/gost $srcTcp $dstTcp &> $basePath/gost_tcp.log &\n echo $! > $basePath/gost_tcp.pid").exec()
            Shell.cmd("$basePath/gost $srcUdp $dstUdp &> $basePath/gost_udp.log &\n echo $! > $basePath/gost_udp.pid").exec()
            Shell.cmd("$basePath/gost $srcDns &> $basePath/gost_dns.log &\n echo $! > $basePath/gost_dns.pid").exec()

            val cmd = StringBuilder()
            cmd.append(CMD_IPTABLES_RETURN.replace("0.0.0.0", proxyProfile.host))

            var redirectCmd = CMD_IPTABLES_REDIRECT_ADD_HTTP
            var dnatCmd = CMD_IPTABLES_DNAT_ADD_HTTP

            if (proxyProfile.proxyType.equals("socks4") || proxyProfile.proxyType.equals("socks5")) {
                redirectCmd = CMD_IPTABLES_REDIRECT_ADD_SOCKS
                dnatCmd = CMD_IPTABLES_DNAT_ADD_SOCKS
            }

            if (proxyProfile.isTargetGlobal) {
                cmd.append(if (ProxySingleton.isRedirectSupport == 1) redirectCmd else dnatCmd)
            }
            else if (proxyProfile.isTargetBypassMode) {
                // for host specified apps
                for (app in proxyProfile.targetApps) {
                    val appData = app.split(":")
                    cmd.append(
                        CMD_IPTABLES_RETURN.replace("-d 0.0.0.0", "").replace(
                            "-t nat",
                            "-t nat -m owner --uid-owner ${appData[1]}"
                        )
                    )
                }

                cmd.append(if (ProxySingleton.isRedirectSupport == 1) redirectCmd else dnatCmd)
            } else {
                for (app in proxyProfile.targetApps) {
                    val appData = app.split(":")
                    cmd.append(
                        (if (ProxySingleton.isRedirectSupport == 1) redirectCmd else dnatCmd).replace(
                            "-t nat",
                            "-t nat -m owner --uid-owner ${appData[1]}"
                        )
                    )
                }
            }

            Shell.cmd(cmd.toString()).exec()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up port forward during connect", e)
        }

        return true
    }

    private fun createNotificationChannel() {
        val name: CharSequence = "EZHZ Proxy Service"
        val description = "EZHZ Proxy Background Service"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("EZHZ Proxy Service", name, importance)
        channel.description = description
        notificationManager.createNotificationChannel(channel)
    }

    private fun initSoundVibrateLights(builder: NotificationCompat.Builder) {
        val audioManager = this.getSystemService(AUDIO_SERVICE) as AudioManager
        if (audioManager.getStreamVolume(AudioManager.STREAM_RING) == 0) {
            builder.setSound(null)
        }

        builder.setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
    }

    private fun notifyAlert(title: String, info: String) {
        val notiTitle = "${getString(R.string.app_name)} | Proxy Module"
        val builder = NotificationCompat.Builder(this, "Service")
        initSoundVibrateLights(builder)
        builder.setAutoCancel(false)
        builder.setTicker(notiTitle)
        builder.setContentTitle(title)
        builder.setContentText(info)
        builder.setSmallIcon(R.drawable.ic_proxy)
        builder.setContentIntent(pendIntent)
        builder.addAction(
            android.R.drawable.ic_lock_power_off,
            getString(R.string.service_stop),
            pStopSelf)
        builder.priority = NotificationCompat.PRIORITY_DEFAULT
        builder.setOngoing(true)
        builder.setChannelId("EZHZ Proxy Service")

        startForeground(1, builder.build())
    }
}