package com.jinman.chahao.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class FavoritesStore(context: Context) {
    private val prefs = context.getSharedPreferences("jm_favorites", Context.MODE_PRIVATE)

    fun load(): Map<String, FavoriteComic> {
        val raw = prefs.getString("items", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val out = mutableMapOf<String, FavoriteComic>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val comic = comicFromJson(o) ?: continue
            out[comic.id] = FavoriteComic(
                comic = comic,
                savedAt = o.optLong("savedAt", System.currentTimeMillis()),
                exported = o.optBoolean("exported"),
                exportedAt = o.optLong("exportedAt").takeIf { it > 0 },
            )
        }
        return out
    }

    fun save(items: Map<String, FavoriteComic>) {
        val arr = JSONArray()
        items.values.sortedByDescending { it.savedAt }.forEach { fav ->
            arr.put(favToJson(fav))
        }
        prefs.edit().putString("items", arr.toString()).apply()
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
        val authors = o.optJSONArray("authors")
        val pages = o.optJSONArray("extraPages")
        return Comic(
            id = id,
            name = o.optString("name").ifBlank { "JM$id" },
            description = o.optString("description"),
            authors = (0 until (authors?.length() ?: 0)).map { authors!!.optString(it) },
            extraPages = (0 until (pages?.length() ?: 0)).mapNotNull { i ->
                val p = pages!!.optJSONObject(i) ?: return@mapNotNull null
                ExtraPage(p.optString("photoId"), p.optString("file"))
            },
            found = o.optBoolean("found", true),
        )
    }
}
