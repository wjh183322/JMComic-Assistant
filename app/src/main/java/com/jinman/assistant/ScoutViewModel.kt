package com.jinman.assistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jinman.assistant.data.Blacklisted
import com.jinman.assistant.data.BlacklistStore
import com.jinman.assistant.data.Comic
import com.jinman.assistant.data.ExtraPage
import com.jinman.assistant.data.ExtractedId
import com.jinman.assistant.data.FavoriteComic
import com.jinman.assistant.data.FavoritesStore
import com.jinman.assistant.data.JmApi
import com.jinman.assistant.data.ParseIds
import com.jinman.assistant.data.SessionId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val draft: String = "",
    val extracted: List<ExtractedId> = emptyList(),
    val session: List<SessionId> = emptyList(),
    val cache: Map<String, Comic> = emptyMap(),
    val searching: Boolean = false,
    val error: String? = null,
    val acceptedClipboard: String = "",
    val pendingClipboard: String? = null,
    val picker: List<ExtractedId> = emptyList(),
    val favorites: Map<String, FavoriteComic> = emptyMap(),
    val favTab: String = "pending",
    val selecting: Boolean = false,
    val selected: Set<String> = emptySet(),
    val toast: String? = null,
    val blacklist: Map<String, Blacklisted> = emptyMap(),
    val readPages: List<ExtraPage> = emptyList(),
    val reading: Boolean = false,
)

sealed class SearchNav {
    data class Detail(val id: String) : SearchNav()
    data object Picker : SearchNav()
    data class Fail(val message: String) : SearchNav()
}

sealed class ReadNav {
    data class Chapters(val albumId: String) : ReadNav()
    data class Reader(val albumId: String, val chapterId: String) : ReadNav()
    data class Fail(val message: String) : ReadNav()
}

class ScoutViewModel(app: Application) : AndroidViewModel(app) {
    private val store = FavoritesStore(app)
    private val blacklistStore = BlacklistStore(app)
    private val _state = MutableStateFlow(
        UiState(favorites = store.load(), blacklist = blacklistStore.load()),
    )
    val state: StateFlow<UiState> = _state
    private var clipboardPrimed = false

    fun setDraft(text: String) {
        _state.update {
            it.copy(
                draft = text,
                extracted = ParseIds.extractIds(text),
                error = null,
                acceptedClipboard = text,
            )
        }
    }

    fun ingestClipboard(text: String) {
        val next = text.trimEnd()
        if (!clipboardPrimed) {
            clipboardPrimed = true
            _state.update { it.copy(acceptedClipboard = next) }
            return
        }
        if (next.isBlank()) return
        val s = _state.value
        if (next == s.acceptedClipboard || next == s.draft) return
        _state.update { it.copy(pendingClipboard = next) }
    }

    fun dismissPending() = _state.update { it.copy(pendingClipboard = null) }

    fun dismissToast() = _state.update { it.copy(toast = null) }

    fun notify(message: String) = _state.update { it.copy(toast = message) }

    fun closePicker() = _state.update { it.copy(picker = emptyList()) }

    suspend fun searchDraft(): SearchNav = searchIds(_state.value.extracted)

    suspend fun searchPending(): SearchNav {
        val pending = _state.value.pendingClipboard ?: return SearchNav.Fail("没有新内容")
        val ids = ParseIds.extractIds(pending)
        _state.update {
            it.copy(
                draft = pending,
                extracted = ids,
                acceptedClipboard = pending,
                pendingClipboard = null,
            )
        }
        return searchIds(ids)
    }

    suspend fun searchOne(id: String): SearchNav {
        val cached = _state.value.cache[id]
        if (cached?.found == true) {
            markSearched(id)
            return SearchNav.Detail(id)
        }
        return lookupAndGo(id)
    }

    private suspend fun searchIds(ids: List<ExtractedId>): SearchNav {
        if (ids.isEmpty()) return SearchNav.Fail("没有识别到车号")
        val blocked = _state.value.blacklist.keys
        val allowed = ids.filter { it.id !in blocked }
        val skipped = ids.size - allowed.size
        if (allowed.isEmpty()) {
            return SearchNav.Fail(
                if (ids.size == 1) "JM${ids.first().id} 在黑名单中，已禁止搜索"
                else "识别到的车号都在黑名单中",
            )
        }
        if (skipped > 0) {
            _state.update { it.copy(toast = "已忽略黑名单中的 $skipped 个车号") }
        }
        beginSession(allowed)
        if (allowed.size > 1) {
            _state.update { it.copy(picker = allowed) }
            return SearchNav.Picker
        }
        return lookupAndGo(allowed.first().id)
    }

    private suspend fun lookupAndGo(id: String): SearchNav {
        _state.update { it.copy(searching = true, error = null) }
        val comic = withContext(Dispatchers.IO) { JmApi.lookup(id) }
        _state.update { s ->
            s.copy(
                searching = false,
                cache = s.cache + (id to comic),
                error = if (comic.found) null else (comic.error ?: "没有这部"),
            )
        }
        if (!comic.found) return SearchNav.Fail(comic.error ?: "没有这部")
        markSearched(id)
        return SearchNav.Detail(id)
    }

