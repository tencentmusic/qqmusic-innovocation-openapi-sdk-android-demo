package com.tencent.qqmusic.qplayer.ui.activity.songlist

import android.annotation.SuppressLint
import android.os.Bundle
import android.provider.Settings.Global
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewModelScope
import coil.annotation.ExperimentalCoilApi
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.baselib.util.JobDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

class SongListActivity : ComponentActivity() {

    companion object {
        const val KEY_FOLDER_ID = "folder_id"
        const val KEY_ALBUM_ID = "album_id"
        const val KEY_SONG = "song_id"
    }

    private val folderId by lazy {
        intent.getStringExtra(KEY_FOLDER_ID) ?: ""
    }

    private val albumId by lazy {
        intent.getStringExtra(KEY_ALBUM_ID) ?: ""
    }

    private val songId by lazy {
        intent.getLongExtra(KEY_SONG, 0)
    }

    private val songListViewModel: SongListViewModel by viewModels()

    @OptIn(ExperimentalCoilApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            if (!folderId.isNullOrEmpty()) {
                SongListScreen(songListViewModel.pagingFolderSongs(folderId))
            }
            else if (!albumId.isNullOrEmpty()) {
                SongListScreen(songListViewModel.pagingAlbumSongs(albumId))
            }
            else if (songId != 0L) {
                SongListScreen(songListViewModel.pagingSongIds(listOf(songId)))
            }
        }
    }
}
