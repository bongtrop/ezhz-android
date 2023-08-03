package cc.ggez.ezhz.module.proxy

import com.topjohnwu.superuser.Shell


class ProxySingleton {
    companion object {
        var isConnecting = false
        var isRedirectSupport = -1
            get() {
                if (field == -1) initHasRedirectSupported()
                return field
            }

        fun initHasRedirectSupported() {
            val sb = StringBuilder()
            val command = "iptables -t nat -A OUTPUT -p udp --dport 54 -j REDIRECT --to 8154"
            val result = Shell.cmd(command).exec()
            val lines = result.out + result.err
            isRedirectSupport = 1

            // flush the check command
            Shell.cmd(command.replace("-A", "-D"))

            if (lines.contains("No chain/target/match")) {
                isRedirectSupport = 0
            }
        }

        fun preserve(s: String): String {
            val sb = java.lang.StringBuilder()
            for (element in s) {
                if (element == '\\' || element == '$' || element == '`' || element == '"') sb.append('\\')
                sb.append(element)
            }
            return sb.toString()
        }
    }
}