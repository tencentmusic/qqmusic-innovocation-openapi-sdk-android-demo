package com.tencent.qqmusic.qplayer.ui.activity.search

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemsIndexed
import coil.compose.rememberImagePainter
import com.google.accompanist.pager.rememberPagerState
import com.tencent.qqmusic.openapisdk.model.Singer
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.LoadMoreItem
import com.tencent.qqmusic.qplayer.ui.activity.loadMoreItemUI
import com.tencent.qqmusic.qplayer.ui.activity.songlist.CommonProfileActivity
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListActivity
import kotlinx.coroutines.flow.Flow

val plachImageID: Int = R.drawable.musicopensdk_icon_light

@Composable
fun singerPage(list: List<Singer>, loadMoreItem: LoadMoreItem? = null) {
    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (folder) = createRefs()
        Box(modifier = Modifier.constrainAs(folder) {
            height = Dimension.fillToConstraints
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
        }) {
            val activity = LocalContext.current as Activity

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                this.items(list.size) { index ->
                    val singer = list.elementAtOrNull(index) ?: return@items

                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            activity.startActivity(
                                Intent(activity, CommonProfileActivity::class.java)
                                    .putExtra(SongListActivity.KEY_SINGER_ID, singer.id)
                            )
                        }) {
                        singerItem(data = singer)
                    }

                }
                loadMoreItemUI(list.size, loadMoreItem = loadMoreItem)
            }
        }

    }
}

@Composable
fun singerPage(flow: Flow<PagingData<Singer>>) {
    val singers = flow.collectAsLazyPagingItems()
    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (folder) = createRefs()
        Box(modifier = Modifier.constrainAs(folder) {
            height = Dimension.fillToConstraints
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
        }) {
            val activity = LocalContext.current as Activity

            LazyColumn(state = rememberLazyListState(), modifier = Modifier.fillMaxSize()) {
                this@LazyColumn.itemsIndexed(singers) { index, singer ->
                    singer ?: return@itemsIndexed
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            activity.startActivity(
                                Intent(activity, CommonProfileActivity::class.java)
                                    .putExtra(SongListActivity.KEY_SINGER_ID, singer.id)
                            )
                        }) {
                        singerItem(data = singer)
                    }
                }
            }
        }

    }
}

@Composable
fun singerItem(data: Singer) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .height(70.dp)
    ) {
        Image(
            painter = rememberImagePainter(data?.singerPic150x150 ?: plachImageID,
                builder = {
                    crossfade(false)
                    placeholder(plachImageID)
                }),
            contentDescription = "",
            modifier = Modifier
                .padding(start = 10.dp)
                .size(60.dp)
                .clip(CircleShape)
        )

        Column {
            Text(text = "歌手ID ： ${data.id}")
            Text(text = "歌手 ： ${data.name}")
        }
    }
}