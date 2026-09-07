package com.jinman.assistant.data

import android.util.Base64
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object JmApi {
    private const val SECRET = "185Hcomic3PAPP7R"
    private const val VERSION = "2.1.2"
    private val HOSTS = listOf(
        "www.cdnhjk.net",
        "www.cdngwc.cc",
        "www.cdngwc.net",
        "www.cdngwc.club",
    )
    val IMAGE_HOSTS = listOf(
        "cdn-msp.jmapiproxy1.cc",
        "cdn-msp.jmapiproxy2.cc",
        "cdn-msp.jmapinodeudzn.net",
    )
    private const val UA =
        "Mozilla/5.0 (Linux; Android 9; V1938CT Build/PQ3A.190705.11211812; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/91.0.4472.114 Safari/537.36"

    private val cookieStore = mutableListOf<Cookie>()
    @Volatile private var warmed = false

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                synchronized(cookieStore) {
                    for (c in cookies) {
                        cookieStore.removeAll { it.name == c.name }
                        cookieStore.add(c)
                    }
                }
            }
            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                synchronized(cookieStore) { return cookieStore.toList() }
            }
        })
        .build()

    private val cache = mutableMapOf<String, Pair<Long, Comic>>()
    private const val TTL = 10 * 60 * 1000L

    fun coverUrl(id: String, host: String = IMAGE_HOSTS.first()) =
        "https://$host/media/albums/$id.jpg"

    fun pageUrl(photoId: String, file: String, host: String = IMAGE_HOSTS.first()) =
        "https://$host/media/photos/$photoId/$file"

    fun lookup(id: String): Comic {
        val hit = cache[id]
        if (hit != null && System.currentTimeMillis() - hit.first < TTL) return hit.second
        ensureSession()
        val comic = try {
            fetchAlbum(id)
        } catch (e: Exception) {
            return Comic(id, "JM$id", "", emptyList(), emptyList(), false, humanError(e))
        }
        cache[id] = System.currentTimeMillis() to comic
        return comic
    }

    private fun humanError(e: Exception): String {
        val m = e.message.orEmpty()
        return when {
            m.contains("未找到") -> "未找到该车号"
            m.contains("empty") -> "未找到该车号"
            m.contains("Unable to resolve") -> "网络连不上禁漫"
            m.contains("timeout", true) -> "查询超时，再试一次"
            else -> m.ifBlank { "查询失败" }
        }
    }

    @Synchronized
    private fun ensureSession() {
        if (warmed) return
        for (host in HOSTS) {
            try {
                val res = apiRaw(host, "/setting")
                if (res != null) {
                    warmed = true
                    return
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun fetchAlbum(id: String): Comic {
        val album = apiGet("/album?id=$id")
        val name = album.optString("name")
        if (name.isBlank()) {
            return Comic(id, "JM$id", "", emptyList(), emptyList(), false, "未找到该车号")
        }
        val series = album.optJSONArray("series")
        val firstChapter = series?.optJSONObject(0)?.optString("id").orEmpty().ifBlank { id }
        val extra = try {
            val chapter = apiGet("/chapter?id=$firstChapter")
            val images = chapter.optJSONArray("images")
            (0 until (images?.length() ?: 0))
                .mapNotNull { images?.optString(it)?.takeIf { s -> s.isNotBlank() } }
                .take(8)
                .map { ExtraPage(firstChapter, it) }
        } catch (_: Exception) {
            emptyList()
        }
        val authors = asList(album.opt("authors")).ifEmpty { asList(album.opt("author")) }
        return Comic(
            id = id,
            name = name,
            description = album.optString("description"),
            authors = authors,
            extraPages = extra,
            found = true,
            works = asList(album.opt("works")),
            actors = asList(album.opt("actors")),
            tags = asList(album.opt("tags")),
            chapters = parseSeries(album),
        )
    }

    fun chapterPages(chapterId: String): List<ExtraPage> {
        ensureSession()
        val chapter = apiGet("/chapter?id=$chapterId")
        val images = chapter.optJSONArray("images")
        return (0 until (images?.length() ?: 0)).mapNotNull { i ->
            images?.optString(i)?.takeIf { s -> s.isNotBlank() }?.let { ExtraPage(chapterId, it) }
        }
    }

    private fun parseSeries(album: JSONObject): List<Chapter> {
        val series = album.opt("series") ?: return emptyList()
        val objs = mutableListOf<JSONObject>()
        when (series) {
            is JSONArray -> {
                for (i in 0 until series.length()) series.optJSONObject(i)?.let { objs += it }
            }
            is JSONObject -> {
                val keys = series.keys().asSequence().toList().sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
                for (k in keys) series.optJSONObject(k)?.let { objs += it }
            }
        }
        return objs.mapNotNull { o ->
            val cid = o.optString("id")
            if (cid.isBlank()) null
            else Chapter(cid, o.optString("name"), o.optString("sort"))
        }
    }

    private fun asList(value: Any?): List<String> = when (value) {
        is JSONArray -> (0 until value.length()).mapNotNull {
            value.optString(it).trim().takeIf { s -> s.isNotBlank() }
        }
        is String -> value.split(Regex("""[,，、\s]+""")).map { it.trim() }.filter { it.isNotEmpty() }
        else -> emptyList()
    }

    private fun apiGet(path: String): JSONObject {
        var last: Exception? = null
        for (host in HOSTS) {
            try {
                val pair = signedGet(host, path) ?: continue
                val (ts, json) = pair
                val data = json.opt("data")
                if (data is String && data.isNotBlank()) {
                    return JSONObject(decrypt(data, ts))
                }
                if (data is JSONObject) return data
                last = Exception("未找到该车号")
            } catch (e: Exception) {
                last = e
            }
        }
        throw last ?: Exception("JM API unreachable")
    }

    private fun apiRaw(host: String, path: String): JSONObject? {
        return try {
            signedGet(host, path)?.second
        } catch (_: Exception) {
            null
        }
    }

    private fun signedGet(host: String, path: String): Pair<String, JSONObject>? {
        val ts = (System.currentTimeMillis() / 1000).toString()
        val token = md5hex(ts + SECRET)
        val req = Request.Builder()
            .url("https://$host$path")
            .header("User-Agent", UA)
            .header("token", token)
            .header("tokenparam", "$ts,$VERSION")
            .header("Accept", "application/json")
            .header("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7")
            .build()
        http.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return null
            val body = res.body?.string().orEmpty()
            if (body.isBlank()) return null
            return ts to JSONObject(body)
        }
    }

    private fun md5hex(value: String): String {
        val d = MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
        return d.joinToString("") { b -> "%02x".format(b) }
    }

    private fun decrypt(data: String, ts: String): String {
        val key = md5hex(ts + SECRET).toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
        val raw = Base64.decode(data, Base64.DEFAULT)
        return cipher.doFinal(raw).toString(Charsets.UTF_8)
    }
}
