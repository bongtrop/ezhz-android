package cc.ggez.ezhz.module.frida

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import cc.ggez.ezhz.module.frida.helper.FridaHelper.Companion.checkFridaServerProcessTag
import cc.ggez.ezhz.module.frida.helper.FridaHelper.Companion.getDownloadedFridaTags
import cc.ggez.ezhz.module.frida.helper.FridaHelper.Companion.removeFridaServer
import cc.ggez.ezhz.module.frida.helper.FridaHelper.Companion.stopFridaServer
import cc.ggez.ezhz.module.frida.helper.GithubHelper
import cc.ggez.ezhz.module.frida.model.FridaItem
import cc.ggez.ezhz.module.frida.model.FridaItemState
import cc.ggez.ezhz.module.frida.model.GithubTag
import com.ixuea.android.downloader.DownloadService
import com.ixuea.android.downloader.callback.DownloadListener
import com.ixuea.android.downloader.domain.DownloadInfo
import com.ixuea.android.downloader.exception.DownloadException
import org.tukaani.xz.XZInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import kotlin.reflect.KFunction0

class FridaViewModel(application: Application) : AndroidViewModel(application) {

    val TAG = "FridaViewModel"

    val fridaItemList = MutableLiveData<List<FridaItem>>()
    val installProgress = MutableLiveData(-1)
    val errorMessage = MutableLiveData<String>()
    val cacheDir: File = getApplication<Application>().cacheDir
    var fridaServerDir = "${getApplication<Application>().filesDir.absolutePath}/server"
    val downloadManager = DownloadService.getDownloadManager(getApplication<Application>().applicationContext);

    private fun checkCacheTags(): Boolean {
        val file = File(cacheDir, "frida_tags.json")
        return file.exists()
    }

    private fun getCacheTags(): String {
        val file = File(cacheDir, "frida_tags.json")
        return file.readText()
    }

    private fun cacheTags(tags: String) {
        val file = File(cacheDir, "frida_tags.json")
        file.writeText(tags)
    }

    private fun fridaItemsFromTags(tags: List<GithubTag>): List<FridaItem> {
        val installedTags = getDownloadedFridaTags(fridaServerDir)
        val executedTag = checkFridaServerProcessTag()

        Log.d("installedTags", installedTags.toString())

        val fridaItems = tags.map {
            val state = if (executedTag == it.name) {
                FridaItemState.EXECUTING
            }
            else if (installedTags.contains(it.name)) {
                FridaItemState.INSTALLED
            } else {
                FridaItemState.NOT_INSTALL
            }

            FridaItem(it, state)
        }

        return fridaItems.sortedWith(Comparator{ a, b ->
            if (a.state < b.state) {
                return@Comparator 1
            }
            else if (a.state > b.state) {
                return@Comparator -1
            }

            val version1 = b.tag.name
            val version2 = a.tag.name

            val parts1 = version1.split(".").map { it.toInt() }
            val parts2 = version2.split(".").map { it.toInt() }

            val maxLen = maxOf(parts1.size, parts2.size)

            for (i in 0 until maxLen) {
                val p1 = parts1.getOrElse(i) { 0 } // Handle missing parts as 0
                val p2 = parts2.getOrElse(i) { 0 }

                if (p1 < p2) return@Comparator -1
                if (p1 > p2) return@Comparator 1
            }

            return@Comparator 0
        })
    }

    fun getAllFridaItems(forceReload: Boolean = false, reloadDone: KFunction0<Unit>) {
        if (forceReload || !checkCacheTags()) {
            GithubHelper.fetchFridaTags("frida", "frida", 0, object: GithubHelper.GithubTagsCallback {
                override fun onSuccess(resJson: String) {
                    cacheTags(resJson)
                    val tags = GithubHelper.translateTags(getCacheTags())
                    fridaItemList.postValue(fridaItemsFromTags(tags))
                    reloadDone()
                }

                override fun onFailure(e: IOException) {
                    errorMessage.postValue(e.message)
                    reloadDone()
                }
            })
        } else {
            val tags = GithubHelper.translateTags(getCacheTags())
            fridaItemList.postValue(fridaItemsFromTags(tags))
        }
    }

    fun executeFridaServer(fridaItem: FridaItem) {
        try {
            val it = Intent(getApplication(), FridaService::class.java)
            val bundle = Bundle()
            bundle.putString("tag", fridaItem.tag.name)
            it.putExtras(bundle)
            getApplication<Application>().startForegroundService(it)
        } catch (e: Exception) {
            Log.e(TAG, "serviceStart: ${e.message}")
        }

        val fridaItems = fridaItemList.value!!
        fridaItems.map { item ->
            if (item.tag.name == fridaItem.tag.name) {
                item.state = FridaItemState.EXECUTING
            }
        }
        fridaItemList.postValue(fridaItems.toMutableList())
    }

