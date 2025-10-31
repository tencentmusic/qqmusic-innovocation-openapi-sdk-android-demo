package com.tencent.qqmusic.qplayer.ui.activity.home

import android.content.Intent
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tencent.qqmusic.innovation.common.util.UtilContext
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.Area
import com.tencent.qqmusic.openapisdk.model.AreaId
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.qplayer.ui.activity.player.voyage.PlayerVoyageActivity
import com.tencent.qqmusic.qplayer.ui.activity.songlist.PlayListParams
import com.tencent.qqmusic.qplayer.ui.activity.songlist.itemUI
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


@Composable
fun VoyagePage(homeViewModel: HomeViewModel) {
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val area = remember { mutableStateOf<Area?>(null) }
    val songs = remember { mutableStateOf(0) }
    homeViewModel.fetchSection(AreaId.Voyage) { data, _ ->
        area.value = data
    }

    var temp: List<SongInfo> = emptyList()

    val list = mutableListOf<SongInfo>()
    area.value?.shelves?.map {
        it.shelfItems.map {
            it.songInfo?.let { list.add(it) }
        }
    }
    val new = ArrayList<SongInfo>()
    AppScope.launchIO {
        val ansyList = list.chunked(40).map { songs ->
            async {
                val l: List<SongInfo>? = suspendCancellableCoroutine { continuation ->
                    OpenApiSDK.getOpenApi().fetchSongInfoBatch(songs.map { it.songId }) { result ->
                        continuation.resume(result.data)
                    }
                }
                new.addAll(l ?: emptyList())
            }
        }
        ansyList.awaitAll()

        temp = new
        songs.value = temp.size
    }
    Column {
        Row {
            Button(onClick = {
                dispatcher?.onBackPressed()
            }) { Text(text = "返回") }
            Button(modifier = Modifier.padding(start = 10.dp), onClick = {
                UtilContext.getApp().startActivity(Intent(UtilContext.getApp(), PlayerVoyageActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

            }) {
                Text(text = "臻品乐航效果展示")
            }
        }
        LazyColumn {
            items(songs.value) { song ->
                itemUI(PlayListParams(temp, temp[song], 0, 0, false))
            }
        }
    }


}