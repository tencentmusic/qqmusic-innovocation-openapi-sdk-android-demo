package com.tencent.qqmusic.qplayer.ui.activity.songlist

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
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
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.ui.activity.main.TopBar
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

private const val TAG = "SongListPage"

@Composable
fun SongListScreen(flow: Flow<PagingData<SongInfo>>, displayOnly: Boolean = false) {
    Scaffold(
        topBar = { TopBar() }
    ) {
        SongListPage(flow, displayOnly = displayOnly)
    }
}

@ExperimentalCoilApi
@Composable
fun SongListPage(flow: Flow<PagingData<SongInfo>>?, displayOnly: Boolean = false) {
    flow ?: return
    val activity = LocalContext.current as Activity
    val composableScope = rememberCoroutineScope()
    val songs = flow.collectAsLazyPagingItems()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        this.items(songs) { song ->
            Log.i(TAG, "SongListPage: songs size ${songs.itemCount}")
            song ?: return@items
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable {
                        if (displayOnly) {
                            return@clickable
                        }

                        composableScope.launch(Dispatchers.Main) {
                            val playList = songs.snapshot().items
                            val cur = System.currentTimeMillis()
                            val ret = OpenApiSDK
                                .getPlayerApi()
                                .playSongs(playList, playList.indexOf(song))
                            Log.d(TAG, "启播歌曲数量: ${playList.size}, ret=$ret")
                            when (ret) {
                                PlayDefine.PlayError.PLAY_ERR_NONETWORK -> {
                                    Toast
                                        .makeText(activity, "无网络连接", Toast.LENGTH_SHORT)
                                        .show()
                                }
                                else -> {
                                    delay(500L)
                                    activity.startActivity(
                                        Intent(
                                            activity,
                                            PlayerActivity::class.java
                                        )
                                    )
                                }
                            }
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