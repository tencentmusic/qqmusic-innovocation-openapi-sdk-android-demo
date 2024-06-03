package com.tencent.qqmusic.qplayer.ui.activity.home

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.tencent.qqmusic.edgemv.data.MediaResDetail
import com.tencent.qqmusic.innovation.common.logging.MLog
import com.tencent.qqmusic.openapisdk.business_common.event.BaseBusinessEvent
import com.tencent.qqmusic.openapisdk.business_common.event.BusinessEventHandler
import com.tencent.qqmusic.openapisdk.business_common.event.event.LoginEvent
import com.tencent.qqmusic.openapisdk.business_common.utils.Utils
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.openapi.OpenApiCallback
import com.tencent.qqmusic.openapisdk.core.openapi.OpenApiResponse
import com.tencent.qqmusic.openapisdk.hologram.EdgeMvProvider
import com.tencent.qqmusic.openapisdk.model.Album
import com.tencent.qqmusic.openapisdk.model.Area
import com.tencent.qqmusic.openapisdk.model.AreaId
import com.tencent.qqmusic.openapisdk.model.AreaInfo
import com.tencent.qqmusic.openapisdk.model.AreaShelf
import com.tencent.qqmusic.openapisdk.model.AreaShelfType
import com.tencent.qqmusic.openapisdk.model.BuyType
import com.tencent.qqmusic.openapisdk.model.Category
import com.tencent.qqmusic.openapisdk.model.Folder
import com.tencent.qqmusic.openapisdk.model.HotKey
import com.tencent.qqmusic.openapisdk.model.RankGroup
import com.tencent.qqmusic.openapisdk.model.SearchResult
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.openapisdk.model.SuperQualityType
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//
var currentNewAiPage = mutableStateOf(-1)

class HomeViewModel : ViewModel() {

    var categories: List<Category> by mutableStateOf(emptyList())
    var sceneCategories: List<Category> by mutableStateOf(emptyList())
    var rankGroups: List<RankGroup> by mutableStateOf(emptyList())
    var newAlbums: List<Album> by mutableStateOf(emptyList())
    var areaId: List<AreaInfo> by mutableStateOf(emptyList())

    var mineFolders: List<Folder> by mutableStateOf(emptyList())
    var favFolders: List<Folder> by mutableStateOf(emptyList())
    var favAlbums: List<Album> by mutableStateOf(emptyList())
    var hotKeys: List<HotKey> by mutableStateOf(emptyList())
    var recentAlbums: List<Album> by mutableStateOf(emptyList())
    var recentFolders: List<Folder> by mutableStateOf(emptyList())
    var recentLongRadio: List<Album> by mutableStateOf(emptyList())
    var albumOfRecord: List<Album> by mutableStateOf(emptyList())
    var songOfRecord: List<SongInfo> by mutableStateOf(emptyList())

    var longAudioCategoryPages: List<Category> by mutableStateOf(emptyList())
    private val _searchResult = MutableStateFlow<SearchResult?>(null)
    val searchResult: StateFlow<SearchResult?> = _searchResult

    var aiFolder: List<Folder> by mutableStateOf(emptyList())
    var newAiFolder = SnapshotStateList<Folder>()
    var showNewAiNextButton = mutableStateOf(false)

    var mvFavList: List<MediaResDetail> by mutableStateOf(emptyList())

    var showDialog by mutableStateOf(true)

    private var provider = OpenApiSDK.getProviderByClass(EdgeMvProvider::class.java)


    companion object {
        private const val TAG = "HomeViewModel"
        var myFolderRequested = false
        var mRankGroupsDisable = false
        var mSceneCategoriesDisable = false
        var myFavRequested = false
        var myFavAlbumRequested = -1
        var myRecentRequested = false
        var fetchHotKey = false
        fun clearRequestState() {
            myFolderRequested = false
            myFavRequested = false
            myRecentRequested = false
            fetchHotKey = false
            myFavAlbumRequested = -1
        }
    }

