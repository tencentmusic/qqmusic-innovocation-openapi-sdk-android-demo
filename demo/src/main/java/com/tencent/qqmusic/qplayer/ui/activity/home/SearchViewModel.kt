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
import com.tencent.qqmusic.openapisdk.model.StreamMusicSkillHistoryItem
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerObserver
import com.tencent.qqmusic.qplayer.ui.activity.search.StreamSkillUiState
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

    // ==================== streamMusicSkill 流式 AI 搜歌 ====================

    private val _streamSkillState = MutableStateFlow(StreamSkillUiState())
    val streamSkillState: StateFlow<StreamSkillUiState> = _streamSkillState.asStateFlow()

    /** 当前轮询的 cancel 函数，非 null 表示正在进行中 */
    private var cancelStreamSkill: (() -> Unit)? = null

    fun startStreamMusicSkill(question: String) {
        // 如果上一次还在进行中，先取消
        cancelStreamSkill?.invoke()
        cancelStreamSkill = null

        // 记录用户发起提问时的时间戳
        val questionTimestampMs = System.currentTimeMillis()

        // 重置本次 UI 状态（保留历史，清空上一次展示内容）
        _streamSkillState.update { current ->
            current.copy(
                isLoading = true,
                streamingText = "",
                songList = emptyList(),
                errorMsg = "",
                miscInfo = null,
                textAtEnd = null,
                openApiSearchType = null,
                musicSkillRecommendation = null,
                playInfoList = emptyList()
            )
        }

        val history = _streamSkillState.value.history

        cancelStreamSkill = OpenApiSDK.getOpenApi().streamMusicSkill(
            originQuestion = question,
            history = history,
            currentSongId = PlayerObserver.currentSong?.songId,
            callback = object : com.tencent.qqmusic.openapisdk.core.openapi.StreamMusicSkillCallback {

                override fun onTextUpdate(text: String, msgType: Int, miscInfo: Map<String, String>?, openApiSearchType: String?) {
                    _streamSkillState.update {
                        it.copy(
                            streamingText = text,
                            lastMsgType = msgType,
                            miscInfo = miscInfo,
                            openApiSearchType = openApiSearchType
                        )
                    }
                }

                override fun onComplete(result: com.tencent.qqmusic.openapisdk.model.StreamMusicSkillResult) {
                    // 将本次对话（用户提问 + AI 回复）追加到历史
                    val newHistory = _streamSkillState.value.history.toMutableList().apply {
                        // 用户提问条目（msgType=1）：使用发起请求时的时间戳，msgId 独立生成
                        add(StreamMusicSkillHistoryItem(
                            msgId = "${result.msgId}_user",
                            timestampMs = questionTimestampMs,
                            msgType = 1,
                            text = question
                        ))
                        // AI 回复条目（msgType=2）：使用服务端返回的时间戳
                        add(StreamMusicSkillHistoryItem(
                            msgId = result.msgId,
                            timestampMs = result.msgTimestampMs,
                            msgType = 2,
                            text = result.finalText,
                            musicContent = result.musicContent
                        ))
                    }
                    _streamSkillState.update {
                        it.copy(
                            isLoading = false,
                            streamingText = result.finalText,
                            songList = result.songList,
                            history = newHistory,
                            miscInfo = result.miscInfo,
                            textAtEnd = result.textAtEnd,
                            openApiSearchType = result.openApiSearchType,
                            musicSkillRecommendation = result.musicContent?.musicSkillRecommendation,
                            playInfoList = result.musicContent?.items?.map { it.playInfo } ?: emptyList()
                        )
                    }
                    cancelStreamSkill = null
                }

                override fun onSafetyHit(hitType: Int, fallbackText: String?) {
                    _streamSkillState.update {
                        it.copy(
                            isLoading = false,
                            errorMsg = "安全拦截（hitType=$hitType）：${fallbackText ?: ""}"
                        )
                    }
                    cancelStreamSkill = null
                }

                override fun onError(ret: Int, subRet: Int, msg: String?) {
                    _streamSkillState.update {
                        it.copy(
                            isLoading = false,
                            errorMsg = "请求失败 ret=$ret subRet=$subRet msg=$msg"
                        )
                    }
                    cancelStreamSkill = null
                }
            }
        )
    }

    /**
     * 取消当前流式请求（同时通知后台 abortReason=1）
     */
    fun cancelStreamMusicSkill() {
        cancelStreamSkill?.invoke()
        cancelStreamSkill = null
        _streamSkillState.update {
            it.copy(isLoading = false, errorMsg = "已取消")
        }
    }

    /**
     * 清空多轮对话历史，开启新一轮对话
     */
    fun clearStreamHistory() {
        cancelStreamMusicSkill()
        _streamSkillState.update { StreamSkillUiState() }
    }

    override fun onCleared() {
        super.onCleared()
        cancelStreamSkill?.invoke()
    }
}