package cc.ggez.ezhz.module.frida

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.telephony.mbms.StreamingService
import android.util.Log
import androidx.core.app.NotificationCompat
import cc.ggez.ezhz.MainActivity
import cc.ggez.ezhz.R
import cc.ggez.ezhz.module.frida.helper.FridaHelper
import java.io.File


class FridaService : Service() {
    val TAG = "FridaService"
    lateinit var fridaServerDir: String
    private val notificationManager: NotificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }
    private lateinit var pendIntent: PendingIntent
    private lateinit var pStopSelf: PendingIntent
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        Log.d(TAG, "Service Frida Create");
        super.onCreate()
        fridaServerDir = "${filesDir.absolutePath}/server"
        createNotificationChannel()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra("menu", "frida")
        pendIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        } else {
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val stopIntent = Intent(this, FridaService::class.java)
        stopIntent.action = "STOP"
        pStopSelf = PendingIntent.getForegroundService(this, 0, stopIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        notifyAlert(
            getString(R.string.app_name) + " | Frida Module",
            getString(R.string.service_running)
        )
    }

    private fun handlerCommand(tag: String): Boolean {
        if (File("$fridaServerDir/frida-server-$tag").exists()) {
            FridaHelper.startFridaServer(fridaServerDir, tag)
            return true
        }
        return false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent?.action.equals("STOP")) {
            stopSelf()
            return super.onStartCommand(intent, flags, startId)
        }
        val tag = intent?.getStringExtra("tag")
        Thread {
            if (tag == null || !handlerCommand(tag)) {
                stopSelf()
            }
        }.start()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        FridaHelper.stopFridaServer()
        notificationManager.cancelAll();
        stopForeground(STOP_FOREGROUND_DETACH);
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val name: CharSequence = "EZHZ Frida Service"
        val description = "EZHZ Frida Background Service"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("EZHZ Frida Service", name, importance)
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
        val notiTitle = "${getString(R.string.app_name)} | Frida Module"
        val builder = NotificationCompat.Builder(this, "Service")
        initSoundVibrateLights(builder)
        builder.setAutoCancel(false)
        builder.setTicker(notiTitle)
        builder.setContentTitle(title)
        builder.setContentText(info)
        builder.setSmallIcon(R.drawable.ic_debugging)
        builder.setContentIntent(pendIntent)
        builder.addAction(
            android.R.drawable.ic_lock_power_off,
            getString(R.string.service_stop),
            pStopSelf)
        builder.priority = NotificationCompat.PRIORITY_DEFAULT
        builder.setOngoing(true)
        builder.setChannelId("EZHZ Frida Service")

        startForeground(2, builder.build())
    }
}