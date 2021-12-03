package com.tencent.qqmusic.qplayer.ui.activity.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListScreen

//
// Created by tylertan on 2021/11/25
// Copyright (c) 2021 Tencent. All rights reserved.
//

class PlayListActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val playList = OpenApiSDK.getPlayerApi().getPlaySongList()
            SongListScreen(playList, true)
        }
    }

}