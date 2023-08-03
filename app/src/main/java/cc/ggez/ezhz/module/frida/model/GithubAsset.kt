package cc.ggez.ezhz.module.frida.model

data class GithubAsset(
    val url: String,
    val browser_download_url: String,
    val id: Int,
    val node_id: String,
    val name: String,
    val label: String,
    val state: String,
    val created_at: String,
    val updated_at: String,
    val uploader: GithubUser
)
