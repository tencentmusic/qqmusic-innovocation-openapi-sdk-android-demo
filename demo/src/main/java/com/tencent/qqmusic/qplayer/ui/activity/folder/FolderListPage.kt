package com.tencent.qqmusic.qplayer.ui.activity.folder

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.openapisdk.model.Folder
import com.tencent.qqmusic.qplayer.ui.activity.LoadMoreItem
import com.tencent.qqmusic.qplayer.ui.activity.loadMoreItemUI
import com.tencent.qqmusic.qplayer.ui.activity.main.TopBar
import com.tencent.qqmusic.qplayer.ui.activity.player.FloatingPlayerPage
import com.tencent.qqmusic.qplayer.ui.activity.songlist.CommonProfileActivity
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListActivity
import com.tencent.qqmusic.qplayer.utils.PerformanceHelper

//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

@Composable
fun FolderListScreen(folders: List<Folder>, loadMore: LoadMoreItem? = null) {
    Scaffold(
        topBar = { TopBar("歌单列表") }
    ) {
        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
            val (folder, player) = createRefs()

            Box(modifier = Modifier.constrainAs(folder) {
                height = Dimension.fillToConstraints
                top.linkTo(parent.top)
                bottom.linkTo(player.top)
            }) {
                FolderListPage(folders = folders, loadMore = loadMore)
            }
            Box(modifier = Modifier.constrainAs(player) {
                bottom.linkTo(parent.bottom)
            }) {
                FloatingPlayerPage()
            }
        }
    }
}

@OptIn(ExperimentalCoilApi::class, ExperimentalFoundationApi::class)
@Composable
fun FolderListPage(folders: List<Folder>, source:Int?=null, loadMore: LoadMoreItem? = null) {
    val activity = LocalContext.current as Activity
    val clipboardManager = LocalContext.current.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    val scrollState = rememberLazyListState()
    PerformanceHelper.MonitorListScroll(scrollState = scrollState, location = "FolderPage")
    // 我喜欢使用单独接口
    val myLikeId = folders.firstOrNull { folder -> folder.name == "我喜欢" }?.id
    LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize()) {
        items(folders.size) { index ->
            val folder = folders.getOrNull(index) ?: return@items
            val folderDeleted = folder.deleteStatus == 1
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp)
                    .combinedClickable(
                        onClick = {
                            if (folderDeleted) {
                                Toast
                                    .makeText(activity, "原歌单已删除，无法请求详情", Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                PerformanceHelper.monitorClick("FolderPage_SongListActivity")
                                activity.startActivity(
                                    Intent(activity, CommonProfileActivity::class.java).apply {
                                        putExtra(SongListActivity.KEY_FOLDER_ID, folder.id)
                                        putExtra(SongListActivity.KEY_IS_MY_LIKE_FOLDER, folder.id == myLikeId)
                                        if (source!=null){
                                            putExtra(SongListActivity.KEY_SOURCE, source)
                                        }
                                    }
                                )
                            }
                        },
                        onLongClick = {
                            // 复制文件夹名称到剪贴板
                            clipboardManager.setPrimaryClip(
                                ClipData.newPlainText(
                                    "FolderId",
                                    folder.id
                                )
                            )
                            Toast.makeText(activity, "歌单Id已复制到剪贴板", Toast.LENGTH_SHORT).show()
                        }
                    )
            ) {
                Image(
                    painter = rememberImagePainter(folder.picUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(2.dp)
                )

                // 原歌单已删除，需要置灰处理
                val textColor = if (folderDeleted) {
                    Color.Gray
                } else {
                    Color.Black
                }
                Column {
                    Text(text = folder.name, color = textColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(text = "${folder.songNum?.toString() ?: 0}首", color = textColor, fontSize = 10.sp)
                }
            }
        }
        loadMoreItemUI(folders.size, LoadMoreItem(loadMore?.needLoadMore ?: mutableStateOf(false), onLoadMore = {
            loadMore?.onLoadMore?.invoke()
        }))
    }
}