package cc.ggez.ezhz.module.proxy

import android.content.SharedPreferences
import android.os.Bundle
import org.json.JSONObject
import java.io.Serializable
import java.net.InetAddress
import java.util.Vector
import java.util.regex.Pattern


class ProxyProfile : Serializable {
    var isProxyRunning: Boolean = false

    var isProxyAuto: Boolean = false
    var pacUrl: String
    var host: String
    var port: Int = 0
    var proxyType: String
    var dns: String

    var isAuth: Boolean = false
    var username: String
    var password: String

    var isTargetGlobal: Boolean = true
    var targetApps: ArrayList<String>
    var isTargetBypassMode: Boolean = false

    companion object {
        fun fromSharedPref(settings: SharedPreferences): ProxyProfile {
            val proxyProfile = ProxyProfile()

            proxyProfile.isProxyRunning = settings.getBoolean("proxy_running", false)

            proxyProfile.isProxyAuto = settings.getBoolean("proxy_auto", false)
            proxyProfile.pacUrl = settings.getString("proxy_pac_url", "").toString()
            proxyProfile.host = settings.getString("proxy_host", "127.0.0.1").toString()
            proxyProfile.port = settings.getString("proxy_port", "1337").toString().toInt()
            proxyProfile.proxyType = settings.getString("proxy_type", "http").toString()
            proxyProfile.dns = settings.getString("proxy_dns", "1.1.1.1").toString()
            proxyProfile.isAuth = settings.getBoolean("auth_enable", false)
            proxyProfile.username = settings.getString("auth_username", "").toString()
            proxyProfile.password = settings.getString("auth_password", "").toString()

            proxyProfile.isTargetGlobal = settings.getBoolean("target_global", false)
            proxyProfile.targetApps = ArrayList(settings.getStringSet("target_apps",  setOf<String>())?: setOf())
            proxyProfile.isTargetBypassMode = settings.getBoolean("target_bypass_mode", false)

            return proxyProfile
        }

        fun fromBundle(bundle: Bundle): ProxyProfile {
            val proxyProfile = ProxyProfile()

            proxyProfile.isProxyRunning = bundle.getBoolean("proxy_running", false)

            proxyProfile.isProxyAuto = bundle.getBoolean("proxy_auto", false)
            proxyProfile.pacUrl = bundle.getString("proxy_pac_url", "").toString()
            proxyProfile.host = bundle.getString("proxy_host", "127.0.0.1").toString()
            proxyProfile.port = bundle.getInt("proxy_port", 1337)
            proxyProfile.proxyType = bundle.getString("proxy_type", "http").toString()
            proxyProfile.dns = bundle.getString("proxy_dns", "1.1.1.1").toString()

            proxyProfile.isAuth = bundle.getBoolean("auth_enable", false)
            proxyProfile.username = bundle.getString("auth_username", "").toString()
            proxyProfile.password = bundle.getString("auth_password", "").toString()

            proxyProfile.isTargetGlobal = bundle.getBoolean("target_global", false)
            proxyProfile.targetApps = bundle.getStringArrayList("target_apps") ?: ArrayList()
            proxyProfile.isTargetBypassMode = bundle.getBoolean("target_bypass_mode", false)

            return proxyProfile
        }

        fun validateAddr(ia: String?): String? {
            val valid1: Boolean = Pattern.matches(
                "[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}/[0-9]{1,2}",
                ia
            )
            val valid2: Boolean = Pattern.matches(
                "[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}", ia
            )
            return if (valid1 || valid2) {
                ia
            } else {
                var addrString: String? = null
                try {
                    val addr = InetAddress.getByName(ia)
                    addrString = addr.hostAddress
                } catch (ignore: Exception) {
                }
                if (addrString != null) {
                    val valid3: Boolean = Pattern.matches(
                        "[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}",
                        addrString
                    )
                    if (!valid3) addrString = null
                }
                addrString
            }
        }

        fun decodeAddrs(addrs: String): Array<String> {
            val list = addrs.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            val ret = Vector<String>()
            for (addr in list) {
                val ta = validateAddr(addr)
                if (ta != null) ret.add(ta)
            }
            return ret.toTypedArray()
        }

        fun encodeAddrs(addrs: Array<String?>): String {
            if (addrs.size == 0) return ""
            val sb = StringBuilder()
            for (addr in addrs) {
                val ta = validateAddr(addr)
                if (ta != null) sb.append(ta).append("|")
            }
            return sb.substring(0, sb.length - 1)
        }
    }

    init {
        isProxyRunning = false

        isProxyAuto = false
        pacUrl = ""
        host = "127.0.0.1"
        port = 1337
        proxyType = "http"
        dns = "1.1.1.1"

        isAuth = false
        username = ""
        password = ""

        isTargetGlobal = true
        targetApps = ArrayList()
        isTargetBypassMode = false
    }

    fun toBundle(): Bundle {
        val bundle = Bundle()

        bundle.putBoolean("proxy_running", isProxyRunning)

        bundle.putBoolean("proxy_auto", isProxyAuto)
        bundle.putString("proxy_pac_url", pacUrl)
        bundle.putString("proxy_host", host)
        bundle.putInt("proxy_port", port)
        bundle.putString("proxy_type", proxyType)
        bundle.putString("proxy_dns", dns)

        bundle.putBoolean("auth_enable", isAuth)
        bundle.putString("auth_username", username)
        bundle.putString("auth_password", password)

        bundle.putBoolean("target_global", isTargetGlobal)
        bundle.putStringArrayList("target_apps", targetApps)
        bundle.putBoolean("target_bypass_mode", isTargetBypassMode)

        return bundle
    }

    fun toSharedPref(settings: SharedPreferences) {
        val ed = settings.edit()

        ed.putBoolean("proxy_running", isProxyRunning)

        ed.putBoolean("proxy_auto", isProxyAuto)
        ed.putString("proxy_pac_url", pacUrl)
        ed.putString("proxy_host", host)
        ed.putString("proxy_port", port.toString())
        ed.putString("proxy_type", proxyType)
        ed.putString("proxy_dns", dns)

        ed.putBoolean("auth_enable", isAuth)
        ed.putString("auth_username", username)
        ed.putString("auth_password", password)

        ed.putBoolean("target_global", isTargetGlobal)
        ed.putBoolean("target_bypass_mode", isTargetBypassMode)
        ed.putStringSet("target_apps", targetApps.toSet())
        ed.apply()
    }

    override fun toString(): String {
        return toJson().toString()
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()

        obj.put("proxy_running", isProxyRunning)

        obj.put("proxy_auto", isProxyAuto)
        obj.put("proxy_pac_url", pacUrl)
        obj.put("proxy_host", host)
        obj.put("proxy_port", port)
        obj.put("proxy_type", proxyType)
        obj.put("proxy_dns", dns)

        obj.put("auth_enable", isAuth)
        obj.put("auth_username", username)
        obj.put("auth_password", password)

        obj.put("target_global", isTargetGlobal)
        obj.put("target_apps", targetApps)
        obj.put("target_bypass_mode", isTargetBypassMode)

        return obj
    }

}