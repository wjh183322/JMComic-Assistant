package com.jinman.assistant.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import java.security.MessageDigest

private const val SCRAMBLE_220980 = 220980
private const val SCRAMBLE_268850 = 268850
private const val SCRAMBLE_421926 = 421926

fun scrambleSegments(photoId: Int, filename: String): Int {
    if (photoId < SCRAMBLE_220980) return 0
    if (photoId < SCRAMBLE_268850) return 10
    val x = if (photoId < SCRAMBLE_421926) 10 else 8
    val stem = filename.substringAfterLast('/').substringBeforeLast('.', filename)
    val digest = md5hex("$photoId$stem")
    val num = digest.last().code % x
    return num * 2 + 2
}

fun descramble(src: Bitmap, photoId: String, filename: String): Bitmap {
    val segments = scrambleSegments(photoId.toIntOrNull() ?: 0, filename)
    if (segments <= 1) return src
    val w = src.width
    val h = src.height
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val remainder = h % segments
    val part = h / segments
    for (i in 0 until segments) {
        var sliceH = part
        var destY = part * i
        val srcY = h - part * (i + 1) - remainder
        if (i == 0) sliceH += remainder else destY += remainder
        if (srcY < 0 || sliceH <= 0) continue
        val srcRect = Rect(0, srcY, w, (srcY + sliceH).coerceAtMost(h))
        val dstRect = Rect(0, destY, w, destY + sliceH)
        canvas.drawBitmap(src, srcRect, dstRect, null)
    }
    return out
}

private fun md5hex(value: String): String {
    val d = MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
    return d.joinToString("") { b -> "%02x".format(b) }
}
