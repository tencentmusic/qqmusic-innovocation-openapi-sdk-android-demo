package com.tencent.qqmusic.qplayer.ui.activity.audio

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.Area
import com.tencent.qqmusic.openapisdk.model.AreaShelf
import com.tencent.qqmusic.openapisdk.model.AreaShelfItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Created by tannyli on 2023/8/1.
 * Copyright (c) 2023 TME. All rights reserved.
 */
class CategoryPageModuleContentSource(val categoryId: Int):
    PagingSource<Int, AreaShelfItem>() {

    companion object {
        private const val TAG = "CategoryPageModule"
    }

    override fun getRefreshKey(state: PagingState<Int, AreaShelfItem>): Int? {
        return state.anchorPosition
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AreaShelfItem> {
        return try {
            withContext(Dispatchers.IO) {
                var hasMore = true
                val nextPage = params.key ?: 0
                val shelfList = OpenApiSDK.getOpenApi().blockingGet<AreaShelf> {
                    OpenApiSDK.getOpenApi()
                        .fetchCategoryPageModuleContentLongAudio(categoryId, nextPage, it)
                }.apply {
                    hasMore = this.hasMore
                }.data?.shelfItems ?: emptyList()
                val prevKey = if (nextPage == 0) null else nextPage - 1
                val nextKey = if (hasMore.not() || shelfList.isEmpty()) null else nextPage + 1
                Log.i(TAG,
                    "load: categoryId:$categoryId, size:${shelfList.size} next page $nextPage, prev key $prevKey, next key $nextKey")
                LoadResult.Page(
                    data = shelfList,
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