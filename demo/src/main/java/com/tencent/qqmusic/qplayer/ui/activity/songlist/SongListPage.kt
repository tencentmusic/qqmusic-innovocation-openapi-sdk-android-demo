package com.tencent.qqmusic.qplayer.ui.activity.songlist

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
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
    val activity = LocalContext.current as Activity
    Scaffold(
        topBar = { TopBar() },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                gotoPlayerActivity(activity)
                },
                backgroundColor = Color.Blue
            ) {
                Text(text = "去播放页", color = Color.White)
            }}
    ) {
        SongListPage(flow, displayOnly = displayOnly)
    }
}

@Composable
fun DialogDemo(showDialog: Boolean, callback: (type: String) -> Unit, setShowDialog: (Boolean) -> Unit) {

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
               setShowDialog(false)
            },
            title = {
                Text("选择你要执行的动作")
            },
            confirmButton = {
                Button(
                    onClick = {
                        setShowDialog(false)
                        callback.invoke("播放")

                    }
                ) {
                    Text("播放")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        setShowDialog(false)
                        callback.invoke("添加到下一首播放")
                    }
                ) {
                    Text("添加到下一首播放")
                }
            }
        )
    }
}

@ExperimentalCoilApi
@Composable
fun SongListPage(flow: Flow<PagingData<SongInfo>>?, displayOnly: Boolean = false) {
    flow ?: return
    val activity = LocalContext.current as Activity
    val composableScope = rememberCoroutineScope()
    val songs = flow.collectAsLazyPagingItems()
    Log.i(TAG, "SongListPage: songs count: ${songs.itemCount}")
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        this.items(songs) { song ->
            song ?: return@items
            val (showDialog, setShowDialog) =  remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable {
                        if (displayOnly) {
                            return@clickable
                        }
                        setShowDialog(true)
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

            DialogDemo(showDialog, callback = {
                when (it) {
                    "播放" -> {
                        composableScope.launch(Dispatchers.Main) {
                            val playList = songs.snapshot().items
                            val cur = System.currentTimeMillis()
                            val ret = OpenApiSDK
                                .getPlayerApi()
                                .playSongs(playList, playList.indexOf(song))
                            OpenApiSDK.getOpenApi().reportRecentPlay(song.songId.toString(), 2, callback=null)
                            Log.d(TAG, "启播歌曲数量: ${playList.size}, ret=$ret")
                            when (ret) {
                                PlayDefine.PlayError.PLAY_ERR_NONETWORK -> {
                                    Toast
                                        .makeText(activity, "无网络连接", Toast.LENGTH_SHORT)
                                        .show()
                                }
                                else -> {
                                    delay(500L)
                                    gotoPlayerActivity(activity)
                                }
                            }
                        }
                    }
                    "添加到下一首播放" -> {
                        OpenApiSDK.getPlayerApi().addToNext(songInfo = song)
                    }
                    else -> {

                    }
                }
            }, setShowDialog = setShowDialog)
        }
    }
}

private fun gotoPlayerActivity(activity: Activity) {
    activity.startActivity(
        Intent(
            activity,
            PlayerActivity::class.java
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    )
}

@Composable
fun SongListPage(songs: List<SongInfo>, displayOnly: Boolean = false) {
    val composableScope = rememberCoroutineScope()
    val activity = LocalContext.current as Activity
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        this.items(songs.size) { index ->
            val song = songs.elementAtOrNull(index) ?: return@items
            val (showDialog, setShowDialog) =  remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable {
                        if (displayOnly) {
                            return@clickable
                        }
                        setShowDialog(true)
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
            DialogDemo(showDialog, callback = {
                when (it) {
                    "播放" -> {
                        composableScope.launch(Dispatchers.Main) {
                            val cur = System.currentTimeMillis()
                            val ret = OpenApiSDK
                                .getPlayerApi()
                                .playSongs(songs, songs.indexOf(song))
                            OpenApiSDK.getOpenApi().reportRecentPlay(song.songId.toString(), 2, callback=null)
                            Log.d(TAG, "起播歌曲数量: ${songs.size}, ret=$ret")
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
                    "添加到下一首播放" -> {
                        OpenApiSDK.getPlayerApi().setPlayList(listOf(song))
                    }
                    else -> {
                    }
                }
            }, setShowDialog = setShowDialog);
        }
    }
}
