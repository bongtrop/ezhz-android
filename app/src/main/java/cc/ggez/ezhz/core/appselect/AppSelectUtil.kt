package cc.ggez.ezhz.core.appselect

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build


class AppSelectUtil {
    companion object {
        fun getApps(context: Context): ArrayList<AppObject> {
            val pm = context.packageManager
            val appInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                pm.getInstalledApplications(0)
            }

            return ArrayList(appInfos.map {
                AppObject(
                    name = pm.getApplicationLabel(it).toString(),
                    packageName = it.packageName,
                    icon = pm.getApplicationIcon(it),
                    enabled = it.enabled,
                    system = (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    uid = it.uid,
                    username = pm.getNameForUid(it.uid),
                    procname = it.processName,
                    proxyed = false
                )
            })
        }
    }
}