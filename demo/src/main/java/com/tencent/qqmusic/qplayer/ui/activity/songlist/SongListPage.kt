package com.tencent.qqmusic.qplayer.ui.activity.songlist

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import com.tencent.qqmusic.openapisdk.business_common.config.SongQualityManager
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.openapi.OpenApiCallback
import com.tencent.qqmusic.openapisdk.core.openapi.OpenApiResponse
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums.Quality
import com.tencent.qqmusic.openapisdk.model.Album
import com.tencent.qqmusic.openapisdk.model.PlayParam
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.playerinsight.util.coverErrorCode
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.core.player.playlist.MusicPlayList
import com.tencent.qqmusic.qplayer.ui.activity.LoadMoreItem
import com.tencent.qqmusic.qplayer.ui.activity.loadMoreItemUI
import com.tencent.qqmusic.qplayer.ui.activity.mv.MVPlayerActivity
import com.tencent.qqmusic.qplayer.ui.activity.mv.MVPlayerActivity.Companion.MV_ID
import com.tencent.qqmusic.qplayer.ui.activity.player.FloatingPlayerPage
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerObserver
import com.tencent.qqmusic.qplayer.utils.PerformanceHelper
import com.tencent.qqmusic.qplayer.utils.UiUtils
import com.tencent.qqmusic.qplayer.utils.UiUtils.getQualityName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

private const val TAG = "SongListPage"

data class PlayListParams(
    // 歌曲列表
    val songList: List<SongInfo>,
    // 播放位置
    val startSong: SongInfo? = null,
    // 歌曲列表类型
    val playListType: Int = MusicPlayList.PLAY_LIST_NULL_TYPE,
    // 歌曲列表ID
    val playListTypeId: Long = 0,
    // 是否只播放已加载歌曲
    val displayOnly: Boolean = false,
    // 是否只播放已缓存歌曲
    val playCachedOnly: Boolean = false
)

