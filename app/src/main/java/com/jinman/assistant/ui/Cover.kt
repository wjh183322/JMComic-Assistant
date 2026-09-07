package com.jinman.assistant.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import com.jinman.assistant.data.JmApi
import com.jinman.assistant.data.descramble
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

@Composable
fun CoverImage(
    id: String,
    photoId: String? = null,
    file: String? = null,
    modifier: Modifier = Modifier,
    forceFit: Boolean = false,
    fillWidth: Boolean = false,
) {
    var failed by remember(id, photoId, file) { mutableStateOf(false) }
    var bmp by remember(id, photoId, file) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(id, photoId, file) {
        failed = false
        bmp = withContext(Dispatchers.IO) {
            val urls = if (photoId != null && file != null) {
                JmApi.IMAGE_HOSTS.map { JmApi.pageUrl(photoId, file, it) }
            } else {
                JmApi.IMAGE_HOSTS.map { JmApi.coverUrl(id, it) }
            }
            val reqUa =
                "Mozilla/5.0 (Linux; Android 9; V1938CT) AppleWebKit/537.36 Chrome/91.0.4472.114 Safari/537.36"
            for (url in urls) {
                try {
                    val req = Request.Builder()
                        .url(url)
                        .header("User-Agent", reqUa)
                        .header("Referer", "https://www.cdnhjk.net/")
                        .header("Accept", "image/webp,image/*,*/*;q=0.8")
                        .build()
                    JmApi.http.newCall(req).execute().use { res ->
                        if (!res.isSuccessful) return@use
                        val bytes = res.body?.bytes() ?: return@use
                        if (bytes.size < 400) return@use
                        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            ?: return@use
                        return@withContext if (photoId != null && file != null) {
                            descramble(decoded, photoId, file)
                        } else {
                            decoded
                        }
                    }
                } catch (_: Exception) {
                    /* try next host */
                }
            }
            null
        }
        if (bmp == null) failed = true
    }

    if (failed || bmp == null) {
        Box(modifier.background(Surface2), contentAlignment = Alignment.Center) {
            Text(if (failed) "无封面" else "…", color = Subtle, fontSize = 11.sp)
        }
    } else {
        val bitmap = bmp!!
        val imageMod = if (fillWidth) {
            val h = bitmap.height.coerceAtLeast(1)
            modifier.fillMaxWidth().aspectRatio(bitmap.width.toFloat() / h.toFloat())
        } else {
            modifier.fillMaxSize()
        }
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = imageMod,
            contentScale = when {
                fillWidth -> ContentScale.FillWidth
                forceFit || photoId != null -> ContentScale.Fit
                else -> ContentScale.Crop
            },
        )
    }
}
