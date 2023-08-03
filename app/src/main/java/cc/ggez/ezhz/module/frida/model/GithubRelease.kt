package cc.ggez.ezhz.module.frida.model

data class GithubRelease(
    val url: String,
    val html_url: String,
    val assets_url: String,
    val upload_url: String,
    val tarball_url: String,
    val zipball_url: String,
    val discussion_url: String,
    val id: Int,
    val node_id: String,
    val tag_name: String,
    val target_commitish: String,
    val name: String,
    val body: String,
    val draft: Boolean,
    val prerelease: Boolean,
    val created_at: String,
    val published_at: String,
    val author: GithubUser,
    val assets: List<GithubAsset>,
)