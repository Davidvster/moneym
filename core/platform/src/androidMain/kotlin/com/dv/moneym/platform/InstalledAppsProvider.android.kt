package com.dv.moneym.platform

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val ICON_PX = 96

class AndroidInstalledAppsProvider(
    private val context: Context,
) : InstalledAppsProvider {

    override suspend fun installedApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(launcherIntent, 0)
            .mapNotNull { resolved ->
                val pkg = resolved.activityInfo?.packageName ?: return@mapNotNull null
                val icon = runCatching { resolved.loadIcon(pm)?.toImageBitmap(ICON_PX) }.getOrNull()
                InstalledApp(packageName = pkg, label = resolved.loadLabel(pm).toString(), icon = icon)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}

private fun Drawable.toImageBitmap(sizePx: Int): ImageBitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    setBounds(0, 0, sizePx, sizePx)
    draw(canvas)
    return bmp.asImageBitmap()
}
