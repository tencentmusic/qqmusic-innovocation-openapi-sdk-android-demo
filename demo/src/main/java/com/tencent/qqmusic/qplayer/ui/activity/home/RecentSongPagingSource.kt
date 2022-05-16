package com.tencent.qqmusic.qplayer.ui.activity.home

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

//
// Created by tylertan on 2021/12/3
// Copyright (c) 2021 Tencent. All rights reserved.
//

class RecentSongPagingSource : PagingSource<Int, SongInfo>() {
    override fun getRefreshKey(state: PagingState<Int, SongInfo>): Int? {
        return state.anchorPosition
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SongInfo> {
        return try {
            withContext(Dispatchers.IO) {
                val songList = SongListRepo().fetchSongByRecent().data ?: emptyList()
                val prevKey = null
                val nextKey = null
                LoadResult.Page<Int, SongInfo>(
                    data = songList,
                    prevKey = prevKey,
                    nextKey = nextKey
                )
            }
        } catch (exception: Exception) {
            return LoadResult.Error(exception)
        }

    }
}
