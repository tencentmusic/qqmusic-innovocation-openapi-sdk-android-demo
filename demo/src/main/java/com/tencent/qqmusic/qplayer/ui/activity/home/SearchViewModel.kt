package com.tencent.qqmusic.qplayer.ui.activity.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.Album
import com.tencent.qqmusic.openapisdk.model.Folder
import com.tencent.qqmusic.openapisdk.model.HotKey
import com.tencent.qqmusic.openapisdk.model.LyricInfo
import com.tencent.qqmusic.openapisdk.model.SearchMVInfo
import com.tencent.qqmusic.openapisdk.model.SearchType
import com.tencent.qqmusic.openapisdk.model.Singer
import com.tencent.qqmusic.openapisdk.model.SongInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val SEARCH_PAGE_NUM = 20

class SearchModel<T>(val type: Int) {
    private val _dataList = mutableListOf<T>()
    val dataList: List<T> = _dataList
    private var nextPage: Int = 0
    private var keyword: String = ""

    private val _dataStateFlow = MutableStateFlow<List<T>>(emptyList())
    val dataStateFlow = _dataStateFlow.asStateFlow()

    private val _loadMoreState = MutableStateFlow(false)
    val loadMoreState = _loadMoreState.asStateFlow()

    fun search(keyword: String) {
        clear()
        this.keyword = keyword
        loadMore()
    }

    @Suppress("UNCHECKED_CAST")
    private fun transResult(result: List<Any>): List<T> { return result as List<T> }

    fun loadMore() {
        OpenApiSDK.getOpenApi().search(keyword, type, nextPage, SEARCH_PAGE_NUM) { resp->
            if (resp.isSuccess()) {
                when (type) {
                    SearchType.SONG -> {
                        resp.data?.songList?.let { _dataList.addAll(transResult(it)) }
                    }
                    SearchType.FOLDER -> {
                        resp.data?.folderList?.let { _dataList.addAll(transResult(it)) }
                    }
                    SearchType.ALBUM -> {
                        resp.data?.albumList?.let { _dataList.addAll(transResult(it)) }
                    }
                    SearchType.SINGER -> {
                        resp.data?.singerList?.let { _dataList.addAll(transResult(it)) }
                    }
                    SearchType.MV -> {
                        resp.data?.mvList?.let { _dataList.addAll(transResult(it)) }
                    }
                    SearchType.LYRIC -> {
                        resp.data?.lyricInfoList?.let { _dataList.addAll(transResult(it)) }
                    }
                    SearchType.RADIO -> {
                        resp.data?.albumList?.let { _dataList.addAll(transResult(it)) }
                    }
                }
                nextPage = (resp.page ?: nextPage) + 1
                _dataStateFlow.update { ArrayList(_dataList) }
                _loadMoreState.update { resp.hasMore }
            }
        }
    }

    fun clear() {
        _dataList.clear()
        nextPage = 0
        keyword = ""
        _dataStateFlow.update { emptyList()  }
        _loadMoreState.update { false }
    }
}

val HOTKEY_TYPE_LIST = listOf("热门搜索",
    "抖音热搜", "国风热搜", "经典热搜", "飙升热搜",
    "影视热搜", "综艺热搜", "视频热搜", "电台热搜"
)

class SearchViewModel: ViewModel() {

    private var keyword: String = ""

    private val searchModels = mapOf(
        SearchType.SONG to SearchModel<SongInfo>(SearchType.SONG),
        SearchType.FOLDER to SearchModel<Folder>(SearchType.FOLDER),
        SearchType.ALBUM to SearchModel<Album>(SearchType.ALBUM),
        SearchType.SINGER to SearchModel<Singer>(SearchType.SINGER),
        SearchType.MV to SearchModel<SearchMVInfo>(SearchType.MV),
        SearchType.LYRIC to SearchModel<LyricInfo>(SearchType.LYRIC),
        SearchType.RADIO to SearchModel<Album>(SearchType.RADIO)
    )

    private val _hotkeyStateFlow = MutableStateFlow<Map<Int, List<HotKey>>>(emptyMap())
    val hotkeyStateFlow: StateFlow<Map<Int, List<HotKey>>> = _hotkeyStateFlow.asStateFlow()

    fun hotkey() {
        viewModelScope.launch(Dispatchers.IO) {
            val typeList = 0..8
            val defers = typeList.map { type ->
                async {
                    OpenApiSDK.getOpenApi().blockingGet<List<HotKey>> {callback->
                        OpenApiSDK.getOpenApi().fetchHotKeyList(type = type, callback = callback)
                    }
                }
            }
            val hotKeys = mutableMapOf<Int, List<HotKey>>()
            defers.forEachIndexed { index, defer ->
                val resp = defer.await()
                if (resp.isSuccess() && !resp.data.isNullOrEmpty()) {
                    hotKeys[index] = resp.data!!
                }
            }
            _hotkeyStateFlow.update { hotKeys }
        }
    }

    fun search(type: Int, keyword: String) {
        if (this.keyword != keyword) {
            searchModels.forEach { (_, model)->
                model.clear()
            }
            this.keyword = keyword
        }
        searchModels[type]?.search(keyword)
    }

    fun smartSearchKey(key: String, callback: ((List<String>) -> Unit)?) {
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().searchSmart(key) {
                callback?.invoke(it.data ?: emptyList())
            }
        }
    }

    fun loadMore(type: Int) {
        searchModels[type]?.loadMore()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> dataListFlow(type: Int): StateFlow<List<T>> {
        return searchModels[type]?.dataStateFlow as StateFlow<List<T>>
    }

    fun loadMoreStateFlow(type: Int): StateFlow<Boolean> {
        return searchModels[type]?.loadMoreState as StateFlow<Boolean>
    }
}