    fun killFridaServer(fridaItem: FridaItem) {
        try {
            getApplication<Application>().stopService(Intent(getApplication(), FridaService::class.java))
            stopFridaServer()
        } catch (e: Exception) {
            Log.e(TAG, "serviceStop: ${e.message}")
        }
        val fridaItems = fridaItemList.value!!
        fridaItems.map { item ->
            if (item.tag.name == fridaItem.tag.name) {
                item.state = FridaItemState.INSTALLED
            }
        }
        fridaItemList.postValue(fridaItems.toMutableList())
    }

    fun installFridaServer(fridaItem: FridaItem) {
        installProgress.postValue(0)
        Log.d(TAG, "[+] Install Start ${fridaItem.tag.name}")
        GithubHelper.fetchFridaRelease("frida", "frida", fridaItem.tag.name, object: GithubHelper.GithubReleaseCallback {
            override fun onSuccess(resJson: String) {
                val release = GithubHelper.translateRelease(resJson)
                val downloadUrl = GithubHelper.downloadUrlFromRelease(release)
                Log.d(TAG, "[+] Download Start $downloadUrl")
                val downloadInfo = DownloadInfo.Builder().setUrl(downloadUrl)
                    .setPath("${cacheDir.absolutePath}/frida-server-${fridaItem.tag.name}.xz")
                    .build()

                downloadInfo.downloadListener = object : DownloadListener {
                    override fun onStart() {
                        installProgress.postValue(0)
                    }

                    override fun onWaited() {
                    }

                    override fun onPaused() {
                    }

                    override fun onDownloading(progress: Long, size: Long) {
                        installProgress.postValue((progress * 100 / size).toInt())
                    }

                    override fun onRemoved() {
                        installProgress.postValue(-1)
                    }

                    override fun onDownloadSuccess() {
                        installProgress.postValue(-1)
                        Log.d(TAG, "[+] Download Finish")
                        val fridaServerDir = File(fridaServerDir)
                        if (!fridaServerDir.exists()) {
                            fridaServerDir.mkdir()
                        }

                        Log.d(TAG, "[+] Uncompressing XZ")
                        try {
                            val fin =
                                FileInputStream(
                                    File(
                                        cacheDir,
                                        "frida-server-${fridaItem.tag.name}.xz"
                                    )
                                )
                            val inXZ = BufferedInputStream(fin)
                            val outXZ =
                                FileOutputStream(
                                    File(
                                        fridaServerDir,
                                        "frida-server-${fridaItem.tag.name}"
                                    )
                                )

                            val xzIn = XZInputStream(inXZ)
                            val buffer = ByteArray(8192)

                            var n = 0
                            while (-1 != xzIn.read(buffer).also { n = it }) {
                                outXZ.write(buffer, 0, n)
                            }
                            xzIn.close()
                            fin.close()
                            outXZ.close()
                            val fridaItems = fridaItemList.value!!
                            fridaItems.map { item ->
                                if (item.tag.name == fridaItem.tag.name) {
                                    item.state = FridaItemState.INSTALLED
                                }
                            }
                            fridaItemList.postValue(fridaItems.toMutableList())
                        } catch (e: IOException) {
                            Log.e(TAG, "Uncompress XZ Error: ${e.message}")
                        }

                        val cacheXZ = File(cacheDir, "frida-server-${fridaItem.tag.name}.xz")
                        if (cacheXZ.exists()) {
                            cacheXZ.delete()
                        }
                    }

                    override fun onDownloadFailed(e: DownloadException?) {
                        installProgress.postValue(-1)
                        errorMessage.postValue("Frida server download failed: ${e?.message}")
                    }

                }

                downloadManager.download(downloadInfo)
            }

            override fun onFailure(e: IOException) {
                Log.d(TAG, e.message.toString())
                errorMessage.postValue(e.message)
            }
        })
    }

    fun uninstallFridaServer(fridaItem: FridaItem) {
        removeFridaServer(fridaServerDir, fridaItem.tag.name)

        val fridaItems = fridaItemList.value!!
        fridaItems.map { item ->
            if (item.tag.name == fridaItem.tag.name) {
                item.state = FridaItemState.NOT_INSTALL
            }
        }
        fridaItemList.postValue(fridaItems.toMutableList())
    }
}