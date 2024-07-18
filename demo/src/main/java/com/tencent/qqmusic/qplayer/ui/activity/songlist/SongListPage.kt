package com.tencent.qqmusic.qplayer.ui.activity.songlist

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.innovation.common.util.ToastUtils
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.openapi.OpenApiCallback
import com.tencent.qqmusic.openapisdk.core.openapi.OpenApiResponse
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums.Quality
import com.tencent.qqmusic.openapisdk.hologram.HologramManager
import com.tencent.qqmusic.openapisdk.hologram.service.IFireEyeXpmService
import com.tencent.qqmusic.openapisdk.model.Album
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.core.player.playlist.MusicPlayList
import com.tencent.qqmusic.qplayer.ui.activity.main.TopBar
import com.tencent.qqmusic.qplayer.ui.activity.mv.MVPlayerActivity
import com.tencent.qqmusic.qplayer.ui.activity.mv.MVPlayerActivity.Companion.MV_ID
import com.tencent.qqmusic.qplayer.ui.activity.player.FloatingPlayerPage
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerActivity
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerObserver
import com.tencent.qqmusic.qplayer.utils.PerformanceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

private const val TAG = "SongListPage"

@Composable
fun SongListScreen(
    flow: Flow<PagingData<SongInfo>>,
    displayOnly: Boolean = false,
    album: Album? = null,
    type: Int = SongListActivity.SONG_TYPE_SONG_LIST,
    playListType: Int = 0,
    playListTypeId: Long = 0
) {
    Scaffold(
        topBar = { TopBar() },
    ) {

        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
            val (albumInfo, folder, player) = createRefs()
            val needShowHeader = album != null && type == SongListActivity.SONG_TYPE_ALBUM
            if (needShowHeader) {
                Column(modifier = Modifier.constrainAs(albumInfo) {
                    top.linkTo(parent.top)
                }) {
                    Text(text = "歌曲总数量：${album?.songNum} ")
                    if (album?.longAudioTag?.isNotEmpty() == true) {
                        Text(text = "播放量：${album.listenNum}")
                        Text(text = "专辑标签：${album.longAudioTag}")
                    }
                }
            }

            Box(modifier = Modifier.constrainAs(folder) {
                height = Dimension.fillToConstraints
                top.linkTo(
                    if (needShowHeader) {
                        albumInfo.bottom
                    } else {
                        parent.top
                    }
                )
                bottom.linkTo(player.top)
            }) {
                SongListPage(flow, displayOnly = displayOnly, playListType = playListType, playListTypeId = playListTypeId)
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
fun SongListPage(
    flow: Flow<PagingData<SongInfo>>?,
    playListType: Int = 0,
    playListTypeId: Long = 0,
    displayOnly: Boolean = false,
    observer: PlayerObserver = PlayerObserver
) {
    flow ?: return
    val songs = flow.collectAsLazyPagingItems()
    Log.i(TAG, "SongListPage: songs count: ${songs.itemCount}")
    val listState = rememberLazyListState()
    PerformanceHelper.MonitorListScroll(scrollState = listState, location = "SongListPage")
    Column {
        playlistHeader(songs = songs.snapshot().items, playListType = playListType, playListTypeId = playListTypeId)

        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {

            this.items(songs) { song ->
                song ?: return@items
                itemUI(songs = songs.snapshot().filterNotNull().toList(), song = song)
            }
        }
    }
}


@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun playlistHeader(songs: List<SongInfo>, playListType: Int = 0, playListTypeId: Long = 0) {
    val collectState = remember {
        mutableStateOf(false)
    }
    val playlistScope = rememberCoroutineScope()
    playlistScope.launch(Dispatchers.IO) {
        when (playListType) {
            MusicPlayList.Companion.PLAY_LIST_ALBUM_TYPE -> {
                OpenApiSDK.getOpenApi().fetchAlbumDetail(albumId = "$playListTypeId") { resp->
                    if (resp.isSuccess()) {
                        collectState.value = resp.data?.favState == 1
                    }
                }
            }

            MusicPlayList.PLAY_LIST_FOLDER_TYPE -> {
                OpenApiSDK.getOpenApi().fetchCollectedFolder { resp ->
                    if (resp.isSuccess()) {
                        collectState.value = resp.data?.firstOrNull { it.id == "$playListTypeId" } != null
                    }
                }
            }
        }
    }
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        val (playAllBtn, collectBtn, collectSongBtn) = createRefs()
        Button(
            modifier = Modifier.constrainAs(playAllBtn) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
            },
            onClick = {
                OpenApiSDK.getPlayerApi().playSongs(songs, 0)
            }) {
            Text(text = "播放全部(${songs.size})")
        }

        if (playListType in arrayOf(MusicPlayList.PLAY_LIST_ALBUM_TYPE, MusicPlayList.PLAY_LIST_FOLDER_TYPE)) {
            Button(
                modifier = Modifier.constrainAs(collectBtn) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(playAllBtn.end)
                    end.linkTo(collectSongBtn.start)
                },
                onClick = {
                    val callback: OpenApiCallback<OpenApiResponse<Boolean>> = {
                        Log.i(TAG, "collect resp: $it")
                        if (it.isSuccess() && it.data == true) {
                            // nothing.
                        } else {
                            collectState.value = !collectState.value
                            ToastUtils.showShort("收藏失败,code=${it.ret},msg=${it.errorMsg}")
                        }
                    }
                    when (playListType) {
                        MusicPlayList.PLAY_LIST_ALBUM_TYPE -> {
                            OpenApiSDK.getOpenApi().collectAlbum(
                                isCollect = !collectState.value,
                                albumIdList = listOf("$playListTypeId"),
                                callback = callback
                            )
                        }

                        MusicPlayList.PLAY_LIST_FOLDER_TYPE -> {
                            if (collectState.value) {
                                OpenApiSDK.getOpenApi().unCollectFolder("$playListTypeId", callback)
                            } else {
                                OpenApiSDK.getOpenApi().collectFolder("$playListTypeId", callback)
                            }
                        }
                    }
                    collectState.value = !collectState.value
                }
            ) {
                Text(text = if (collectState.value) "取消收藏" else "收藏")
            }
        }

        Button(
            modifier = Modifier.constrainAs(collectSongBtn) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                end.linkTo(parent.end)
            },
            onClick = {
                collectSongs(songs, collectState)
            }) {
            Text(text = if (collectState.value) "取消收藏" else "收藏(${songs.size})首")
        }
    }
}