    init {
        // 进来直接加载分类
        fetchCategory()
        //获取新碟的地区分类
        fetchNewAlbumsByArea()

        OpenApiSDK.registerBusinessEventHandler(object : BusinessEventHandler {
            override fun handle(event: BaseBusinessEvent) {
                when (event) {
                    is LoginEvent -> {
                        viewModelScope.launch(Dispatchers.IO) {
                            fetchCategory()
                            fetchCollectedAlbum()
                            fetchRecentAlbums()
                            fetchSceneCategory()
                            fetchRecentFolders()
                            fetchRankGroup()
                            fetchNewAlbumsByArea()
                            getFreeLimitedTimeProfitInfo()
                        }
                    }
                }
            }
        })
    }


    fun fetchAiFolder() {
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchGetAiSongList {
                if (it.isSuccess()) {
                    aiFolder = it.data ?: emptyList()
                }
            }
        }
    }


    fun fetchNewAiFolder(page: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchAllTypeAiSongList(12, page) {
                if (it.isSuccess()) {
                    if (page != currentNewAiPage.value || newAiFolder.isEmpty()) {
                        showNewAiNextButton.value = it.hasMore
                        currentNewAiPage.value = page
                        newAiFolder.addAll(it.data ?: emptyList())
                    }
                }
            }
        }
    }

    fun fetchCategory() {
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchCategoryOfFolder {
                categories = if (it.isSuccess()) {
                    it.data ?: emptyList()
                } else {
                    emptyList()
                }

            }
        }
    }

    fun fetchSceneCategory() {
        if (mSceneCategoriesDisable) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchCategoryOfSongListScene {
                sceneCategories = if (it.isSuccess()) {
                    mSceneCategoriesDisable = true
                    it.data ?: emptyList()
                } else {
                    emptyList()
                }
            }
        }
    }

    fun fetchNewAlbumsByArea() {
        QLog.i(TAG, "fetchAreaByCategory")
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchNewAlbumsByArea {
                areaId = if (it.isSuccess()) {
                    it.data ?: emptyList()
                } else {
                    QLog.e(TAG, "fetchAreaByCategory failed:$it")
                    emptyList()
                }
            }
        }
    }

    fun fetchNewAlbum(areaId: Int, type: Int?) {
        QLog.i(TAG, "fetchNewAlbum")
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchNewAlbum(area = areaId, type = type, page = 0, count = 50) {
                newAlbums = if (it.isSuccess()) {
                    it.data ?: emptyList()
                } else {
                    QLog.e(TAG, "fetchNewAlbum failed:$it")
                    emptyList()
                }
            }
        }
    }

    fun fetchRankGroup() {
        if (mRankGroupsDisable) {
            return
        }
            QLog.i(TAG, "fetchRankGroup")
            viewModelScope.launch(Dispatchers.IO) {
                OpenApiSDK.getOpenApi().fetchAllRankGroup {
                    rankGroups = if (it.isSuccess()) {
                        mRankGroupsDisable = true
                        it.data ?: emptyList()
                    } else {
                        QLog.e(TAG, "fetchRankGroup failed:$it")
                        emptyList()
                    }
                }
            }
    }

    fun fetchMineFolder() {
        if (!myFolderRequested) {
            viewModelScope.launch(Dispatchers.IO) {
                OpenApiSDK.getOpenApi().fetchPersonalFolder {
                    mineFolders = if (it.isSuccess()) {
                        it.data ?: emptyList()
                    } else {
                        emptyList()
                    }
                }
            }
            myFolderRequested = true
        }
    }

    private val lasFetchFavMVListTime = AtomicLong(0)
    fun fetchFavMVList() {
        Utils.methodControlTime(lasFetchFavMVListTime, false, 1000L) {
            provider?.getMediaNetWorkImpl()?.apply {
                getCollectList(0, 50) {
                    if (it.isSuccess()) {
                        mvFavList = it.data ?: emptyList()
                    }
                }
            }
        }
    }

    fun fetchCollectedFolder() {
        if (!myFavRequested) {
            viewModelScope.launch(Dispatchers.IO) {
                OpenApiSDK.getOpenApi().fetchCollectedFolder {
                    if (it.isSuccess()) {
                        favFolders = it.data ?: emptyList()
                    }
                }
            }
            myFavRequested = true
        }
    }

    fun fetchCollectedAlbum(type: Int = 0) {
        if (myFavAlbumRequested != type) {
            viewModelScope.launch(Dispatchers.IO) {
                OpenApiSDK.getOpenApi().fetchCollectedAlbum(0, 50, type) {
                    if (it.isSuccess()) {
                        favAlbums = it.data ?: emptyList()
                    }
                }
            }
            myFavAlbumRequested = type
        }
    }

    fun fetchRecentAlbums() {
        OpenApiSDK.getOpenApi().fetchRecentPlayAlbum {
            if (it.isSuccess()) {
                recentAlbums = it.data ?: emptyList()
            } else {
                // 请求出错啦
            }
        }
    }


    fun fetchRecentFolders() {
        OpenApiSDK.getOpenApi().fetchRecentPlayFolder {
            recentFolders = if (it.isSuccess()) {
                it.data ?: emptyList()
            } else {
                // 请求出错啦
                emptyList()
            }
        }
    }

    fun fetchRecentLongRadios() {
        OpenApiSDK.getOpenApi().fetchRecentPlayLongAudio {
            if (it.isSuccess()) {
                recentLongRadio = it.data ?: emptyList()
            } else {
                // 请求出错啦

            }
        }
    }

    var currentPage = 0
    fun fetchBuyRecordOfAlbum() {
        val dataList = mutableListOf<Album>()
        val nextPage = currentPage + 1
        if (nextPage <= 50) {
            OpenApiSDK.getOpenApi().fetchBuyRecord(BuyType.ALBUMS, nextPage, callback = {
                if (it.isSuccess() && it.data?.albums?.isNotEmpty() == true) {
                    albumOfRecord += it.data?.albums!!
                }
                if (albumOfRecord.isEmpty()) {
                    currentPage = nextPage
                    dataList.addAll(albumOfRecord)
                }
            })


        }


    }

    var currentPages = 0
    fun fetchBuyRecordOfSong() {
        val dataLists = mutableListOf<SongInfo>()
        val nextPages = currentPages.toInt() + 1
        if (nextPages <= 50) {
            OpenApiSDK.getOpenApi().fetchBuyRecord(BuyType.SONGS, page = currentPages, callback = {
                if (it.isSuccess() && it.data?.songList?.isNotEmpty() == true) {
                    songOfRecord += it.data?.songList!!
                }
            })
            if (songOfRecord.isEmpty()) {
                dataLists.clear()
                currentPages = nextPages
                dataLists.addAll(songOfRecord)
                // 将新数据添加到列表末尾
            }


        }


    }


    fun pagingLongAudioSong(type: Int): Flow<PagingData<SongInfo>>? {
        return Pager(PagingConfig(pageSize = 50)) { MyLongAudioSongPagingSource(type) }.flow
    }


    fun pagingRecentSong(): Flow<PagingData<SongInfo>>? {
        return Pager(PagingConfig(pageSize = 50)) { RecentSongPagingSource() }.flow
    }


    fun search(type: Int, key: String) {
        OpenApiSDK.getOpenApi().search(key, type, 0, 10, callback = {
            viewModelScope.launch(Dispatchers.IO) {
                it.data?.let {
                    Log.d(TAG, "search: ${it}")
                    _searchResult.emit(it)
                }
            }
        })
    }


    fun fetchHotKeys(type: Int) {
        QLog.i(TAG, "fetchHotKeys type = $type")
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchHotKeyList(type = type) {
                QLog.i(TAG, "fetchHotKeys resp = $it")
                if (it.isSuccess()) {
                    hotKeys = it.data ?: emptyList()
                } else {
                    QLog.e(TAG, "fetchHotKeys error $it")
                }
            }
        }
    }


    fun fetchHiresSection(callback: (Area?) -> Unit) {
        QLog.i(TAG, "fetch hires")
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchHiresSectionByShelfTypes(emptyList(), callback = {
                if (it.isSuccess()) {
                    QLog.i(TAG, "fetch hires success")

                    callback.invoke(it.data)
                } else {
                    callback.invoke(null)
                }
            })
        }
    }

    fun fetchDolbySection(callback: (Area?) -> Unit) {
        QLog.i(TAG, "fetch dolby")
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchDolbySectionByShelfTypes(emptyList(), callback = {
                if (it.isSuccess()) {
                    QLog.i(TAG, "fetch dolby success")
                    callback.invoke(it.data)
                } else {
                    callback.invoke(null)
                }
            })
        }
    }

    fun fetchGalaxySection(callback: (Area?) -> Unit) {
        QLog.i(TAG, "fetch galaxy")
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchGalaxySectionByShelfTypes(emptyList(), callback = {
                if (it.isSuccess()) {
                    QLog.i(TAG, "fetch galaxy success")
                    callback.invoke(it.data)
                } else {
                    callback.invoke(null)
                }
            })
        }
    }

    fun fetchWanosSection(callback: (Area?) -> Unit) {
        QLog.i(TAG, "fetch Wanos")
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchAreaSectionByShelfTypes(AreaId.Wanos, arrayListOf(AreaShelfType.AreaShelfType_Song), callback = {
                if (it.isSuccess()) {
                    QLog.i(TAG, "fetch galaxy success")
                    callback.invoke(it.data)
                } else {
                    callback.invoke(null)
                }
            })
        }
    }

    fun getFreeLimitedTimeProfitInfo() {
        OpenApiSDK.getOpenApi().getFreeLimitedTimeProfitInfo(SuperQualityType.QUALITY_TYPE_WANOS) {
            MLog.i(TAG, "getFreeLimitedTimeProfitInfo ${it.data}")
            showDialog = if (it.isSuccess()) {
                it.data?.status == 0 && it.data?.used == 0 && it.data?.canTry == true
            } else {
                false
            }
        }
    }

    fun openFreeLimitedTimeAuth(callback: OpenApiCallback<OpenApiResponse<Boolean>>? = null) {
        OpenApiSDK.getOpenApi().openFreeLimitedTimeAuth(SuperQualityType.QUALITY_TYPE_WANOS, callback = callback)
    }

    fun fetchShelfContent(
        areaId: Int,
        shelfId: Int,
        contentSize: Int,
        lastContentId: String,
        callback: (AreaShelf?) -> Unit,
    ) {
        QLog.i(TAG, "fetch area: $areaId, shelf by id: $shelfId")
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchShelfContent(shelfId, contentSize, lastContentId, AreaId.AreaHires, callback = {
                if (it.isSuccess()) {
                    QLog.i(TAG, "fetch area: $areaId, shelf by id: $shelfId success")
                    callback.invoke(it.data)
                } else {
                    callback.invoke(null)
                }
            })
        }
    }

    fun fetchLongAudioCategoryPages() {
        if (longAudioCategoryPages.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchCategoryPageOfLongAudio {
                longAudioCategoryPages = it.data ?: emptyList()
            }
        }
    }

    fun pagingCategoryPageDetail(fId: Int, sId: Int) = Pager(PagingConfig(pageSize = 5)) {
        CategoryPageDetailSource(fId, sId)
    }.flow.cachedIn(viewModelScope)

    fun smartSearchKey(key: String, callback: ((List<String>) -> Unit)?) {
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().searchSmart(key) {
                callback?.invoke(it.data ?: emptyList())
            }
        }
    }

    fun fetchSingerWiki(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchSingerWiki(id) {

            }
        }
    }

    fun pagingCollectedSinger() = Pager(PagingConfig(pageSize = 10)) {
        OrderedSingerPagingSource()
    }.flow.cachedIn(viewModelScope)

}