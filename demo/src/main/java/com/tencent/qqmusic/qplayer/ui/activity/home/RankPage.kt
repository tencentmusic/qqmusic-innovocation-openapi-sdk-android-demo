package com.tencent.qqmusic.qplayer.ui.activity.home

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.google.accompanist.flowlayout.FlowRow
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListActivity
import com.tencent.qqmusic.qplayer.utils.PerformanceHelper

/**
 * Created by tannyli on 2022/12/21.
 * Copyright (c) 2022 TME. All rights reserved.
 */

@OptIn(ExperimentalCoilApi::class)
@Composable
fun rankPage(homeViewModel: HomeViewModel){
    LaunchedEffect(Unit) {
        homeViewModel.fetchRankGroup()
    }
    val activity = LocalContext.current as Activity
    val groups by remember { homeViewModel.rankGroups }
    val listState = rememberLazyListState()
    PerformanceHelper.MonitorListScroll(scrollState = listState, location = "rankPage")
    LazyColumn(
        state = listState
    ){
        items(groups.count()) { it ->
            val rankGroup = groups.getOrNull(it) ?: return@items
            val rankList = rankGroup.rankList ?: emptyList()
            val rankGridStyle = rememberSaveable { mutableStateOf(true) }
            Row (
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rankGroup.groupName,
                    color = Color.Black,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.width(5.dp))
                IconButton(onClick = {
                    rankGridStyle.value = !rankGridStyle.value
                }) {
                    Icon(imageVector = if (rankGridStyle.value) Icons.Default.Menu else Icons.Default.List, contentDescription = null)
                }
            }
            FlowRow {
                repeat(rankList.size) {
                    val rank = rankList.getOrNull(it) ?: return@repeat
                    Row(
                        modifier = Modifier
                            .wrapContentSize()
                            .clickable {
                                PerformanceHelper.monitorClick("rankPage_SongListActivity")
                                activity.startActivity(
                                    Intent(activity, SongListActivity::class.java)
                                        .putExtra(SongListActivity.KEY_RANK_ID, rank.id)
                                )
                            }
                    ) {
                        Box(modifier = Modifier
                            .wrapContentSize()
                            .padding(10.dp)
                            ) {
                            Image(
                                // 排行榜列表用 topBannerPic, 详情页内使用 topHeaderPic
                                painter = rememberImagePainter(rank.topBannerPic),
                                contentDescription = null,
                                modifier = Modifier
                                    .width(150.dp)
                                    .height(150.dp)
                                    .padding(2.dp),

                                )
                            Text(
                                modifier = Modifier
                                    .wrapContentSize()
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp),
                                text = "${(rank.listenNum ?: 0) / 10000}万",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Text(
                                modifier = Modifier
                                    .wrapContentSize()
                                    .align(Alignment.TopCenter)
                                    .padding(10.dp),
                                text = rank.name,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                        if (!rankGridStyle.value) {
                            Column(
                                modifier = Modifier.padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                rank.songList?.forEachIndexed { index, songInfo ->
                                    Text(text = buildAnnotatedString {
                                        var len = 0
                                        append("${index + 1} ")
                                        addStyle(SpanStyle(fontStyle = FontStyle.Italic, color = MaterialTheme.colors.primary, fontWeight = MaterialTheme.typography.caption.fontWeight), len, length)
                                        len = length
                                        append(songInfo.songName)
                                        addStyle(SpanStyle(fontStyle = FontStyle.Normal, color = MaterialTheme.colors.primaryVariant, fontWeight = MaterialTheme.typography.subtitle2.fontWeight), len , length)
                                        len = length
                                        songInfo.singerName.takeIf {singerName-> !singerName.isNullOrEmpty() }?.let { singerName-> "-$singerName"}?.let { singer->
                                            append(singer)
                                            addStyle(SpanStyle(fontStyle = FontStyle.Normal, color = MaterialTheme.colors.primaryVariant, fontWeight = MaterialTheme.typography.subtitle1.fontWeight), len, length)
                                        }
                                    }, style = MaterialTheme.typography.subtitle1, color = MaterialTheme.colors.primaryVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}