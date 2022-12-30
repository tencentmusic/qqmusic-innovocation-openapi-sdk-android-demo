package com.tencent.qqmusic.qplayer.ui.activity.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListScreen
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListViewModel

//
// Created by tylertan on 2021/11/25
// Copyright (c) 2021 Tencent. All rights reserved.
//

class PlayListActivity : ComponentActivity() {

    companion object {
        const val KEY_DISPLAY_ONLY = "KEY_DISPLAY_ONLY"
    }

    private val vm: SongListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val displayOnly = intent?.getBooleanExtra(KEY_DISPLAY_ONLY, true) ?: true
        setContent {
            SongListScreen(vm.pagingPlayList(), displayOnly)
        }
    }

}