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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.openapisdk.model.Shelf
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListActivity
import com.tencent.qqmusic.qplayer.utils.PerformanceHelper
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun HomeRecommendPage(homeViewModel: HomeViewModel) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        homeViewModel.recommendation.shelfList.forEach {
            RecommendItem(it)
            Spacer(modifier = Modifier.height(16.dp))
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
