package com.tencent.qqmusic.qplayer.ui.activity.home

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.flowlayout.FlowRow
import com.tencent.qqmusic.openapisdk.model.Category
import com.tencent.qqmusic.qplayer.ui.activity.folder.FolderActivity

//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomePage(categories: List<Category>) {
    val activity = LocalContext.current as Activity

    LazyColumn {
        items(categories.count()) { it ->
            val topCategory = categories.getOrNull(it) ?: return@items
            val subCategories = topCategory.subCategory ?: emptyList()

            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
) {
                Text(
                    text = topCategory.name,
                    color = Color.Gray,
                    fontSize = 18.sp
                )
            }

            FlowRow {
                repeat(subCategories.size) {
                    val sub = subCategories.getOrNull(it) ?: return@repeat
                    val subId = sub.id
                    Box(
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(16.dp)
                            .clickable {
                                activity.startActivity(
                                    Intent(activity, FolderActivity::class.java)
                                        .putIntegerArrayListExtra(
                                            FolderActivity.KEY_CATEGORY_IDS,
                                            arrayListOf(topCategory.id, subId)
                                        )
                                )
                            },
                        contentAlignment = Alignment.Center
) {
                        Text(
                            text = sub.name,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}