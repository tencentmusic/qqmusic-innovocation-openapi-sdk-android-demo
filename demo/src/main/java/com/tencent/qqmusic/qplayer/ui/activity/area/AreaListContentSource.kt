package com.tencent.qqmusic.qplayer.ui.activity.area

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.AreaShelf
import com.tencent.qqmusic.openapisdk.model.AreaShelfItem
import com.tencent.qqmusic.openapisdk.model.AreaShelfType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Created by tannyli on 2023/8/1.
 * Copyright (c) 2023 TME. All rights reserved.
 */
class AreaListContentSource(val areaId: Int, val shelfId: Int):
    PagingSource<String, AreaShelfItem>() {

    companion object {
        private const val TAG = "AreaListContentSource"
    }

    override fun getRefreshKey(state: PagingState<String, AreaShelfItem>): String? {
        return state.anchorPosition?.toString()
    }

    override suspend fun load(params: LoadParams<String>): LoadResult<String, AreaShelfItem> {
        return try {
            withContext(Dispatchers.IO) {
                var hasMore = true
                var lastId = ""
                var shelfType = 0
                val nextPage = params.key ?: ""
                val shelfList = OpenApiSDK.getOpenApi().blockingGet<AreaShelf> {
                    OpenApiSDK.getOpenApi()
                        .fetchShelfContent(shelfId, 20, nextPage, areaId, it)
                }.apply {
                    hasMore = this.hasMore
                    shelfType = this.data?.shelfType ?: 0
                }.data?.shelfItems ?: emptyList()
                val prevKey = if (nextPage.isNullOrEmpty()) null else nextPage
                if (shelfType == AreaShelfType.AreaShelfType_Song) {
                    lastId = shelfList.lastOrNull()?.songInfo?.songId?.toString() ?: ""
                } else if (shelfType == AreaShelfType.AreaShelfType_Folder) {
                    lastId = shelfList.lastOrNull()?.folder?.id ?: ""
                } else if (shelfType == AreaShelfType.AreaShelfType_Album) {
                    lastId = shelfList.lastOrNull()?.album?.id ?: ""
                }
                val nextKey = if (hasMore.not() || shelfList.isEmpty()) null else lastId
                Log.i(TAG,
                    "load: shelfId:$shelfId, size:${shelfList.size} next page $nextPage, prev key $prevKey, next key $nextKey")
                LoadResult.Page(
                    data = shelfList,
                    prevKey = null,
                    nextKey = nextKey
                )
            }
        } catch (exception: Exception) {
            Log.e(TAG, "load: ", exception)
            return LoadResult.Error(exception)
        }
    }

}