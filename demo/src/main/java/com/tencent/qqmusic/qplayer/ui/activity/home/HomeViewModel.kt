package com.tencent.qqmusic.qplayer.ui.activity.home

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.MutableLiveData
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
import com.tencent.qqmusic.openapisdk.model.AreaShelf
import com.tencent.qqmusic.openapisdk.model.AreaShelfType
import com.tencent.qqmusic.openapisdk.model.Banner
import com.tencent.qqmusic.openapisdk.model.BuyType
import com.tencent.qqmusic.openapisdk.model.Category
import com.tencent.qqmusic.openapisdk.model.Folder
import com.tencent.qqmusic.openapisdk.model.HomepageRecommendation
import com.tencent.qqmusic.openapisdk.model.OtherPlatListeningList
import com.tencent.qqmusic.openapisdk.model.RankGroup
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.openapisdk.model.SuperQualityType
import com.tencent.qqmusic.openapisdk.model.UserInfo
import com.tencent.qqmusic.openapisdk.model.vip.UnionVipOrderInfo
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//
var currentNewAiPage = mutableStateOf(-1)

class HomeViewModel : ViewModel() {

    var categories: List<Category> by mutableStateOf(emptyList())
    var recommendation: HomepageRecommendation by mutableStateOf(HomepageRecommendation(emptyList(), emptyList()))
    var bannerConfig: List<Banner> by mutableStateOf(emptyList())
    var sceneCategories: List<Category> by mutableStateOf(emptyList())
    var rankGroups: MutableState<List<RankGroup>> = mutableStateOf(emptyList())

    var mineFolders: List<Folder> by mutableStateOf(emptyList())
    var favFolders: List<Folder> by mutableStateOf(emptyList())
    var favAlbums: List<Album> by mutableStateOf(emptyList())

    var recentAlbums: List<Album> by mutableStateOf(emptyList())
    var recentFolders: List<Folder> by mutableStateOf(emptyList())
    var recentLongRadio: List<Album> by mutableStateOf(emptyList())
    var albumOfRecord = mutableStateOf(emptyList<Album>())
    val albumOfRecordHasMore = mutableStateOf(false)
    var songOfRecord = mutableStateOf<List<SongInfo>>(emptyList())
    var songOfRecordHasMore = mutableStateOf(false)
    var songOfMyLikeHasMore = mutableStateOf(false)
    var songOfOther: OtherPlatListeningList by mutableStateOf(OtherPlatListeningList())
    var songOfMyLike = mutableStateOf<List<SongInfo>>(emptyList())

    var unionVipOrderList = mutableStateOf<List<UnionVipOrderInfo>>(emptyList())

    var sourceType: Int? = null
    var loginState = MutableLiveData<Pair<Boolean, Boolean>>()
    var userInfo = MutableLiveData<UserInfo?>()

    var longAudioCategoryPages: List<Category> by mutableStateOf(emptyList())

    var aiFolder: List<Folder> by mutableStateOf(emptyList())
    var newAiFolder = SnapshotStateList<Folder>()
    var showNewAiNextButton = mutableStateOf(false)

    var mvFavList: List<MediaResDetail> by mutableStateOf(emptyList())

    var showWanosDialog by mutableStateOf(true)

    var showRefreshButton by mutableStateOf(false)

    private var provider = OpenApiSDK.getProviderByClass(EdgeMvProvider::class.java)


    companion object {
        private const val TAG = "HomeViewModel"
        var myFolderRequested = false
        var mFetchOtherPlayList = false
        var mRankGroupsDisable = false
        var mSceneCategoriesDisable = false
        var myFavRequested = false
        var myFavAlbumRequested = -1
        var myRecentRequested = false
        var fetchHotKey = false
        fun clearRequestState() {
            myFolderRequested = false
            mFetchOtherPlayList = false
            myFavRequested = false
            myRecentRequested = false
            fetchHotKey = false
            myFavAlbumRequested = -1
        }
    }

