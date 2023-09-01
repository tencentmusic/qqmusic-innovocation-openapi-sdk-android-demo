package com.tencent.qqmusic.qplayer.ui.activity.songlist

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.Rank
import com.tencent.qqmusic.openapisdk.model.SongInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Created by tannyli on 2022/12/21.
 * Copyright (c) 2022 TME. All rights reserved.
 */

class FreeSongPagingSource() : PagingSource<Int, SongInfo>() {

    companion object {
        private const val TAG = "FreeSongPagingSource"
    }

    override fun getRefreshKey(state: PagingState<Int, SongInfo>): Int? {
        return state.anchorPosition
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SongInfo> {
        return try {
            withContext(Dispatchers.IO) {
                val nextPage = params.key ?: 0
                val songList = OpenApiSDK.getOpenApi().blockingGet<List<Rank>> {
                    OpenApiSDK.getOpenApi().fetchJustListenRank(callback = it)
                }.data?.get(0)?.songList ?: emptyList()
                    ?: emptyList()
                val prevKey = if (nextPage == 0) null else nextPage - 1
                val nextKey = if (songList.isEmpty()) null else nextPage + 1
                Log.i(TAG,
                    "load:, next page $nextPage, prev key $prevKey, next key $nextKey")
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