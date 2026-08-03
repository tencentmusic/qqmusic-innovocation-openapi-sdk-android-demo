package com.tencent.qqmusic.qplayer.ui.activity.home

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.openapisdk.model.Banner
import com.tencent.qqmusic.openapisdk.model.BannerPosition
import com.tencent.qqmusic.openapisdk.model.Shelf
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.qplayer.ui.activity.login.WebViewActivity
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListActivity
import com.tencent.qqmusic.qplayer.utils.PerformanceHelper
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

@Composable
fun HomeRecommendPage(homeViewModel: HomeViewModel) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row {
            val banners = homeViewModel.selectedBanners
            if (banners.isNotEmpty()) {
                Banner(
                    items = banners,
                    positions = homeViewModel.bannerConfig,
                    selectedPositionIndex = homeViewModel.selectedBannerPositionIndex,
                    onPositionSelected = { homeViewModel.selectedBannerPositionIndex = it }
                )
            }
        }
        homeViewModel.recommendation.shelfList.forEach {
            RecommendItem(it)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BannerPositionSelector(
    positions: List<BannerPosition>,
    selectedIndex: Int,
    onPositionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = positions.getOrNull(selectedIndex)?.positionName ?: ""
    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = selectedName,
                color = Color.White,
                fontSize = 12.sp
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            positions.forEachIndexed { index, position ->
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onPositionSelected(index)
                    }
                ) {
                    Text(text = position.positionName)
                }
            }
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun Banner(
    items: List<Banner>,
    positions: List<BannerPosition> = emptyList(),
    selectedPositionIndex: Int = 0,
    onPositionSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    autoScroll: Boolean = true,
    scrollDelay: Long = 3000L
) {
    val pagerState = rememberPagerState()
    val context = LocalContext.current

    LaunchedEffect(pagerState, items) {
        if (autoScroll) {
            while (true) {
                yield()
                delay(scrollDelay)
                if (items.isNotEmpty()) {
                    pagerState.animateScrollToPage(
                        page = (pagerState.currentPage + 1) % items.size
                    )
                }
            }
        }
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            HorizontalPager(
                count = items.size,
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray)
                        .clickable { handleBannerClick(context, items[page]) }
                ) {
                    Image(
                        painter = rememberImagePainter(items[page].pic),
                        contentDescription = null,
                        contentScale = ContentScale.Inside,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.medium)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "tag:" + (items[page].tag ?: ""),
                            color = Color.Yellow,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "标题:" + items[page].title,
                            color = Color.Yellow,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .padding(0.dp, 6.dp)
                        )
                        Text(
                            text = "描述:" + items[page].desc,
                            color = Color.Yellow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                        )
                        // 类型: 1.歌曲  2.歌单  3.专辑  4.H5  5.排行榜  6.电台  7.H5收银台
                        Text(
                            text = "类型:${items[page].type}#" + when (items[page].type) {
                                1 -> "歌曲"
                                2 -> "歌单"
                                3 -> "专辑"
                                4 -> "H5"
                                5 -> "排行榜"
                                6 -> "电台"
                                7 -> "H5收银台"
                                else -> "未知type"
                            },
                            color = Color.Yellow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                        )
                        // 场景值：仅 type 为 7（H5收银台）时展示
                        if (items[page].type == 7) {
                            Text(
                                text = "场景值:" + (items[page].scene ?: ""),
                                color = Color.Yellow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                            )
                        }
                        Text(
                            text = "资源Id:" + items[page].contentId,
                            color = Color.Yellow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                        )
                    }
                }
            }

            if (positions.size > 1) {
                BannerPositionSelector(
                    positions = positions,
                    selectedIndex = selectedPositionIndex,
                    onPositionSelected = onPositionSelected,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(items.size) { index ->
                val color = if (pagerState.currentPage == index) Color.Yellow else Color.Gray
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

private fun handleBannerClick(context: android.content.Context, banner: Banner) {
    when (banner.type) {
        1 -> {
            val songId = banner.contentId.toLongOrNull()
            if (songId != null) {
                AppScope.launchIO {
                    OpenApiSDK.getOpenApi().fetchSongInfoBatch(listOf(songId)) { songInfoList ->
                        if (songInfoList.isSuccess() && songInfoList.data != null) {
                            val songs = songInfoList.data!!
                            val result = OpenApiSDK.getPlayerApi().playSongs(songs, 0)
                            if (result != 0) {
                                AppScope.launchUI {
                                    val msg = if (result == PlayDefine.PlayError.PLAY_ERR_CANNOT_PLAY && songs.isNotEmpty()) {
                                        "播放失败 错误码：$result， 错误信息：${songs[0].unplayableMsg}"
                                    } else {
                                        "播放失败 错误码：$result"
                                    }
                                    UiUtils.showToast(msg)
                                }
                            }
                        } else {
                            AppScope.launchUI {
                                UiUtils.showToast("获取歌曲信息失败")
                            }
                        }
                    }
                }
            } else {
                UiUtils.showToast("歌曲ID无效")
            }
        }
        2 -> {
            if (banner.contentId.isNotEmpty()) {
                context.startActivity(
                    Intent(context, SongListActivity::class.java)
                        .putExtra(SongListActivity.KEY_FOLDER_ID, banner.contentId)
                )
            } else {
                UiUtils.showToast("歌单ID为空")
            }
        }
        3 -> {
            if (banner.contentId.isNotEmpty()) {
                context.startActivity(
                    Intent(context, SongListActivity::class.java)
                        .putExtra(SongListActivity.KEY_ALBUM_ID, banner.contentId)
                )
            } else {
                UiUtils.showToast("专辑ID为空")
            }
        }
        4, 7 -> {
            val url = banner.url
            if (!url.isNullOrEmpty()) {
                WebViewActivity.start(context, url)
            } else {
                UiUtils.showToast("url为空")
            }
        }
        5 -> {
            val rankId = banner.contentId.toIntOrNull()
            if (rankId != null) {
                context.startActivity(
                    Intent(context, SongListActivity::class.java)
                        .putExtra(SongListActivity.KEY_RANK_ID, rankId)
                )
            } else {
                UiUtils.showToast("排行榜ID无效")
            }
        }
        6 -> {
            if (banner.contentId.isNotEmpty()) {
                context.startActivity(
                    Intent(context, SongListActivity::class.java)
                        .putExtra(SongListActivity.KEY_FOLDER_ID, banner.contentId)
                )
            } else {
                UiUtils.showToast("电台ID为空")
            }
        }
        else -> {
            val url = banner.url
            if (!url.isNullOrEmpty()) {
                WebViewActivity.start(context, url)
            } else {
                UiUtils.showToast("未知类型: ${banner.type}")
            }
        }
    }
}

@Composable
fun RecommendItem(itemData: Shelf) {
    if (itemData.cardList.isNotEmpty()) {
        if (itemData.cardList[0].type == 200) {
            RecommendSongs(itemData)
        } else if (itemData.cardList[0].type == 500) {
            RecommendFolderList(itemData)
        }
    }
}

@Composable
private fun RecommendSongs(itemData: Shelf) {
    val coroutineScope = rememberCoroutineScope()
    Text(
        text = itemData.title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val songIdList = itemData.cardList.map { it.id.toLongOrNull() ?: 0L }
        itemData.cardList.forEach {
            SongItem(
                it.cover, title = it.title, description = it.subTitle
            ) {
                coroutineScope.launch(Dispatchers.IO) {
                    OpenApiSDK.getOpenApi().fetchSongInfoBatch(songIdList) { songInfoList ->
                        if (songInfoList.isSuccess() && songInfoList.data != null) {
                            val songIndex = songInfoList.data!!.indexOfFirst { find -> it.id == find.songId.toString() }
                            val result = OpenApiSDK
                                .getPlayerApi()
                                .playSongs(
                                    songInfoList.data!!, songIndex
                                )

                            if (result == PlayDefine.PlayError.PLAY_ERR_CANNOT_PLAY) {
                                UiUtils.showToast("播放失败 错误码：$result， 错误信息：${songInfoList.data!![songIndex].unplayableMsg}")
                            } else if (result != 0) {
                                UiUtils.showToast("播放失败 错误码：$result")
                            }
                        } else {
                            UiUtils.showToast("获取歌曲信息失败")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendFolderList(itemData: Shelf) {
    Text(
        text = itemData.title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        itemData.cardList.forEach {
            ImageCard(it.cover, description = it.title, it.subTitle, it.id)
        }
    }
}


@Composable
fun SongItem(imageUrl: String, title: String, description: String, clickSongItem: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                clickSongItem.invoke()
            }) {
        Image(
            painter = rememberImagePainter(imageUrl),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = description, fontSize = 14.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ImageCard(imageUrl: String, description: String, subTitle: String = "", folderId: String) {
    val activity = LocalContext.current as Activity
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        Box {
            Image(painter = rememberImagePainter(imageUrl),
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(120.dp)
                    .fillMaxWidth()
                    .clickable {
                        PerformanceHelper.monitorClick("FolderPage_SongListActivity")
                        activity.startActivity(
                            Intent(
                                activity, SongListActivity::class.java
                            ).putExtra(SongListActivity.KEY_FOLDER_ID, folderId)
                        )
                    })
            if (subTitle.isNotEmpty()) {
                Text(
                    text = subTitle,
                    fontSize = 9.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .background(Color.White)
                )
            }
        }
        Text(
            text = description,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
