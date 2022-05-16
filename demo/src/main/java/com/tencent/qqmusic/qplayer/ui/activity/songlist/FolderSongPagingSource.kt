package com.tencent.qqmusic.qplayer.ui.activity.songlist

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.tencent.qqmusic.openapisdk.model.SongInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

//
// Created by tylertan on 2021/12/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

class FolderSongPagingSource(val folderId: String) : PagingSource<Int, SongInfo>() {

    companion object {
        private const val TAG = "FolderSongPagingSource"
    }

    override fun getRefreshKey(state: PagingState<Int, SongInfo>): Int? {
        return state.anchorPosition
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SongInfo> {
        return try {
            withContext(Dispatchers.IO) {
                val nextPage = params.key ?: 0
                val songList = SongListRepo().fetchSongInfoByFolder(folderId, nextPage, 50).data
                    ?: emptyList()
                val prevKey = if (nextPage == 0) null else nextPage - 1
                val nextKey = if (songList.isEmpty()) null else nextPage + 1
                Log.i(TAG,
                    "load: folder id $folderId, next page $nextPage, prev key $prevKey, next key $nextKey")
                LoadResult.Page(
                    data = songList,
                    prevKey = prevKey,
                    nextKey = nextKey
                )
            }
        } catch (exception: Exception) {
            Log.e(TAG, "load: ", exception)
            return LoadResult.Error(exception)
        }
    }
}