    init {
        fetchHomeRecommend()
        // 进来直接加载分类
        fetchCategory()

        OpenApiSDK.registerBusinessEventHandler(object : BusinessEventHandler {
            override fun handle(event: BaseBusinessEvent) {
                when (event) {
                    is LoginEvent -> {
                        viewModelScope.launch(Dispatchers.IO) {
                            fetchHomeRecommend()
                            fetchCategory()
                            fetchCollectedAlbum()
                            fetchRecentAlbums()
                            fetchSceneCategory()
                            fetchRecentFolders()
                            fetchRankGroup()
                            getFreeLimitedTimeWanosProfitInfo()
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
                    val fixData = mutableListOf<Category>()
                    fixData.addAll(it.data?.get(0)?.subCategory!!)
                    fixData.add(Category().apply {
                        name = "猜你喜欢(click)"
                        id = 5
                    })
                    it.data?.get(0)?.subCategory = fixData
                    it.data ?: emptyList()
                } else {
                    emptyList()
                }

            }
        }
    }

    fun fetchHomeRecommend() {
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchHomepageRecommendation(listOf(200L, 500L)) {
                if (it.isSuccess()) {
                    it.data?.let { data ->
                        recommendation = data
                    }
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchBannerConfig {
                if (it.isSuccess()) {
                    it.data?.let { data ->
                        bannerConfig = data
                    }
                }
            }
        }
    }

    fun fetchSceneCategory() {
        if (mSceneCategoriesDisable) {
            return
        }
        if (sceneCategories.isNotEmpty()) {
            return
        }
        OpenApiSDK.getOpenApi().fetchCategoryOfSongListScene {
            viewModelScope.launch(Dispatchers.Main) {
                sceneCategories = if (it.isSuccess()) {
                    mSceneCategoriesDisable = true
                    it.data ?: emptyList()
                } else {
                    emptyList()
                }
            }
        }
    }

    fun fetchRankGroup() {
            QLog.i(TAG, "fetchRankGroup")
            viewModelScope.launch(Dispatchers.IO) {
                OpenApiSDK.getOpenApi().fetchAllRankGroup {
                    val groups = if (it.isSuccess()) {
                        it.data ?: emptyList()
                    } else {
                        QLog.e(TAG, "fetchRankGroup failed:$it")
                        emptyList()
                    }
                    rankGroups.value = groups
                }
            }
    }

    fun fetchMineFolder() {
        if (!myFolderRequested) {
            viewModelScope.launch(Dispatchers.IO) {
                OpenApiSDK.getOpenApi().fetchPersonalFolder {
                    mineFolders = if (it.isSuccess()) {
                        myFolderRequested = true
                        it.data ?: emptyList()
                    } else {
                        emptyList()
                    }
                }
            }
        }
    }

    fun fetchOtherFlatPlayList() {
        if (!mFetchOtherPlayList) {
            viewModelScope.launch(Dispatchers.IO) {
                OpenApiSDK.getOpenApi().fetchOtherPlatListeningList {
                    it.data?.let { data ->
                        songOfOther = data
                    }
                    mFetchOtherPlayList = false
                }
            }
            mFetchOtherPlayList = true
        }
    }

    fun fetchAllUnionVipOrderList() {
        val allOrders = mutableListOf<UnionVipOrderInfo>() // Temporary list to accumulate all orders

        fun fetchNextPage(nextPageToken: String?) {
            OpenApiSDK.getOpenApi().getUnionVipOrderList(nextPageToken = nextPageToken, pageSize = 2) { response ->
                if (response.isSuccess()) {
                    response.data?.orders?.let { orders ->
                        allOrders.addAll(orders) // Add current page orders to the list
                    }
                    val newNextPageToken = response.passBack // Get token for next page

                    if (newNextPageToken.isNullOrEmpty()) {
                        unionVipOrderList.value = allOrders // No more pages, update LiveData
                    } else {
                        fetchNextPage(newNextPageToken) // Fetch next page
                    }
                }
            }
        }

        fetchNextPage(null) // Start fetching from the first page
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
                    Log.d(TAG, "fetchCollectedAlbum res:${it.isSuccess()} size:${it.data?.size}")
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
        if (currentPage <= 50) {
            val dataList = mutableListOf<Album>()
            dataList.addAll(albumOfRecord.value)
            OpenApiSDK.getOpenApi().fetchBuyRecord(BuyType.ALBUMS, currentPage, callback = {
                if (it.isSuccess() && it.data?.albums?.isNotEmpty() == true) {
                    val albums = it.data?.albums ?: emptyList()
                    dataList.addAll(albums)
                    albumOfRecord.value = dataList
                    albumOfRecordHasMore.value = it.hasMore
                    if (it.hasMore) {
                        currentPage = it.page?.inc() ?: (currentPage + 1)
                    }
                }
            })
        }
    }

    var currentPages = 0
    fun fetchBuyRecordOfSong() {
        val dataLists = mutableListOf<SongInfo>()
        val nextPages = currentPages.toInt() + 1
        if (nextPages <= 50) {
            dataLists.addAll(songOfRecord.value)
            OpenApiSDK.getOpenApi().fetchBuyRecord(BuyType.SONGS, page = nextPages, callback = {
                if (it.isSuccess()) {
                    val songs = it.data?.songList ?: emptyList()
                    dataLists.addAll(songs)
                    songOfRecord.value = dataLists
                    songOfRecordHasMore.value = it.hasMore
                    currentPages = if (it.hasMore) nextPages else currentPages
                }
            })
        }
    }


    fun pagingLongAudioSong(type: Int): Flow<PagingData<SongInfo>>? {
        return Pager(PagingConfig(pageSize = 50)) { MyLongAudioSongPagingSource(type) }.flow
    }


    fun pagingRecentSong(): Flow<PagingData<SongInfo>>? {
        return Pager(PagingConfig(pageSize = 50)) { RecentSongPagingSource() }.flow
    }

    var myLikePassBack = ""
    fun fetchMyLikeSong() {
        val dataLists = mutableListOf<SongInfo>()
        dataLists.addAll(songOfMyLike.value)
        OpenApiSDK.getOpenApi().fetchSongOfMyLikeFolder(passBack = myLikePassBack, callback = {
            if (it.isSuccess()) {
                val songs = it.data?: emptyList()
                dataLists.addAll(songs)
                songOfMyLike.value = dataLists
                songOfMyLikeHasMore.value = it.hasMore
                myLikePassBack = if (it.hasMore) it.passBack.toString() else ""
            }
        })
    }

    fun fetchHiresSection(callback: (Area?, String?) -> Unit) {
        QLog.i(TAG, "fetch hires")
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchHiresSectionByShelfTypes(emptyList(), callback = {
                if (it.isSuccess()) {
                    QLog.i(TAG, "fetch hires success")
                    callback.invoke(it.data,it.errorMsg)
                } else {
                    callback.invoke(null, it.errorMsg)
                }
            })
        }
    }

    fun fetchDolbySection(callback: (Area?, String?) -> Unit) {
        QLog.i(TAG, "fetch dolby")
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchDolbySectionByShelfTypes(emptyList(), callback = {
                if (it.isSuccess()) {
                    QLog.i(TAG, "fetch dolby success")
                    callback.invoke(it.data,it.errorMsg)
                } else {
                    callback.invoke(null, it.errorMsg)
                }
            })
        }
    }

    fun fetchGalaxySection(callback: (Area?, String?) -> Unit) {
        QLog.i(TAG, "fetch galaxy")
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchGalaxySectionByShelfTypes(emptyList(), callback = {
                if (it.isSuccess()) {
                    QLog.i(TAG, "fetch galaxy success")
                    callback.invoke(it.data,it.errorMsg)
                } else {
                    callback.invoke(null, it.errorMsg)
                }
            })
        }
    }

    fun fetchSection(areaId: Int, callback: (Area?, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchAreaSectionByShelfTypes(areaId, arrayListOf(AreaShelfType.AreaShelfType_Song), callback = {
                if (it.isSuccess()) {
                    callback.invoke(it.data,it.errorMsg)
                } else {
                    callback.invoke(null, it.errorMsg)
                }
            })
        }
    }

    fun fetchVinylSection(callback: (Area?, String?) -> Unit) {
        QLog.i(TAG, "fetch vinyl")
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchAreaSectionByShelfTypes(AreaId.Vinly, arrayListOf(AreaShelfType.AreaShelfType_Album, AreaShelfType.AreaShelfType_Folder), callback = {
                if (it.isSuccess()) {
                    QLog.i(TAG, "fetch vinyl success")
                    callback.invoke(it.data,it.errorMsg)
                } else {
                    callback.invoke(null, it.errorMsg)
                }
            })
        }
    }

