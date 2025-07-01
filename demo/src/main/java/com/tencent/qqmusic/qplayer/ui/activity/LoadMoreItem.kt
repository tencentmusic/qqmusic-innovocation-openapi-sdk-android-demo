package com.tencent.qqmusic.qplayer.ui.activity

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class LoadMoreItem(val needLoadMore: State<Boolean>, val onLoadMore: () -> Unit)

fun LazyListScope.loadMoreItemUI(key: Any, loadMoreItem: LoadMoreItem? = null) {
    if (loadMoreItem?.needLoadMore?.value == true) {
        item {
            Box(modifier = Modifier.fillParentMaxWidth().height(50.dp), contentAlignment = Alignment.Center)  {
                Text("正在加载更多...")
            }
            LaunchedEffect(key) {
                loadMoreItem.onLoadMore()
            }
        }
    } else  {
        item {
            Box(modifier = Modifier.fillParentMaxWidth().height(50.dp), contentAlignment = Alignment.Center)  {
                Text("已全部加载")
            }
        }
    }
}