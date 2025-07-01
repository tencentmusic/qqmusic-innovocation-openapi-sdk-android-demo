package com.tencent.qqmusic.qplayer.ui.activity.songlist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.Album
import com.tencent.qqmusic.openapisdk.model.Category
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.baselib.util.JobDispatcher
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayListPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

class SongListViewModel : ViewModel() {

    var albumDetail: Album? by mutableStateOf(null)

   suspend fun pagingFolderSongs(folderId: String, source:Int?=null, block: suspend (List<SongInfo>, Boolean) -> Unit) {
        var passback = ""
        while (passback != "-1") {
            val ret = SongListRepo().fetchSongInfoByFolder(folderId, passback, 50, source)
            val list = ret.data ?: emptyList()
            passback = if (ret.isSuccess() && ret.hasMore && ret.passBack != null) {
                ret.passBack ?: "-1"
            } else {
                "-1"
            }

            block(list, passback == "-1")
        }
    }

    fun fetchAlbumDetail(albumId: String) {
        OpenApiSDK.getOpenApi().fetchAlbumDetail(albumId) {
            albumDetail = it.data
        }
    }

    fun pagingAlbumSongs(albumId: String) = Pager(PagingConfig(pageSize = 50, prefetchDistance = 10, initialLoadSize = 50)) {
        AlbumSongPagingSource(albumId)
    }.flow

    fun pagingSongIds(songIds: List<Long>) = Pager(PagingConfig(pageSize = 50, prefetchDistance = 10, initialLoadSize = 50)) {
        SongListPagingSource(emptyList(), songIds)
    }.flow

    fun pagingSongListSongs(songList: List<SongInfo>) = Pager(PagingConfig(pageSize = 50, prefetchDistance = 10, initialLoadSize = 50)) {
        SongListPagingSource(songList, emptyList())
    }.flow

    fun pagingRankSongList(rankId: Int) = Pager(PagingConfig(pageSize = 20, prefetchDistance = 10, initialLoadSize = 20)) {
        RankSongPagingSource(rankId)
    }.flow

    fun pagingSongListScene(groupId: Int, subGroupId: Int) = Pager(PagingConfig(pageSize = 20, prefetchDistance = 10, initialLoadSize = 20)) {
        SongListScenePagingSource(groupId, subGroupId)
    }.flow
}