    private fun beginSession(ids: List<ExtractedId>) {
        _state.update { s ->
            s.copy(
                session = ids.map {
                    SessionId(it.id, it.method, it.snippet, s.cache[it.id]?.found == true)
                },
            )
        }
    }

    private fun markSearched(id: String) {
        _state.update { s ->
            s.copy(session = s.session.map { if (it.id == id) it.copy(searched = true) else it })
        }
    }

    fun toggleFavorite(comic: Comic) {
        if (!comic.found) return
        _state.update { s ->
            val next = s.favorites.toMutableMap()
            if (next.containsKey(comic.id)) next.remove(comic.id)
            else next[comic.id] = FavoriteComic(comic, System.currentTimeMillis(), false)
            store.save(next)
            s.copy(favorites = next, toast = if (next.containsKey(comic.id)) "已加入收藏夹" else "已取消收藏")
        }
    }

    fun toggleBlacklist(comic: Comic) {
        if (comic.id.isBlank()) return
        _state.update { s ->
            val next = s.blacklist.toMutableMap()
            val removing = next.containsKey(comic.id)
            if (removing) next.remove(comic.id)
            else next[comic.id] = Blacklisted(
                id = comic.id,
                name = comic.name.ifBlank { "JM${comic.id}" },
                addedAt = System.currentTimeMillis(),
            )
            blacklistStore.save(next)
            s.copy(
                blacklist = next,
                toast = if (removing) "已移出黑名单" else "已加入黑名单",
            )
        }
    }

    fun setFavTab(tab: String) {
        _state.update { it.copy(favTab = tab, selecting = false, selected = emptySet()) }
    }

    fun enterSelect(id: String) {
        _state.update { it.copy(selecting = true, selected = it.selected + id) }
    }

    fun toggleSelect(id: String) {
        _state.update {
            val sel = if (id in it.selected) it.selected - id else it.selected + id
            it.copy(selected = sel)
        }
    }

    fun selectAll(ids: List<String>) {
        _state.update { it.copy(selecting = true, selected = ids.toSet()) }
    }

    fun exitSelect() = _state.update { it.copy(selecting = false, selected = emptySet()) }

    fun deleteSelected() {
        _state.update { s ->
            val next = s.favorites.filterKeys { it !in s.selected }
            store.save(next)
            s.copy(favorites = next, selected = emptySet(), selecting = false, toast = "已删除")
        }
    }

    fun markExported(ids: Collection<String>) {
        val now = System.currentTimeMillis()
        _state.update { s ->
            val next = s.favorites.mapValues { (id, fav) ->
                if (id in ids) fav.copy(exported = true, exportedAt = now) else fav
            }
            store.save(next)
            s.copy(favorites = next, selected = emptySet(), selecting = false, toast = "已复制 ${ids.size} 个车号")
        }
    }

    fun markUnexported(ids: Collection<String>) {
        _state.update { s ->
            val next = s.favorites.mapValues { (id, fav) ->
                if (id in ids) fav.copy(exported = false, exportedAt = null) else fav
            }
            store.save(next)
            s.copy(
                favorites = next,
                selected = emptySet(),
                selecting = false,
                toast = "已撤回为未导出",
            )
        }
    }

    fun backupJson(): String = store.exportJson(_state.value.favorites)

    fun importJson(text: String) {
        viewModelScope.launch {
            try {
                val incoming = store.parseImport(text)
                _state.update { s ->
                    val next = s.favorites.toMutableMap()
                    var added = 0
                    var kept = 0
                    for (item in incoming) {
                        if (next.containsKey(item.comic.id)) kept++
                        else {
                            next[item.comic.id] = item
                            added++
                        }
                    }
                    store.save(next)
                    s.copy(favorites = next, toast = "导入完成，新增 $added，保留已有 $kept")
                }
            } catch (_: Exception) {
                _state.update { it.copy(toast = "无法读取这个收藏文件") }
            }
        }
    }

    suspend fun startRead(comic: Comic): ReadNav {
        if (!comic.found) return ReadNav.Fail("没有这部")
        var chapters = comic.chapters
        if (chapters.isEmpty()) {
            val fresh = withContext(Dispatchers.IO) { JmApi.lookup(comic.id) }
            if (fresh.found) {
                _state.update { it.copy(cache = it.cache + (fresh.id to fresh)) }
                chapters = fresh.chapters
            }
        }
        return if (chapters.isEmpty()) ReadNav.Reader(comic.id, comic.id)
        else ReadNav.Chapters(comic.id)
    }

    fun loadChapterPages(chapterId: String) {
        viewModelScope.launch {
            _state.update { it.copy(reading = true, readPages = emptyList()) }
            val pages = withContext(Dispatchers.IO) {
                runCatching { JmApi.chapterPages(chapterId) }.getOrDefault(emptyList())
            }
            _state.update {
                it.copy(
                    reading = false,
                    readPages = pages,
                    toast = if (pages.isEmpty()) "这一话没有图片" else it.toast,
                )
            }
        }
    }
}
