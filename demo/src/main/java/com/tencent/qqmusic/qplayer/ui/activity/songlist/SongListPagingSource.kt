package com.tencent.qqmusic.qplayer.ui.activity.songlist

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.SongInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class SongListPagingSource(val songList: List<SongInfo>?, val songId: List<Long>?) : PagingSource<Int, SongInfo>() {

    companion object {
        private const val TAG = "SongListPagingSource"
    }

    override fun getRefreshKey(state: PagingState<Int, SongInfo>): Int? {
        return state.anchorPosition
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SongInfo> {
        return try {
            withContext(Dispatchers.IO) {
                var realSongList: List<SongInfo> = songList ?: emptyList()
                if (songId?.isNotEmpty() == true) {
                    realSongList = OpenApiSDK.getOpenApi().blockingGet<List<SongInfo>> {
                        OpenApiSDK.getOpenApi().fetchSongInfoBatch(songId, callback = it)
                    }.data ?: emptyList()
                }
                val nextPage = params.key ?: 0
                val prevKey = if (nextPage == 0) null else nextPage - 1
                val nextKey = null
                LoadResult.Page(
                    data = realSongList,
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