package com.tencent.qqmusic.qplayer.ui.activity.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.Category
import com.tencent.qqmusic.openapisdk.model.Folder
import com.tencent.qqmusic.openapisdk.model.SongInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

class HomeViewModel : ViewModel() {

    var categories: List<Category> by mutableStateOf(emptyList())

    var mineFolders: List<Folder> by mutableStateOf(emptyList())
    var favFolders: List<Folder> by mutableStateOf(emptyList())
    var recentSongs: List<SongInfo> by mutableStateOf(emptyList())

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
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchPersonalFolder {
                if (it.isSuccess()) {
                    mineFolders = it.data ?: emptyList()
                } else {
                }
            }
        }
    }

    fun fetchCollectedFolder() {
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchCollectedFolder {
                if (it.isSuccess()) {
                    favFolders = it.data ?: emptyList()
                } else {
                }

            }
        }
    }

    fun fetchRecentPlaySong() {
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchRecentPlaySong {
                if (it.isSuccess()) {
                    recentSongs = it.data ?: emptyList()
                } else {
                }

            }
        }
    }

}