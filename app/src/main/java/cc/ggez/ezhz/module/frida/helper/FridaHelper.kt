package cc.ggez.ezhz.module.frida.helper

import android.util.Log
import cc.ggez.ezhz.module.frida.model.GithubRelease
import cc.ggez.ezhz.module.frida.model.GithubTag
import com.google.gson.Gson
import com.topjohnwu.superuser.Shell
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class FridaHelper {
    companion object {
        val archType: String = CommonHelper.getArchType()

        fun checkFridaServerProcessTag(): String {
            val stdout: List<String> = ArrayList()
            Shell.cmd("ps -A | grep -v grep | grep ggez-server | awk '{print \$9}'").to(stdout).exec()
            if (stdout.isNotEmpty()) {
                return stdout[0].replace(   "ggez-server-", "")
            }
            return ""
        }

        fun checkFridaServerProcessId(): String {
            val stdout: List<String> = ArrayList()
            Shell.cmd("ps -A | grep -v grep | grep ggez-server | awk '{print \$2}'").to(stdout).exec()
            if (stdout.isNotEmpty()) {
                return stdout[0].replace(   "ggez-server-", "")
            }
            return ""
        }

        fun startFridaServer(serverPath: String, tag: String) {
            Shell.cmd("cp ${serverPath}/frida-server-${tag} /data/local/tmp/ggez-server-${tag}",
                "chmod +x /data/local/tmp/ggez-server-${tag}",
                "/data/local/tmp/ggez-server-${tag} &"
            ).submit()
        }

        fun stopFridaServer() {
            val pid = checkFridaServerProcessId()
            if (pid.isNotEmpty()) Shell.cmd("kill -9 ${pid}").exec()
        }

        fun getDownloadedFridaTags(path: String): List<String> {
            val serverDir = File(path)
            val files = serverDir.listFiles()
            return files?.map { it.name.replace("frida-server-", "") } ?: emptyList()
        }

        fun removeFridaServer(path: String, tag: String) {
            val serverDir = File(path)
            val files = serverDir.listFiles()
            files?.forEach {
                if (it.name.contains(tag)) {
                    it.delete()
                }
            }
        }
    }
}