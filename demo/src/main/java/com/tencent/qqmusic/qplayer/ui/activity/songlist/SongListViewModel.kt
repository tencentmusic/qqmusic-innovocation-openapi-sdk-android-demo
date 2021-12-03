package com.tencent.qqmusic.qplayer.ui.activity.songlist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.SongInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

class SongListViewModel : ViewModel() {

    var songs: List<SongInfo> by mutableStateOf(emptyList())

    fun fetchSongInfoByFolder(folderId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val ret = OpenApiSDK.getOpenApi().blockingGet<List<SongInfo>> {
                OpenApiSDK.getOpenApi().fetchSongOfFolder(folderId, callback = it)
            }
            if (ret.isSuccess()) {
                val pre = ret.data ?: emptyList()
//                val after = pre.refreshSelf()
                songs = pre
            } else {

            }
        }

    }

}