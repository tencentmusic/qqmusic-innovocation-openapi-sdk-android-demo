package com.tencent.qqmusic.qplayer.ui.activity.home.other.ordersong

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun OtherPage() {
    LazyColumn {
        item {
            Text("多人点歌", modifier = Modifier.clickable {

            })
        }
    }
}