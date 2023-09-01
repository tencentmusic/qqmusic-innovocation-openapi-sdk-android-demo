package com.tencent.qqmusic.qplayer.ui.activity.home

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.Area
import com.tencent.qqmusic.openapisdk.model.AreaShelf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Created by tannyli on 2023/8/1.
 * Copyright (c) 2023 TME. All rights reserved.
 */
class CategoryPageDetailSource(val categoryId: Int, val subCategoryId: Int):
    PagingSource<Int, AreaShelf>() {

    companion object {
        private const val TAG = "CategoryPageDetail"
    }

    override fun getRefreshKey(state: PagingState<Int, AreaShelf>): Int? {
        return state.anchorPosition
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AreaShelf> {
        return try {
            withContext(Dispatchers.IO) {
                var hasMore = true
                val nextPage = params.key ?: 0
                val shelfList = OpenApiSDK.getOpenApi().blockingGet<Area> {
                    Global.getOpenApi()
                        .fetchCategoryPageDetailOfLongAudio(categoryId, subCategoryId, nextPage, it)
                }.apply {
                    hasMore = this.hasMore
                }.data?.shelves ?: emptyList()
                val prevKey = if (nextPage == 0) null else nextPage - 1
                val nextKey = if (hasMore.not() || shelfList.isEmpty()) null else nextPage + 1
                Log.i(TAG,
                    "load: categoryId:$categoryId, subCategoryId:$subCategoryId, size:${shelfList.size} next page $nextPage, prev key $prevKey, next key $nextKey")
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