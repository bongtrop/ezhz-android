package cc.ggez.ezhz.module.frida.model

data class GithubTag(
    val name: String,
    val commit: GithubCommit,
    val zipball_url: String,
    val tarball_url: String,
    val node_id: String,
)