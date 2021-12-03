package com.tencent.qqmusic.qplayer.ui.activity.songlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import coil.annotation.ExperimentalCoilApi

//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

class SongListActivity : ComponentActivity() {

    companion object {
        const val KEY_FOLDER_ID = "folder_id"
    }

    private val folderId by lazy {
        intent.getStringExtra(KEY_FOLDER_ID)
    }

    private val songListViewModel by lazy { SongListViewModel() }

    @OptIn(ExperimentalCoilApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SongListScreen(songListViewModel.songs)
        }

        songListViewModel.fetchSongInfoByFolder(folderId ?: "")
    }

}