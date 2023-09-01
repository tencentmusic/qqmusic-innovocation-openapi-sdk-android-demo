package com.tencent.qqmusic.qplayer.ui.activity.songlist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.Category
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.baselib.util.JobDispatcher
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayListPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

class SongListViewModel : ViewModel() {

   suspend fun pagingFolderSongs(folderId: String, block: suspend (List<SongInfo>, Boolean) -> Unit) {
        var nextPage = 0
        while (nextPage >= 0) {
            val list = SongListRepo().fetchSongInfoByFolder(folderId, nextPage, 50).data
                ?: emptyList()
            if (list.isEmpty()) {
                nextPage = -1
                block(list, true)
            } else {
                nextPage++
                block(list, false)
            }
        }
    }


    fun pagingAlbumSongs(albumId: String) = Pager(PagingConfig(pageSize = 50)) {
        AlbumSongPagingSource(albumId)
    }.flow

    fun pagingSongIds(songIds: List<Long>) = Pager(PagingConfig(pageSize = 50)) {
        SongListPagingSource(emptyList(), songIds)
    }.flow

    fun pagingSongListSongs(songList: List<SongInfo>) = Pager(PagingConfig(pageSize = 50)) {
        SongListPagingSource(songList, emptyList())
    }.flow

    fun pagingRankSongList(rankId: Int) = Pager(PagingConfig(pageSize = 20)) {
        RankSongPagingSource(rankId)
    }.flow

    fun pagingSongListScene(groupId: Int, subGroupId: Int) = Pager(PagingConfig(pageSize = 20)) {
        SongListScenePagingSource(groupId, subGroupId)
    }.flow
}