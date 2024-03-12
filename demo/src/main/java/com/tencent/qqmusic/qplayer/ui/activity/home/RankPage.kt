package com.tencent.qqmusic.qplayer.ui.activity.home

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import com.google.accompanist.flowlayout.FlowRow
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListActivity

/**
 * Created by tannyli on 2022/12/21.
 * Copyright (c) 2022 TME. All rights reserved.
 */

@Composable
fun rankPage(homeViewModel: HomeViewModel){
    homeViewModel.fetchRankGroup()
    val activity = LocalContext.current as Activity
    var groups = homeViewModel.rankGroups
    LazyColumn{
        items(groups.count()) { it ->
            val rankGroup = groups.getOrNull(it) ?: return@items
            val rankList = rankGroup.rankList ?: emptyList()
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rankGroup.groupName,
                    color = Color.Black,
                    fontSize = 18.sp
                )
            }
            FlowRow {
                repeat(rankList.size) {
                    val rank = rankList.getOrNull(it) ?: return@repeat
                    Box(modifier = Modifier.wrapContentSize().padding(10.dp).clickable {
                        activity.startActivity(
                            Intent(activity, SongListActivity::class.java)
                            .putExtra(SongListActivity.KEY_RANK_ID, rank.id))
                    }) {
                        Image(
                            // 排行榜列表用 topBannerPic, 详情页内使用 topHeaderPic
                            painter = rememberImagePainter(rank.topBannerPic),
                            contentDescription = null,
                            modifier = Modifier
                                .width(150.dp).height(150.dp)
                                .padding(2.dp),

                        )
                        Text(
                            modifier = Modifier.wrapContentSize().align(Alignment.BottomStart).padding(10.dp),
                            text = "${(rank.listenNum ?: 0) / 10000}万",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Text(
                            modifier = Modifier.wrapContentSize().align(Alignment.TopCenter).padding(10.dp),
                            text = rank.name,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }

                }
            }
        }
    }
}