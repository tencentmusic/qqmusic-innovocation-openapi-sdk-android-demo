package com.tencent.qqmusic.qplayer.ui.activity.songlist

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.ui.activity.main.TopBar
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

@Composable
fun SongListScreen(songs: List<SongInfo>, displayOnly: Boolean = false) {
    Scaffold(
        topBar = { TopBar() }
    ) {
        SongListPage(songs = songs, displayOnly = displayOnly)
    }
}

@ExperimentalCoilApi
@Composable
fun SongListPage(songs: List<SongInfo>, displayOnly: Boolean = false) {
    val activity = LocalContext.current as Activity
    val composableScope = rememberCoroutineScope()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(songs.size) { index ->
            val song = songs.getOrNull(index) ?: return@items
//                val canPlay = song.canPlay()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable {
                        if (displayOnly) {
                            return@clickable
                        }

                        composableScope.launch(Dispatchers.Main) {
                            OpenApiSDK
                                .getPlayerApi()
                                .playSongs(songs, songs.indexOf(song))
                            delay(500L)
                            activity.startActivity(
                                Intent(
                                    activity,
                                    PlayerActivity::class.java
                                )
                            )

//                                if (canPlay) {
//                                    OpenApiSDK.getPlayerApi().playSongs(songs, songs.indexOf(song))
//                                    delay(500L)
//                                    activity.startActivity(
//                                        Intent(
//                                            activity,
//                                            PlayerActivity::class.java
//                                        )
//                                    )
//                                } else {
//                                    Toast
//                                        .makeText(
//                                            activity,
//                                            "当前歌曲无法播放，code: ${song.unplayableCode}, msg: ${song.unplayableMsg}",
//                                            Toast.LENGTH_SHORT
//                                        )
//                                        .show()
//                                }
                        }
                    }
            ) {
                Image(
                    painter = rememberImagePainter(song.smallCoverUrl()),
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .padding(2.dp)
                )
                Column {
//                        Text(text = song.songName, color = if (canPlay) Color.Black else Color.Gray)
                    Text(text = song.songName, color = Color.Black)
                    Text(
                        text = song.singerName ?: "未知",
//                            color = if (canPlay) Color.Black else Color.Gray
                        color = Color.Black
                    )
                }
            }
        }
    }
}