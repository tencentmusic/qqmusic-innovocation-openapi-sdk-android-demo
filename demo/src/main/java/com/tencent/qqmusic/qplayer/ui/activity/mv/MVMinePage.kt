package com.tencent.qqmusic.qplayer.ui.activity.mv

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.tencent.qqmusic.edgemv.data.MediaResDetail
import com.tencent.qqmusic.qplayer.ui.activity.search.plachImageID

@Composable
fun MVResDetailPage(list: List<MediaResDetail>) {
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
                    MVResDetaiItem(data = mvInfo)
                }
            }
        }

    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun MVResDetaiItem(data: MediaResDetail) {
    val activity = LocalContext.current as Activity
    Box(modifier = Modifier.clickable {
        activity.startActivity(
            Intent(activity, MVPlayerActivity::class.java).apply {
                putExtra(MVPlayerActivity.MV_RES, data)
            })
    }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
                .height(120.dp)
        ) {
            Image(
                painter = rememberImagePainter(
                    data.coverImage ?: plachImageID,
                    builder = {
                        crossfade(false)
                        placeholder(plachImageID)
                    }),
                contentDescription = "",
                modifier = Modifier
                    .padding(start = 5.dp, end = 5.dp)
                    .size(110.dp)
                    .clip(RoundedCornerShape(40))
            )
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val singer: String? = if ((data.singers?.size ?: 0) >= 2) {
                    data.singers?.joinToString(",") { it.name ?: "" }
                } else {
                    data.singerName
                }
                Text(text = "标题 ： ${data.title}")
                Text(text = "歌手 ： $singer")
            }
        }
    }
}