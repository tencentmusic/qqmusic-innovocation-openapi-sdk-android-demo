package com.tencent.qqmusic.qplayer.ui.activity.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.*
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.log

//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

class HomeViewModel : ViewModel() {

    var categories: List<Category> by mutableStateOf(emptyList())
    var rankGroups: List<RankGroup> by mutableStateOf(emptyList())

    var mineFolders: List<Folder> by mutableStateOf(emptyList())
    var favFolders: List<Folder> by mutableStateOf(emptyList())
    var favAlbums: List<Album> by mutableStateOf(emptyList())
    var hotKeys: List<HotKey> by mutableStateOf(emptyList())
    var recentAlbums: List<Album> by mutableStateOf(emptyList())
    var recentFolders: List<Folder> by mutableStateOf(emptyList())
    var recentLongRadio: List<Album> by mutableStateOf(emptyList())

    var searchInput: String by mutableStateOf("")
    var searchSongs: List<SongInfo> by mutableStateOf(emptyList())
    var searchFolders: List<Folder> by mutableStateOf(emptyList())
    var searchAlbums: List<Album> by mutableStateOf(emptyList())

    var loginState = MutableLiveData<Pair<Boolean, Boolean>>()
    var userInfo = MutableLiveData<UserInfo?>()

    companion object {
        private const val TAG = "HomeViewModel"
        var myFolderRequested = false
        var myFavRequested = false
        var myFavAlbumRequested = false
        var myRecentRequested = false
        var fetchHotKey = false
        fun clearRequestState() {
            myFolderRequested = false
            myFavRequested = false
            myRecentRequested = false
            fetchHotKey = false
            myFavAlbumRequested = false
        }
    }

    init {
        // 进来直接加载分类
        fetchCategory()
    }

    fun fetchCategory() {
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchCategoryOfFolder {
                if (it.isSuccess()) {
                    categories = it.data ?: emptyList()
                } else {
                }
            }
        }
    }

    fun fetchRankGroup() {
        if (rankGroups.isEmpty()) {
            QLog.i(TAG, "fetchRankGroup")
            viewModelScope.launch(Dispatchers.IO) {
                OpenApiSDK.getOpenApi().fetchAllRankGroup {
                    if (it.isSuccess()) {
                        rankGroups = it.data ?: emptyList()
                    } else {
                        QLog.e(TAG, "fetchRankGroup failed:$it")
                    }
                }
            }
        }
    }

    fun fetchMineFolder() {
        if (!myFolderRequested) {
            viewModelScope.launch(Dispatchers.IO) {
                OpenApiSDK.getOpenApi().fetchPersonalFolder {
                    if (it.isSuccess()) {
                        mineFolders = it.data ?: emptyList()
                    } else {
                    }
                }
            }
            myFolderRequested = true
        }
    }

    fun fetchCollectedFolder() {
        if (!myFavRequested) {
            viewModelScope.launch(Dispatchers.IO) {
                OpenApiSDK.getOpenApi().fetchCollectedFolder {
                    if (it.isSuccess()) {
                        favFolders = it.data ?: emptyList()
                    } else {
                    }
                }
            }
            myFavRequested = true
        }
    }

    fun fetchCollectedAlbum() {
        if (!myFavAlbumRequested) {
            viewModelScope.launch(Dispatchers.IO) {
                OpenApiSDK.getOpenApi().fetchCollectedAlbum(0, 50) {
                    if (it.isSuccess()) {
                        favAlbums = it.data ?: emptyList()
                    } else {
                    }
                }
            }
            myFavAlbumRequested = true
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
            if (it.isSuccess()) {
                recentFolders = it.data ?: emptyList()
            } else {
                // 请求出错啦
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

    fun pagingRecentSong(): Flow<PagingData<SongInfo>>? {
        return Pager(PagingConfig(pageSize = 50)) { RecentSongPagingSource() }.flow
//        if (!myRecentRequested) {
//            myRecentRequested = true
//            return Pager(PagingConfig(pageSize = 50)) { RecentSongPagingSource() }.flow
//        }
//        return null
    }

    fun pagingSearchSong(): Flow<PagingData<SongInfo>> {
        return Pager(PagingConfig(pageSize = 50)) { SongListPagingSource(searchSongs, emptyList()) }.flow
    }

    fun searchSong() {
        OpenApiSDK.getOpenApi().search(searchInput, SearchType.SONG, 0, 10, callback = {
            searchSongs = it.data?.songList!!
        })
    }

    fun searchFolder() {
        OpenApiSDK.getOpenApi().search(searchInput, SearchType.FOLDER, 0, 10, callback = {
            searchFolders = it.data?.folderList!!
        })
    }

    fun searchAlbum() {
        OpenApiSDK.getOpenApi().search(searchInput, SearchType.ALBUM, 0, 10, callback = {
            searchAlbums = it.data?.albumList!!
        })
    }

    fun fetchHotKeys(type: Int)  {
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

    fun fetchUserLoginStatus() {
        QLog.i(TAG, "fetchUserLoginStatus.")
        viewModelScope.launch(Dispatchers.IO) {
            flow<Pair<Boolean, Boolean>> {
                emit(Pair(
                    Global.getLoginModuleApi().openIdInfo != null,
                    Global.getOpenApi().hasPartnerEnvSet()
                ))
            }.map {
                loginState.postValue(it)
                it
            }.map {loginStatus->
                userInfo.postValue(
                    if (loginStatus.first) {
                        OpenApiSDK.getOpenApi().blockingGet<UserInfo> {
                            OpenApiSDK.getOpenApi().fetchUserInfo(it)
                        }.data
                    } else {
                        null
                    }
                )
                loginStatus
            }.map {loginStatus->
                val status = if (loginStatus.second) {
                    OpenApiSDK.getOpenApi().blockingGet<Boolean> {
                        OpenApiSDK.getOpenApi().hasBindPartnerAccount(it)
                    }.isSuccess()
                } else {
                    false
                }
                var isLogin = Pair(loginStatus.first, status)
                loginState.postValue(isLogin)
                isLogin
            }.collect()
        }
    }

    fun logout() {
        QLog.i(TAG, "logout.")
        viewModelScope.launch {
            OpenApiSDK.getLoginApi().logout()
            loginState.value = Pair(false, false)
            userInfo.value = null
        }
    }

    fun fetchHiresSection(callback:(Area?)-> Unit) {
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

    fun fetchShelfContent(areaId:Int,
                          shelfId: Int,
                          contentSize:Int,
                          lastContentId:String,
                          callback:(AreaShelf?) -> Unit) {
        QLog.i(TAG, "fetch area: $areaId, shelf by id: $shelfId")
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchShelfContent(shelfId, contentSize, lastContentId,AreaId.AreaHires, callback = {
                if (it.isSuccess()) {
                    QLog.i(TAG, "fetch area: $areaId, shelf by id: $shelfId success")
                    callback.invoke(it.data)
                }
                else {
                    callback.invoke(null)
                }
            })
        }
    }


}