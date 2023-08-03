package cc.ggez.ezhz.core.appselect

import android.graphics.drawable.Drawable

data class AppObject(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    val enabled: Boolean,
    val uid: Int,
    val username: String?,
    val procname: String,
    var proxyed: Boolean,
    val system: Boolean
)
