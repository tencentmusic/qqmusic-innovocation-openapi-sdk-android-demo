@file:OptIn(ExperimentalCoilApi::class)

package com.tencent.qqmusic.qplayer.ui.activity.search

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.openapisdk.model.SearchMVInfo
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.qplayer.ui.activity.mv.MVPlayerActivity

@Composable
fun MVPage(list: List<SearchMVInfo>) {
    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (folder) = createRefs()
        Box(modifier = Modifier.constrainAs(folder) {
            height = Dimension.fillToConstraints
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
        }) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                this.items(list.size) { index ->
                    val mvInfo = list.elementAtOrNull(index) ?: return@items
                    MVSearchItem(data = mvInfo)
                }
            }
        }

    }
}

@Composable
fun MVSearchItem(data: SearchMVInfo) {
    val activity = LocalContext.current as Activity
    Box(modifier = Modifier.clickable {
        activity.startActivity(
            Intent(activity, MVPlayerActivity::class.java).apply {
                putExtra(MVPlayerActivity.MV_ID, data.mvVid)
            })
    }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
                .height(80.dp)
        ) {
            Image(
                painter = rememberImagePainter(
                    data.picUrl ?: plachImageID,
                    builder = {
                        crossfade(false)
                        placeholder(plachImageID)
                    }),
                contentDescription = "",
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(75.dp)
                    .clip(CircleShape)
            )
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "标题 ： ${data.mvTitle}")
                Text(text = "歌手 ： ${data.singer_name}")
                Row {
                    if ((data.mvDolby4KSize ?: 0) > 0) {
                        Text(text = "杜比")
                    }
                    if ((data.mvExcellentSize ?: 0) > 0) {
                        Text(text = "臻品")
                    }
                }
                Text(text = "播放量 ： ${data.playCount}")

            }
        }

        AppScope.launchIO {

        }
    }
}