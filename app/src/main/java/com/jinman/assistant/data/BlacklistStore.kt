package com.jinman.assistant.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class BlacklistStore(context: Context) {
    private val prefs = context.getSharedPreferences("jm_blacklist", Context.MODE_PRIVATE)
    private val file = File(context.filesDir, "blacklist.json")

    fun load(): Map<String, Blacklisted> {
        val fromPrefs = parse(prefs.getString("items", null))
        val fromFile = if (file.exists()) parse(runCatching { file.readText() }.getOrNull()) else emptyList()
        val merged = linkedMapOf<String, Blacklisted>()
        for (item in fromFile + fromPrefs) {
            val prev = merged[item.id]
            if (prev == null || item.addedAt >= prev.addedAt) merged[item.id] = item
        }
        if (merged.isNotEmpty() && fromPrefs.isEmpty()) save(merged)
        return merged
    }

    fun save(items: Map<String, Blacklisted>) {
        val arr = JSONArray()
        items.values.sortedByDescending { it.addedAt }.forEach { item ->
            arr.put(
                JSONObject()
                    .put("id", item.id)
                    .put("name", item.name)
                    .put("addedAt", item.addedAt),
            )
        }
        val raw = arr.toString()
        prefs.edit().putString("items", raw).commit()
        runCatching { file.writeText(raw) }
    }

    private fun parse(raw: String?): List<Blacklisted> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw.trim())
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val id = o.optString("id")
                if (id.isBlank()) null
                else Blacklisted(id, o.optString("name"), o.optLong("addedAt", System.currentTimeMillis()))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
