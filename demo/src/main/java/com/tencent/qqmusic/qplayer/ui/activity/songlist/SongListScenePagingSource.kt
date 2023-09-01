package com.tencent.qqmusic.qplayer.ui.activity.songlist

import android.annotation.SuppressLint
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.SongInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Created by tannyli on 2022/12/21.
 * Copyright (c) 2022 TME. All rights reserved.
 */

class SongListScenePagingSource(val groupId: Int, val subGroupId: Int) : PagingSource<Int, SongInfo>() {

    companion object {
        private const val TAG = "SongListScenePagingSource"
    }

    override fun getRefreshKey(state: PagingState<Int, SongInfo>): Int? {
        return state.anchorPosition
    }

    @SuppressLint("LongLogTag")
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SongInfo> {
        return try {
            withContext(Dispatchers.IO) {
                val nextPage = params.key ?: 0
                val songList = if (nextPage == 0) {
                    OpenApiSDK.getOpenApi().blockingGet<List<SongInfo>> {
                        OpenApiSDK.getOpenApi().fetchSongOfSongListScene(groupId, subGroupId, callback = it)
                    }.data ?: emptyList()
                    ?: emptyList()
                } else {
                    emptyList()
                }
                val prevKey = if (nextPage == 0) null else nextPage - 1
                val nextKey = if (songList.isEmpty()) null else nextPage + 1

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