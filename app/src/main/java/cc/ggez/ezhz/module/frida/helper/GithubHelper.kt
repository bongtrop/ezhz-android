package cc.ggez.ezhz.module.frida.helper

import android.util.Log
import cc.ggez.ezhz.module.frida.model.GithubRelease
import cc.ggez.ezhz.module.frida.model.GithubTag
import com.google.gson.Gson
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

class GithubHelper {
    interface GithubTagsCallback {
        fun onSuccess(resJson: String)
        fun onFailure(e: IOException)
    }

    interface GithubReleaseCallback {
        fun onSuccess(resJson: String)
        fun onFailure(e: IOException)
    }
    companion object {

        const val TAG = "FridaGithubHelper"
        const val fridaGithubEndpoint = "https://api.github.com/repos"
        val archType = CommonHelper.getArchType()

        private val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        fun fetchFridaTags(owner: String, repo: String, page: Int, callback: GithubTagsCallback) {
            val urlBuilder: HttpUrl.Builder =
                ("$fridaGithubEndpoint/$owner/$repo/tags").toHttpUrl().newBuilder()

            urlBuilder.addQueryParameter("per_page", "100")
            urlBuilder.addQueryParameter("page", page.toString())
            val url = urlBuilder.build().toString()

            val request = Request.Builder()
                .url(url)
                .build()

            val call = client.newCall(request)
            call.enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "${e.message}")
                    callback.onFailure(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val resJson = response.body!!.string()
                    callback.onSuccess(resJson)
                }
            })
        }

        fun fetchFridaRelease(owner: String, repo: String, tag: String, callback: GithubReleaseCallback) {
            val urlBuilder: HttpUrl.Builder =
                ("$fridaGithubEndpoint/$owner/$repo/releases/tags/$tag").toHttpUrl().newBuilder()

            val url = urlBuilder.build().toString()
            Log.d(TAG, url)

            val request: Request = Request.Builder()
                .url(url)
                .build()

            val call = client.newCall(request)
            call.enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "${e.message}")
                    callback.onFailure(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val resJson = response.body!!.string()
                    callback.onSuccess(resJson)
                }
            })
        }

        fun translateTags(resJson: String): List<GithubTag> {
            val gson = Gson()
            val tags = gson.fromJson(resJson, Array<GithubTag>::class.java)
            return ArrayList(tags.toList())
        }

        fun translateRelease(resJson: String): GithubRelease {
            val gson = Gson()
            return gson.fromJson(resJson, GithubRelease::class.java)
        }

        fun downloadUrlFromRelease(release: GithubRelease): String {
            val assets = release.assets
            for (asset in assets) {
                if (asset.name.contains("-$archType") && asset.name.contains("frida-server")) {
                    return asset.browser_download_url
                }
            }
            return ""
        }
    }
}