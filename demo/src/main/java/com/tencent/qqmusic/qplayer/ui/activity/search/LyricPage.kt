package com.tencent.qqmusic.qplayer.ui.activity.search

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.tencent.qqmusic.openapisdk.model.LyricInfo
import com.tencent.qqmusic.qplayer.ui.activity.LoadMoreItem
import com.tencent.qqmusic.qplayer.ui.activity.loadMoreItemUI


@Composable
fun LyricPage(list: List<LyricInfo>, loadMoreItem: LoadMoreItem? = null) {
    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (folder) = createRefs()
        Box(modifier = Modifier.constrainAs(folder) {
            height = Dimension.fillToConstraints
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
        }) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                this.items(list.size) { index ->
                    val singer = list.elementAtOrNull(index) ?: return@items
                    LyricItem(data = singer)
                }
                loadMoreItemUI(list.size, loadMoreItem = loadMoreItem)
            }
        }

    }
}

@Composable
fun LyricItem(data: LyricInfo) {
    val activity = LocalContext.current as Activity
    Box(modifier = Modifier.clickable {
        activity.startActivity(
            Intent(activity, SearchPageActivity::class.java)
                .putExtra(SearchPageActivity.searchType,SearchPageActivity.lyricIntentTag)
                .putExtra(SearchPageActivity.lyricIntentTag, data.lyric)

        )
    }){
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .height(70.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                Text(text = "歌曲 ： ${data.songName}")
                Text(text = "专辑 ： ${data.albumName}")
                Text(text = "歌手 ： ${data.singerName}")
            }
        }
    }
}