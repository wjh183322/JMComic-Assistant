package com.jinman.chahao.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class FavoritesStore(context: Context) {
    private val prefs = context.getSharedPreferences("jm_favorites", Context.MODE_PRIVATE)
    private val file = File(context.filesDir, "favorites.json")

    fun load(): Map<String, FavoriteComic> {
        val fromPrefs = parseList(prefs.getString("items", null))
        val fromFile = if (file.exists()) parseList(runCatching { file.readText() }.getOrNull()) else emptyList()
        val merged = linkedMapOf<String, FavoriteComic>()
        for (item in fromFile + fromPrefs) {
            val prev = merged[item.comic.id]
            if (prev == null || item.savedAt >= prev.savedAt) merged[item.comic.id] = item
        }
        if (merged.isNotEmpty() && fromPrefs.isEmpty()) save(merged)
        return merged
    }

    fun save(items: Map<String, FavoriteComic>) {
        val arr = JSONArray()
        items.values.sortedByDescending { it.savedAt }.forEach { arr.put(favToJson(it)) }
        val raw = arr.toString()
        prefs.edit().putString("items", raw).commit()
        runCatching { file.writeText(raw) }
    }

    fun exportJson(items: Map<String, FavoriteComic>): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        val arr = JSONArray()
        items.values.forEach { arr.put(favToJson(it)) }
        root.put("items", arr)
        return root.toString(2)
    }

    fun parseImport(text: String): List<FavoriteComic> {
        val trimmed = text.trim()
        val arr = when {
            trimmed.startsWith("{") -> JSONObject(trimmed).optJSONArray("items") ?: JSONArray()
            else -> JSONArray(trimmed)
        }
        return parseArray(arr)
    }

    private fun parseList(raw: String?): List<FavoriteComic> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val trimmed = raw.trim()
            if (trimmed.startsWith("{")) {
                parseArray(JSONObject(trimmed).optJSONArray("items") ?: JSONArray())
            } else {
                parseArray(JSONArray(trimmed))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseArray(arr: JSONArray): List<FavoriteComic> {
        val out = mutableListOf<FavoriteComic>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val comic = comicFromJson(o) ?: continue
            out += FavoriteComic(
                comic = comic,
                savedAt = o.optLong("savedAt", System.currentTimeMillis()),
                exported = o.optBoolean("exported"),
                exportedAt = o.optLong("exportedAt").takeIf { it > 0 },
            )
        }
        return out
    }

    private fun favToJson(fav: FavoriteComic): JSONObject {
        val o = JSONObject()
        val c = fav.comic
        o.put("id", c.id)
        o.put("name", c.name)
        o.put("description", c.description)
        o.put("authors", JSONArray(c.authors))
        o.put("works", JSONArray(c.works))
        o.put("actors", JSONArray(c.actors))
        o.put("tags", JSONArray(c.tags))
        val chapters = JSONArray()
        c.chapters.forEach { ch ->
            chapters.put(JSONObject().put("id", ch.id).put("name", ch.name).put("sort", ch.sort))
        }
        o.put("chapters", chapters)
        val pages = JSONArray()
        c.extraPages.forEach { p ->
            pages.put(JSONObject().put("photoId", p.photoId).put("file", p.file))
        }
        o.put("extraPages", pages)
        o.put("found", c.found)
        o.put("savedAt", fav.savedAt)
        o.put("exported", fav.exported)
        fav.exportedAt?.let { o.put("exportedAt", it) }
        return o
    }

    private fun comicFromJson(o: JSONObject): Comic? {
        val id = o.optString("id")
        if (id.isBlank()) return null
        val pages = o.optJSONArray("extraPages")
        return Comic(
            id = id,
            name = o.optString("name").ifBlank { "JM$id" },
            description = o.optString("description"),
            authors = strList(o.optJSONArray("authors")),
            extraPages = (0 until (pages?.length() ?: 0)).mapNotNull { i ->
                val p = pages!!.optJSONObject(i) ?: return@mapNotNull null
                ExtraPage(p.optString("photoId"), p.optString("file"))
            },
            found = o.optBoolean("found", true),
            works = strList(o.optJSONArray("works")),
            actors = strList(o.optJSONArray("actors")),
            tags = strList(o.optJSONArray("tags")),
            chapters = run {
                val arr = o.optJSONArray("chapters")
                (0 until (arr?.length() ?: 0)).mapNotNull { i ->
                    val c = arr!!.optJSONObject(i) ?: return@mapNotNull null
                    val cid = c.optString("id")
                    if (cid.isBlank()) null else Chapter(cid, c.optString("name"), c.optString("sort"))
                }
            },
        )
    }

    private fun strList(arr: JSONArray?): List<String> =
        (0 until (arr?.length() ?: 0)).map { arr!!.optString(it) }.filter { it.isNotBlank() }
}
