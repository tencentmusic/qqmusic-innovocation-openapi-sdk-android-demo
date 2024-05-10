package com.tencent.qqmusic.qplayer.ui.activity.songlist

import android.annotation.SuppressLint
import android.os.Bundle
import android.provider.Settings.Global
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import coil.annotation.ExperimentalCoilApi
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.baselib.util.JobDispatcher
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.qplayer.core.player.playlist.MusicPlayList
import com.tencent.qqmusic.qplayer.ui.activity.folder.FolderActivity
import kotlinx.coroutines.*

//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

class SongListActivity : ComponentActivity() {

    companion object {
        const val KEY_FOLDER_ID = "folder_id"
        const val KEY_ALBUM_ID = "album_id"
        const val KEY_SONG = "song_id"
        const val KEY_RANK_ID = "rank_id"

        const val KEY_CATEGORY_IDS = "category_ids"

        const val SONG_TYPE_ALBUM = 1
        const val SONG_TYPE_SONG_LIST = 2

        private const val TAG = "SongListActivity"
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

    private val rankId by lazy {
        intent.getIntExtra(KEY_RANK_ID, 0)
    }

    private val categoryIds by lazy {
        intent.getIntegerArrayListExtra(KEY_CATEGORY_IDS) ?: emptyList()
    }

    private val songListViewModel: SongListViewModel by viewModels()

    private val songList = mutableStateOf(emptyList<SongInfo>())

    private val loadingText = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            mainView()
        }
    }

    @Composable
    fun mainView() {
                Column(modifier = Modifier.fillMaxSize()) {
            if (loadingText.value.isNullOrEmpty().not()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    Text(text = loadingText.value, color = Color.Red, fontSize = 32.sp, textAlign = TextAlign.Center)
                }
            }
            if (!folderId.isNullOrEmpty()) {
                playlistHeader(songs = songList.value, playListType = MusicPlayList.PLAY_LIST_FOLDER_TYPE, playListTypeId = folderId.toLongOrNull() ?: 0)

                DisposableEffect(Unit) {
                    val job = lifecycleScope.launch(Dispatchers.IO) {
                        songListViewModel.pagingFolderSongs(folderId) { songInfos, isEnd ->
                            delay(1)
                            loadingText.value = if (isEnd) {
                                async {
                                    delay(2000)
                                    loadingText.value = ""
                                }
                                "加载完成"
                            } else {
                                "列表加载ing"
                            }
                            val list = songList.value.toMutableList().apply {
                                addAll(songInfos)
                            }
                            songList.value = list
                        }
                    }
                    onDispose {
                        job.cancel()
                    }
                }
                SongListPage(songList.value, false)
            } else if (!albumId.isNullOrEmpty()) {
                songListViewModel.fetchAlbumDetail(albumId)
                SongListScreen(songListViewModel.pagingAlbumSongs(albumId), album = songListViewModel.albumDetail, type = SONG_TYPE_ALBUM,
                    playListType = MusicPlayList.PLAY_LIST_ALBUM_TYPE, playListTypeId = albumId.toLong()
                )
            } else if (songId != 0L) {
                SongListScreen(songListViewModel.pagingSongIds(listOf(songId)))
            } else if (rankId != 0) {
                SongListScreen(songListViewModel.pagingRankSongList(rankId))
            } else if (categoryIds.isNotEmpty()) {
                SongListScreen(songListViewModel.pagingSongListScene(categoryIds[0], categoryIds[1]))
            } else {
                QLog.e(TAG, "err prams")
            }
        }

    }
}