    fun fetchMasterSection(callback: (Area?, String?) -> Unit) {
        QLog.i(TAG, "fetchMaster")
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchAreaSectionByShelfTypes(AreaId.Master, arrayListOf(), callback = {
                if (it.isSuccess()) {
                    QLog.i(TAG, "fetchMaster success")
                    callback.invoke(it.data,it.errorMsg)
                } else {
                    callback.invoke(null, it.errorMsg)
                }
            })
        }
    }

    fun getFreeLimitedTimeWanosProfitInfo() {
        OpenApiSDK.getOpenApi().getFreeLimitedTimeProfitInfo(SuperQualityType.QUALITY_TYPE_WANOS) {
            MLog.i(TAG, "getFreeLimitedTimeProfitInfo ${it.data}")
            showWanosDialog = if (it.isSuccess()) {
                it.data?.status == 0 && it.data?.used == 0 && it.data?.canTry == true
            } else {
                false
            }
        }
    }

    fun openFreeLimitedTimeAuthWanos(callback: OpenApiCallback<OpenApiResponse<Boolean>>? = null) {
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

    fun fetchSingerWiki(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchSingerWiki(id) {

            }
        }
    }

    fun pagingCollectedSinger() = Pager(PagingConfig(pageSize = 10)) {
        OrderedSingerPagingSource()
    }.flow.cachedIn(viewModelScope)

    fun cleanData(){
        showWanosDialog = true
        showRefreshButton = false
    }
}