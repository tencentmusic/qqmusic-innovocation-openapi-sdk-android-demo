package com.tencent.qqmusic.qplayer.ui.activity.home

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.Singer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Create by tinguo on 2024/1/26
 * Copyright (c) 2024 TME. All rights reserved.
 */
class OrderedSingerPagingSource(): PagingSource<Int, Singer>() {
    override fun getRefreshKey(state: PagingState<Int, Singer>): Int? = state.anchorPosition

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Singer> = try {
        val nextPage = params.key ?: 0
        val pageSize = params.loadSize

        withContext(Dispatchers.IO) {
            var hasMore = false
            val resp = OpenApiSDK.getOpenApi().blockingGet<List<Singer>> {
                OpenApiSDK.getOpenApi().fetchCollectedSinger(
                    nextPage, pageSize, it
                )
            }.apply {
                hasMore = this.hasMore
            }.data ?: emptyList()
            val prevKey = if (nextPage == 0) null else nextPage - 1
            val nextKey = if (hasMore) nextPage + 1 else null
            return@withContext LoadResult.Page(resp, prevKey, nextKey)
        }
    } catch (e: Exception) {
        Log.i("OrderedSinger", "[load] load collected singer exception.", e)
        LoadResult.Error(e)
    }
}