@Composable
fun SongListScreen(
    flow: Flow<PagingData<SongInfo>>,
    displayOnly: Boolean = false,
    album: Album? = null,
    type: Int = SongListActivity.SONG_TYPE_SONG_LIST,
    playListType: Int = 0,
    playListTypeId: Long = 0
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
    val activity = LocalContext.current as Activity
    val coroutineScope = rememberCoroutineScope()
    val songs = flow.collectAsLazyPagingItems()
    val songList = songs.snapshot().filterNotNull().toList()
    Log.i(TAG, "SongListPage: songs count: ${songs.itemCount}")
    val listState = rememberLazyListState()
    PerformanceHelper.MonitorListScroll(scrollState = listState, location = "SongListPage")
    Column {
        playlistHeader(songs = songs.snapshot().items, playListType = playListType, playListTypeId = playListTypeId)

        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(songs) { song ->
                song ?: return@items
                itemUI(PlayListParams(songList, song, playListType, playListTypeId, displayOnly))
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
                val playMode = OpenApiSDK.getPlayerApi().getPlayMode()
                val playParams = PlayParam(playListType, playListTypeId, songs, -1, playMode, startPlay = true)
                val ret = OpenApiSDK.getPlayerApi().playSongs(playParams)
                if (ret != PlayDefine.PlayError.PLAY_ERR_NONE) {
                    UiUtils.showPlayErrToast(ret,songs.firstOrNull())
                } else {
                     val resType = when (playListType) {
                         MusicPlayList.PLAY_LIST_ALBUM_TYPE -> 3
                         MusicPlayList.PLAY_LIST_FOLDER_TYPE -> 4
                         else -> return@Button
                     }
                    OpenApiSDK.getOpenApi().reportRecentPlay("$playListTypeId", resType) { resp->
                        Log.i("playlistHeader", "reportRecentPlay resp: $resp")
                    }
                }
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
                val idList = arrayListOf<Long>()
                val midList = mutableListOf<String>()
                val typeList = mutableListOf<String>()
                songs.forEach {
                    idList.add(it.songId)
                    midList.add(it.songMid ?: "")
                    typeList.add(it.songType?.toString() ?: "")
                }
                if (collectState.value) {
                    OpenApiSDK.getOpenApi().deleteSongFromFolder(
                        folderId,
                        songIdList = idList,
                        midList = midList,
                        songTypes = typeList
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
fun itemUI(params: PlayListParams) {
    val activity = LocalContext.current as Activity
    val currentSong = PlayerObserver.currentSong
    val coroutineScope = rememberCoroutineScope()

    val collectState = remember {
        mutableStateOf(params.startSong?.hot == 1)
    }
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp)
            .height(50.dp)
            .clickable {
                if (params.displayOnly.not()) {
                    val result = if (params.playCachedOnly) {
                        val startPos = params.startSong?.let { params.songList.indexOf(it) } ?: -1
                        OpenApiSDK.getPlayerApi().playCachedSongs(params.songList, startPos)
                    } else {
                        val startPos = params.startSong?.let { params.songList.indexOf(it) } ?: -1
                        val playMode = OpenApiSDK.getPlayerApi().getPlayMode()
                        OpenApiSDK
                            .getPlayerApi()
                            .playSongs(
                                PlayParam(
                                    params.playListType,
                                    params.playListTypeId,
                                    params.songList,
                                    startPos,
                                    playMode,
                                    startPlay = true
                                )
                            )
                    }
                    if (result == 0) {
                        PerformanceHelper.monitorClick("SongItemUI_PlayerActivity")
                        UiUtils.gotoPlayerPage()
                        coroutineScope.launch(Dispatchers.IO) {
                            val resType = when (params.playListType) {
                                MusicPlayList.PLAY_LIST_ALBUM_TYPE -> 3
                                MusicPlayList.PLAY_LIST_FOLDER_TYPE -> 4
                                else -> return@launch
                            }
                            OpenApiSDK.getOpenApi()
                                .reportRecentPlay("${params.playListTypeId}", resType) { resp ->
                                    Log.i("itemUI", "reportRecentPlay resp: $resp")
                                }
                        }
                    } else {
                        coroutineScope.launch(Dispatchers.Main) {
                            UiUtils.showPlayErrToast(result,params.startSong)
                        }
                    }
                }
            }
    ) {
        val (cover, songInfo, next, playingIcon, collect, mv) = createRefs()

        Image(
            painter = rememberImagePainter(params.startSong?.smallCoverUrl()),
            contentDescription = null,
            modifier = Modifier
                .size(45.dp)
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
                .padding(start = 5.dp)
                .constrainAs(songInfo) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(cover.end)
                }, horizontalAlignment = Alignment.Start
        ) {
            val txtColor = if (params.startSong?.canPlay() == true) {
                Color.Black
            } else {
                Color.Gray
            }
            Text(text = params.startSong?.songName?.let {
                if (it.length > 15) it.substring(0, 15) + "..." else it
            } ?: "", color = txtColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(
                text = buildString {
                    val mainSinger = params.startSong?.singerName ?: "未知"
                    if (params.startSong?.otherSingerList.isNullOrEmpty()) {
                        append(mainSinger)
                    } else {
                        val otherSingers = params.startSong?.otherSingerList?.joinToString("/") {
                            it.title ?: it.name
                        }
                        val allSingers = "$mainSinger/$otherSingers"
                        if (allSingers.length < 12) {
                            append(allSingers)
                        } else if (mainSinger.length > 12 && (otherSingers?.length ?: 0) > 6) {
                            append("${mainSinger.substring(0..7)}/${otherSingers?.substring(0..5)}...")
                        } else {
                            append("${allSingers.substring(0..10)}...")
                        }
                    }
                },
                color = Color.Gray, fontSize = 10.sp
            )
            // label标签
            params.startSong?.let { songInfo ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (songInfo.vip == 1) {
                        Image(
                            painter = painterResource(R.drawable.pay_icon_in_cell_old),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 1.dp)
                                .width(18.dp)
                                .height(10.dp)
                        )
                    }
                    if (songInfo.longAudioVip == 1) {
                        Image(
                            painter = painterResource(R.drawable.ic_long_audio_vip_new),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 1.dp)
                                .width(18.dp)
                                .height(10.dp)
                        )
                    }
                    if (songInfo.isFreeLimit()) {
                        Image(
                            painter = painterResource(R.drawable.free_icon),
                            contentDescription = null,
                            modifier = Modifier
                                .width(18.dp)
                                .height(10.dp)
                        )
                    }
                    // 按音质优先级展示icon, 杜比、全景声、母带同级。
                    ShowIconWithPriority(
                        songInfo = songInfo,
                        qualityPriority = listOf(Quality.DOLBY)
                    )
                    ShowIconWithPriority(
                        songInfo = songInfo,
                        qualityPriority = listOf(Quality.GALAXY, Quality.HIRES, Quality.EXCELLENT, Quality.SQ, Quality.HQ, Quality.STANDARD)
                    )
                    ShowIconWithPriority(
                        songInfo = songInfo,
                        qualityPriority = listOf(Quality.WANOS, Quality.VINYL, Quality.MASTER_TAPE)
                    )
                    ShowIconWithPriority(
                        songInfo = songInfo,
                        qualityPriority = listOf(Quality.VOCAL_ACCOMPANY)
                    )
                    val pd = android.graphics.Color.parseColor("#1FCF91")
                    if (songInfo.isAISong()) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "AI",
                            fontSize = 8.sp,
                            color = Color(pd),
                            modifier = Modifier
                                .padding(1.dp)
                                .wrapContentWidth()
                                .height(10.dp)
                        )
                    }
                    if (songInfo.extraInfo?.mood?.isNotEmpty() == true) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = songInfo.extraInfo?.mood ?: "",
                            fontSize = 8.sp,
                            color = Color(pd),
                            modifier = Modifier
                                .wrapContentWidth()
                                .height(10.dp)
                        )
                    }
                }
            }
        }

        if (currentSong?.songId == params.startSong?.songId) {
            Image(
                painter = painterResource(R.drawable.list_icon_playing),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .width(30.dp)
                    .height(30.dp)
                    .constrainAs(playingIcon) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        end.linkTo(next.start)
                    }
            )
        }

        if (params.startSong?.mvVid?.isNotEmpty() == true) {
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
                    .width(50.dp)
                    .height(35.dp)
                    .padding(10.dp)
                    .clickable {
                        activity.startActivity(
                            Intent(
                                activity,
                                MVPlayerActivity::class.java
                            ).apply {
                                putExtra(MV_ID, params.startSong.mvVid.toString())
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
                    if (currentSong?.songId == params.startSong?.songId) {
                        end.linkTo(playingIcon.start)
                    } else if (params.displayOnly.not()) {
                        end.linkTo(next.start)
                    } else {
                        end.linkTo(parent.end)
                    }
                }
                .width(40.dp)
                .height(40.dp)
                .padding(10.dp)
                .clickable {
                    params.startSong?.let {
                        collectSongs(listOf(it), collectState)
                    }
                }
        )

        if (params.displayOnly.not()) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .constrainAs(next) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        end.linkTo(parent.end)
                    }) {
                TextButton(
                    modifier = Modifier.height(18.dp),
                    contentPadding = PaddingValues(0.dp),
                    onClick = {
                        val result = OpenApiSDK.getPlayerApi().addToNext(params.startSong, true)
                        coroutineScope.launch(Dispatchers.Main) {
                            Toast.makeText(
                                activity,
                                "添加下一曲:${coverErrorCode(result ?: -1)}",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                    }) {
                    Text(text = "添加下一曲", fontSize = 10.sp)
                }
                TextButton(
                    modifier = Modifier.height(18.dp),
                    contentPadding = PaddingValues(0.dp),
                    onClick = {
                        params.startSong ?: return@TextButton
                        val result =
                            OpenApiSDK.getPlayerApi().appendSongToPlaylist(listOf(params.startSong))
                        coroutineScope.launch(Dispatchers.Main) {
                            Toast
                                .makeText(
                                    activity,
                                    "添加末尾:${coverErrorCode(result ?: -1)}",
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                    }
                ) {
                    Text(text = "添加末尾", fontSize = 10.sp)
                }
                TextButton(
                    modifier = Modifier.height(18.dp),
                    contentPadding = PaddingValues(0.dp),
                    onClick = {
                        val intent = Intent(activity, SongProfileActivity::class.java)
                        intent.putExtra(SongProfileActivity.KEY_SONG, params.startSong)
                        activity.startActivity(intent)
                    }
                ) {
                    Text(text = "详情", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun SongListPage(
    songs: List<SongInfo>,
    displayOnly: Boolean = false,
    needPlayer: Boolean = true,
    loadMoreItem: LoadMoreItem? = null
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .weight(1f)) {
            val scrollState = rememberLazyListState()
            PerformanceHelper.MonitorListScroll(
                scrollState = scrollState,
                location = "SongListPage"
            )
            LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize()) {
                items(songs) { song ->
                    itemUI(PlayListParams(songs, song, displayOnly = displayOnly))
                }
                loadMoreItemUI(songs.size, loadMoreItem)
            }
        }
        if (needPlayer) {
            Box(modifier = Modifier) {
                FloatingPlayerPage()
            }
        }
    }
}

@Composable
fun ShowIconWithPriority(songInfo: SongInfo, qualityPriority: List<Int>): Int? {
    """按音质优先级展示icon"""
    var shouldShowQuality: Int? = null
    for (q in qualityPriority) {  // 这几个品质仅展示一种
        if (SongQualityManager.getSongHasQuality(songInfo, q)) {
            shouldShowQuality = q
            break
        }
    }
    shouldShowQuality?.let {
        val icon = UiUtils.getQualityIcon(it)
        if (icon != R.drawable.ic_lq) {
            Image(
                painter = painterResource(icon), contentDescription = it.getQualityName(),
                Modifier
                    .wrapContentHeight()
                    .width(26.dp)
                    .padding(end = 1.dp)
            )
        }
    }
    return shouldShowQuality
}