private fun collectSongs(
    songs: List<SongInfo>,
    collectState: MutableState<Boolean>
) {
    if (songs.isEmpty()) {
        ToastUtils.showShort("没有歌曲")
        return
    }
    val songList = mutableListOf<String>()
    val longSongList = mutableListOf<Long>()
    songs.forEach {
        if (it.isLongAudioSong()) {
            longSongList.add(it.songId)
        } else {
            it.songMid?.let { mid ->
                songList.add(mid)
            }
        }
    }
    if (longSongList.isNotEmpty()) {
        OpenApiSDK.getOpenApi().collectLongAudioSong(
            !collectState.value,
            longSongList
        ) {
            val msg = if (collectState.value) "取消收藏" else "收藏"
            if (it.isSuccess()) {
                ToastUtils.showShort("$msg 成功")
                collectState.value = !collectState.value
            } else {
                ToastUtils.showShort("$msg 失败：${it.errorMsg}")
            }
        }
    }
    if (songList.isNotEmpty()) {
        OpenApiSDK.getOpenApi().fetchPersonalFolder {
            if (it.isSuccess()) {
                val folderId = it.data?.firstOrNull { folder -> folder.name == "我喜欢" }?.id
                    ?: return@fetchPersonalFolder
                val songMids = songs.mapNotNull { song -> song?.songMid }
                Log.i(TAG, "fav or unfav song size: ${songMids.size}")
                if (collectState.value) {
                    OpenApiSDK.getOpenApi().deleteSongFromFolder(
                        folderId,
                        midList = songMids
                    ) { resp ->
                        if(resp.isSuccess()){
                            collectState.value = !collectState.value
                        }else{
                            ToastUtils.showShort("code=${resp.ret},msg=${resp.errorMsg}")
                            Log.e(TAG, "del resp: $resp")
                        }
                    }
                } else {
                    OpenApiSDK.getOpenApi().addSongToFolder(
                        folderId,
                        songList = songs
                    ) { resp ->
                        if(resp.isSuccess()){
                            collectState.value = !collectState.value
                        }else{
                            ToastUtils.showShort("code=${resp.ret},msg=${resp.errorMsg}")
                            Log.e(TAG, "add resp: $resp")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun itemUI(songs: List<SongInfo>, song: SongInfo, displayOnly: Boolean = false) {
    val activity = LocalContext.current as Activity
    val currentSong = PlayerObserver.currentSong
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val collectState = remember {
        mutableStateOf(song.hot == 1)
    }
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .height(70.dp)
            .clickable {
                if (displayOnly.not()) {
                    val result = OpenApiSDK
                        .getPlayerApi()
                        .playSongs(
                            songs,
                            songs.indexOf(song)
                        )
                    if (result == 0) {
                        PerformanceHelper.monitorClick("SongItemUI_PlayerActivity")
                        activity.startActivity(Intent(activity, PlayerActivity::class.java))
                    } else {
                        coroutineScope.launch(Dispatchers.Main) {
                            val toastTxt =
                                if (result == PlayDefine.PlayError.PLAY_ERR_CANNOT_PLAY) {
                                    "播放失败 错误码：$result， 错误信息：${song.unplayableMsg}"
                                } else {
                                    "播放失败 错误码：$result"
                                }
                            Toast
                                .makeText(activity, toastTxt, Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }
    ) {
        val (cover, songInfo, next, playingIcon, collect, mv) = createRefs()

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
                    end.linkTo(songInfo.start)
                }
        )
        Column(
            modifier = Modifier
                .padding(start = 10.dp)
                .constrainAs(songInfo) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(cover.end)
                }, horizontalAlignment = Alignment.Start
        ) {
            val txtColor = if (song.canPlay()) {
                Color.Black
            } else {
                Color.Gray
            }
            Text(text = song.songName, color = txtColor)
            Text(
                text = song.singerName ?: "未知",
                color = txtColor
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
                if (song.longAudioVip == 1) {
                    Image(
                        painter = painterResource(R.drawable.ic_long_audio_vip_new),
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

                } else if (OpenApiSDK.getPlayerApi().getSongHasQuality(song, Quality.WANOS)) {
                    Image(
                        painter = painterResource(R.drawable.acion_icon_quality_wanos), contentDescription = null, modifier = Modifier
                            .width(18.dp)
                            .height(10.dp)
                    )
                } else if (OpenApiSDK.getPlayerApi().getSongHasQuality(song, Quality.VINYL)) {
                    Image(
                        painter = painterResource(R.drawable.action_icon_quality_vinyl), contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
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

        if (song.mvVid?.isNotEmpty() == true) {
            Image(
                painter = painterResource(R.drawable.mv_song_list_tip_icon),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .constrainAs(mv) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(songInfo.end)
                    }
                    .width(55.dp)
                    .height(40.dp)
                    .padding(10.dp)
                    .clickable {
                        activity.startActivity(
                            Intent(
                                activity,
                                MVPlayerActivity::class.java
                            ).apply {
                                putExtra(MV_ID, song.mvVid.toString())
                            })
                    }
            )
        }
        Image(
            painter = painterResource(
                if (collectState.value)
                    R.drawable.icon_collect
                else
                    R.drawable.icon_uncollect
            ),
            contentDescription = null,
            modifier = Modifier
                .constrainAs(collect) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    if (currentSong?.songId == song.songId) {
                        end.linkTo(playingIcon.start)
                    } else if (displayOnly.not()) {
                        end.linkTo(next.start)
                    } else {
                        end.linkTo(parent.end)
                    }
                }
                .width(40.dp)
                .height(40.dp)
                .padding(10.dp)
                .clickable {
                    collectSongs(listOf(song), collectState)
                }
        )

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
                    onClick = {
                        val result = OpenApiSDK.getPlayerApi().addToNext(songInfo = song)
                        coroutineScope.launch(Dispatchers.Main) {
                            Toast
                                .makeText(
                                    activity,
                                    "添加下一曲" + if (result == 0) "成功!" else "失败!Code=$result",
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                    }) {
                    Text(text = "添加下一曲", fontSize = 10.sp)
                }
                TextButton(modifier = Modifier.height(20.dp),
                    contentPadding = PaddingValues(0.dp),
                    onClick = {
                        val result = OpenApiSDK.getPlayerApi().appendSongToPlaylist(listOf(song))
                        coroutineScope.launch(Dispatchers.Main) {
                            Toast
                                .makeText(
                                    activity,
                                    if (result == 0) "添加末尾成功" else "添加末尾失败 Code=$result",
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                    }
                ) {
                    Text(text = "添加末尾", fontSize = 10.sp)
                }
                TextButton(modifier = Modifier.height(20.dp),
                    contentPadding = PaddingValues(0.dp),
                    onClick = {
                        // 将文本放入剪贴板
                        clipboardManager.setText(AnnotatedString(song.songId.toString()))
                        coroutineScope.launch(Dispatchers.Main) {
                            Toast
                                .makeText(
                                    activity,
                                    "songId:${song.songId},复制成功",
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                    }
                ) {
                    Text(text = "复制歌曲Id", fontSize = 10.sp)
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
            val scrollState = rememberLazyListState()
            PerformanceHelper.MonitorListScroll(scrollState = scrollState, location = "SongListPage")
            LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize()) {
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
