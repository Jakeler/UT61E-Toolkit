package jk.ut61eTool.ui.navigation

import android.net.Uri

/** Navigation routes of the single-activity Compose app. */
object Routes {
    const val HOME = "home"
    const val SCAN = "scan"
    const val SETTINGS = "settings"
    const val LOG_FILES = "logfiles"

    const val LIVE = "live/{address}?name={name}"
    fun live(address: String, name: String?): String =
        "live/$address" + (name?.let { "?name=${Uri.encode(it)}" } ?: "")

    const val VIEWER = "viewer/{fileUri}"
    fun viewer(fileUri: Uri): String = "viewer/${Uri.encode(fileUri.toString())}"
}
