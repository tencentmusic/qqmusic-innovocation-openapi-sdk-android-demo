package com.tencent.qqmusic.qplayer.ui.activity.songlist

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.folder.FolderPage
import com.tencent.qqmusic.qplayer.ui.activity.main.TopBar
import com.tencent.qqmusic.qplayer.ui.activity.player.FloatingPlayerPage
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerActivity
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerObserver
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
        topBar = { TopBar() },
    ) {

        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
            val (folder, player) = createRefs()

            Box(modifier = Modifier.constrainAs(folder) {
                height = Dimension.fillToConstraints
                top.linkTo(parent.top)
                bottom.linkTo(player.top)
            }) {
                SongListPage(flow, displayOnly = displayOnly)
            }
            Box(modifier = Modifier.constrainAs(player) {
                bottom.linkTo(parent.bottom)
            }) {
                FloatingPlayerPage()
            }
        }

    }
}

@ExperimentalCoilApi
@Composable
fun SongListPage(flow: Flow<PagingData<SongInfo>>?, displayOnly: Boolean = false, observer: PlayerObserver = PlayerObserver) {
    flow ?: return
    val songs = flow.collectAsLazyPagingItems()
    Log.i(TAG, "SongListPage: songs count: ${songs.itemCount}")
    val listState = rememberLazyListState()
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {

        this.items(songs) { song ->
            song ?: return@items
            itemUI(songs = songs.snapshot().filterNotNull().toList(), song = song)
        }
    }
}


@Composable
fun itemUI(songs: List<SongInfo>, song: SongInfo, displayOnly: Boolean = false) {
    val activity = LocalContext.current as Activity
    val currentSong = PlayerObserver.currentSong
    val coroutineScope = rememberCoroutineScope()
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .height(60.dp)
            .clickable {
                if (displayOnly.not()) {
                    val result = OpenApiSDK
                        .getPlayerApi()
                        .playSongs(
                            songs,
                            songs.indexOf(song)
                        )
                    if (result == 0) {
                        activity.startActivity(Intent(activity, PlayerActivity::class.java))
                    } else {
                        coroutineScope.launch(Dispatchers.Main) {
                            Toast
                                .makeText(activity, "播放失败 Code is $result", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }
    ) {
        val (cover, songInfo, next, playingIcon) = createRefs()

        Image(
            painter = rememberImagePainter(song.smallCoverUrl()),
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .padding(2.dp)
                .constrainAs(cover) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                }
        )
        Column(
            modifier = Modifier
                .padding(start = 10.dp)
                .constrainAs(songInfo) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(cover.end)
                }
        ) {
            Text(text = song.songName, color = Color.Black)
            Text(
                text = song.singerName ?: "未知",
//                            color = if (canPlay) Color.Black else Color.Gray
                color = Color.Black
            )
            Row {
                if (song.vip == 1) {
                    Image(
                        painter = painterResource(R.drawable.pay_icon_in_cell_old),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 5.dp)
                            .width(18.dp)
                            .height(10.dp)
                    )
                }
                if (song.hasQualityHQ()) {
                    Image(
                        painter = painterResource(R.drawable.hq_icon), contentDescription = null, modifier = Modifier
                            .width(18.dp)
                            .height(10.dp)
                    )
                }
            }
        }

        if (currentSong?.songId == song.songId) {
            Image(
                painter = painterResource(R.drawable.list_icon_playing), contentDescription = null, modifier = Modifier
                    .padding(start = 10.dp)
                    .width(30.dp)
                    .height(30.dp)
                    .constrainAs(playingIcon) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        end.linkTo(next.start)
                    }
            )
        }

        if (displayOnly.not()) {
            Column(modifier = Modifier
                .fillMaxHeight()
                .constrainAs(next) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    end.linkTo(parent.end)
                }) {
                TextButton(
                    modifier = Modifier.height(20.dp),
                    contentPadding = PaddingValues(0.dp),
                    onClick = { OpenApiSDK.getPlayerApi().addToNext(songInfo = song) }) {
                    Text(text = "添加下一曲", fontSize = 10.sp)
                }
                TextButton(modifier = Modifier.height(20.dp),
                    contentPadding = PaddingValues(0.dp),
                    onClick = { OpenApiSDK.getPlayerApi().appendSongToPlaylist(listOf(song)) }) {
                    Text(text = "添加末尾", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun SongListPage(songs: List<SongInfo>, displayOnly: Boolean = false, needPlayer: Boolean = true) {
    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (folder, player) = createRefs()

        Box(modifier = Modifier.constrainAs(folder) {
            height = Dimension.fillToConstraints
            top.linkTo(parent.top)
            bottom.linkTo(player.top)
        }) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                this.items(songs.size) { index ->
                    val song = songs.elementAtOrNull(index) ?: return@items
                    itemUI(songs = songs, song = song, displayOnly)
                }
            }
        }
        if (needPlayer) {
            Box(modifier = Modifier.constrainAs(player) {
                bottom.linkTo(parent.bottom)
            }) {
                FloatingPlayerPage()
            }
        }
    }
}
