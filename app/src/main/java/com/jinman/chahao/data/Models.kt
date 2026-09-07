package com.jinman.chahao.data

data class ExtractedId(
    val id: String,
    val method: String,
    val snippet: String,
)

data class SessionId(
    val id: String,
    val method: String,
    val snippet: String,
    val searched: Boolean,
)

data class ExtraPage(
    val photoId: String,
    val file: String,
)

data class Comic(
    val id: String,
    val name: String,
    val description: String,
    val authors: List<String>,
    val extraPages: List<ExtraPage>,
    val found: Boolean,
    val error: String? = null,
    val works: List<String> = emptyList(),
    val actors: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
)

data class FavoriteComic(
    val comic: Comic,
    val savedAt: Long,
    val exported: Boolean,
    val exportedAt: Long? = null,
)

data class Blacklisted(
    val id: String,
    val name: String,
    val addedAt: Long,
)

fun methodLabel(method: String) = when (method) {
    "prefixed" -> "车牌"
    "cipher" -> "暗号"
    else -> "数字"
}
