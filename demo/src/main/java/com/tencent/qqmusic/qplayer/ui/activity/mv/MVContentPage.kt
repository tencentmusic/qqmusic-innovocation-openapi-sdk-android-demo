package com.tencent.qqmusic.qplayer.ui.activity.mv

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
//import androidx.compose.foundation.lazy.GridCells
//import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MVFunctionPage() {
    val activity = LocalContext.current as Activity

    val pages = mutableMapOf(
        "最新最热的MV" to MVPlayerActivity.Recommend,
        "Dolby视界专区" to MVPlayerActivity.Dolby_Content,
        "4K臻品世界" to MVPlayerActivity.Content_EXCELLENT,
    )

    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Fixed(2),
    ) {
        items(pages.keys.toList()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier
                .height(100.dp)
                .clickable {
                    activity.startActivity(
                        Intent(activity, MVPlayerActivity::class.java).apply {
                            putExtra(MVPlayerActivity.Content_Type, pages.get(it))
                        })
                }) {
                Text(text = it)
            }
        }
    }
}
