package com.tencent.qqmusic.qplayer.ui.activity.folder

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.openapisdk.model.Folder
import com.tencent.qqmusic.qplayer.ui.activity.main.TopBar
import com.tencent.qqmusic.qplayer.ui.activity.player.FloatingPlayerPage
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListActivity

//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

@Composable
fun FolderScreen(folders: List<Folder>) {
    Scaffold(
        topBar = { TopBar() }
    ) {
        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
            val (folder, player) = createRefs()

            Box(modifier = Modifier.constrainAs(folder) {
                height = Dimension.fillToConstraints
                top.linkTo(parent.top)
                bottom.linkTo(player.top)
            }) {
                FolderPage(folders = folders)
            }
            Box(modifier = Modifier.constrainAs(player) {
                bottom.linkTo(parent.bottom)
            }) {
                FloatingPlayerPage()
            }
        }
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun FolderPage(folders: List<Folder>) {
    val activity = LocalContext.current as Activity

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(folders.size) { index ->
            val folder = folders.getOrNull(index) ?: return@items
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable {
                        activity.startActivity(
                            Intent(activity, SongListActivity::class.java)
                                .putExtra(SongListActivity.KEY_FOLDER_ID, folder.id)
                        )
                    }
            ) {
                Image(
                    painter = rememberImagePainter(folder.picUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .padding(2.dp)
                )
                Column {
                    Text(text = folder.name)
                    Text(text = "${folder.songNum?.toString() ?: 0}首")
                }
            }
        }
    }
}