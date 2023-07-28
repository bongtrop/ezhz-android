package cc.ggez.ezhz.ui.appselect

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import java.util.Arrays
import java.util.StringTokenizer
import java.util.Vector


class AppSelectService {
    companion object {
        fun getApps(context: Context) {

            val pm = context.packageManager
            val appInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                pm.getInstalledApplications(0)
            }

            val itAppInfo: Iterator<ApplicationInfo> = lAppInfo.iterator()
            var aInfo: ApplicationInfo
            while (itAppInfo.hasNext()) {
                aInfo = itAppInfo.next()

                // ignore system apps
                if (aInfo.uid < 10000) continue
                if (aInfo.processName == null) continue
                if (pMgr.getApplicationLabel(aInfo) == null || pMgr.getApplicationLabel(aInfo)
                        .toString() == ""
                ) continue
                if (pMgr.getApplicationIcon(aInfo) == null) continue
                val tApp = ProxyedApp()
                tApp.setEnabled(aInfo.enabled)
                tApp.setUid(aInfo.uid)
                tApp.setUsername(pMgr.getNameForUid(tApp.getUid()))
                tApp.setProcname(aInfo.processName)
                tApp.setName(pMgr.getApplicationLabel(aInfo).toString())

                // check if this application is allowed
                tApp.setProxyed(Arrays.binarySearch(tordApps, tApp.getUsername()) >= 0)
                vectorApps.add(tApp)
            }
            apps = arrayOfNulls<ProxyedApp>(vectorApps.size)
            vectorApps.toArray(apps)
        }
    }
}