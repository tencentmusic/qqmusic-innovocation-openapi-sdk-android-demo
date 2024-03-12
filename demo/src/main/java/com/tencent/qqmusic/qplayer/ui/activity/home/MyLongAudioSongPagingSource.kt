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

class MyLongAudioSongPagingSource(val type: Int) : PagingSource<Int, SongInfo>() {
    override fun getRefreshKey(state: PagingState<Int, SongInfo>): Int? {
        return state.anchorPosition
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SongInfo> {
        return try {
            withContext(Dispatchers.IO) {
                val next = params.key ?: 0
                val ret = SongListRepo().fetchMyLongAudioSong(type, next)
                val prevKey = next - 1
                val nextKey = next + 1
                LoadResult.Page<Int, SongInfo>(
                    data = ret.data ?: listOf(),
                    prevKey = if (prevKey < 0) null else prevKey,
                    nextKey = if (ret.hasMore) nextKey else null
                )
            }
        } catch (exception: Exception) {
            return LoadResult.Error(exception)
        }

    }
}
