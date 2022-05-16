package com.tencent.qqmusic.qplayer.ui.activity.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.Category
import com.tencent.qqmusic.openapisdk.model.Folder
import com.tencent.qqmusic.openapisdk.model.SongInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

class HomeViewModel : ViewModel() {

    var categories: List<Category> by mutableStateOf(emptyList())

    var mineFolders: List<Folder> by mutableStateOf(emptyList())
    var favFolders: List<Folder> by mutableStateOf(emptyList())

    companion object {
        var myFolderRequested = false
        var myFavRequested = false
        var myRecentRequested = false
        fun clearRequestState() {
            myFolderRequested = false
            myFavRequested = false
            myRecentRequested = false
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

    fun pagingRecentSong(): Flow<PagingData<SongInfo>>? {
        if (!myRecentRequested) {
            myRecentRequested = true
            return Pager(PagingConfig(pageSize = 50)) { RecentSongPagingSource() }.flow
        }
        return null
    }

}