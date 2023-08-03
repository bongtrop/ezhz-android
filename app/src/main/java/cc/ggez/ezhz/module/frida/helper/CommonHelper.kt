package cc.ggez.ezhz.module.frida.helper

import com.topjohnwu.superuser.Shell
import java.io.File

class CommonHelper {

    companion object {
        fun getArchType(): String {
            val archType = Shell.cmd("getprop ro.product.cpu.abi").exec().out[0]
            return when (archType) {
                "x86" -> "x86"
                "arm64-v8a" -> "arm64"
                "x86_64" -> "x86_64"
                "armeabi-v7a" -> "arm"
                "armeabi" -> "arm"
                else -> "arm64"
            }
        }
    }
}