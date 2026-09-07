package com.jinman.assistant.data

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

data class Chapter(
    val id: String,
    val name: String,
    val sort: String = "",
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
    val chapters: List<Chapter> = emptyList(),
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

fun chapterLabel(index: Int, chapter: Chapter): String {
    val name = chapter.name.trim()
    if (name.contains("话") || name.contains("話")) return name
    val n = chapter.sort.toIntOrNull() ?: (index + 1)
    return if (name.isEmpty()) "第${n}话" else "第${n}话  $name"
}
