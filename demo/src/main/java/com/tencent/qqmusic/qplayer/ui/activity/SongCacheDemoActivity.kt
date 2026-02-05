package com.tencent.qqmusic.qplayer.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.hologram.EdgeMvProvider
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.ui.activity.main.TopBar
import com.tencent.qqmusic.qplayer.ui.activity.songlist.PlayListParams
import com.tencent.qqmusic.qplayer.ui.activity.songlist.itemUI
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread

// 
// Created by clydeazhang on 2021/12/20 2:49 下午.
// Copyright (c) 2021 Tencent. All rights reserved.
//
@SuppressLint("SetTextI18n")
class SongCacheDemoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SongCacheDemoActivity"
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Scaffold(topBar = { TopBar("缓存测试页") },
                modifier = Modifier.semantics{ testTagsAsResourceId=true }) {
                CachePage()
            }
        }
    }

    @Preview
    @Composable
    private fun CachePage() {
        var usage by remember { mutableStateOf(TextFieldValue("200")) }
        var usageMV by remember { mutableStateOf(TextFieldValue("200")) }
        var cachedSongList by remember { mutableStateOf(listOf<SongInfo>()) }
        val coroutineScope = rememberCoroutineScope()
        var cachedSize by remember { mutableStateOf(0L) }
        var cachedMVSize by remember { mutableStateOf(0L) }
        var songCacheResult by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.White),
            verticalArrangement = Arrangement.Top
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    thread {
                        OpenApiSDK.getPlayerApi().getCurrentSongInfo()?.let { songInfo ->
                            songCacheResult = if(
                                OpenApiSDK.getSongCacheApi().isSongCached(songInfo = songInfo)){
                                "已缓存:${songInfo.songId}-《${songInfo.songName}》"
                            }else{
                                "未缓存:${songInfo.songId}-《${songInfo.songName}》"
                            }
                            UiUtils.showToast(songCacheResult)
                        }
                    }
                }) { Text(text = "查询当前播放歌曲是否已缓存") }
            }
            Text(text = songCacheResult)


            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    cachedSize = getCacheSize(true)
                }) {
                    Text(text = "获取已缓存歌曲大小")
                }

                Text(text = "缓存大小：$cachedSize Mb")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    cachedMVSize = getCacheSize(false)
                }) {
                    Text(text = "获取已缓存MV大小")
                }

                Text(text = "缓存大小：$cachedMVSize Mb")
            }

            Row {
                TextField(
                    value = usage,
                    label = {
                        Text(text = "输入歌曲缓存上限-Mb")
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    onValueChange = {
                        usage = it
                    },
                    modifier = Modifier.wrapContentSize()
                )

                Button(onClick = {
                    if (UiUtils.isStrInt(usage.text)) {
                        val ret = OpenApiSDK.getSongCacheApi().setCacheMaxSize(usage.text.toInt())
                        showSetCacheSizeRet(true, ret)
                    }
                }) {
                    Text(text = "设置")
                }
            }

            Row {
                TextField(
                    value = usageMV,
                    label = {
                        Text(text = "输入MV缓存上限-Mb")
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    onValueChange = {
                        usageMV = it
                    },
                    modifier = Modifier.wrapContentSize()
                )

                Button(onClick = {
                    if (UiUtils.isStrInt(usageMV.text)) {
                        OpenApiSDK.getProviderByClass(EdgeMvProvider::class.java)?.setCacheSize(usageMV.text.toInt())
                        showSetCacheSizeRet(false, 0)
                    }
                }) {
                    Text(text = "设置")
                }
            }

            Button(onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    OpenApiSDK.getSongCacheApi().clearAllCaches()
                    withContext(Dispatchers.Main) {
                        cachedSize = getCacheSize(true)
                    }
                }
            }) {
                Text(text = "清除缓存")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            cachedSongList = OpenApiSDK.getSongCacheApi().getCachedSongList()
                        }
                    }
                }) {
                    Text(text = "获取已缓存歌曲")
                }
                
                Text(text = "歌曲数量：${cachedSongList.size}首", modifier = Modifier.padding(start = 10.dp))
            }

            if (cachedSongList.isEmpty()) {
                Text(text = "暂无已缓存歌曲")
            } else {
                LazyColumn {
                    items(cachedSongList) {
                        itemUI(PlayListParams(cachedSongList, it, playCachedOnly = true))
                    }
                }
            }
        }
    }

    private fun showSetCacheSizeRet(isSong: Boolean, ret: Int) {
        if (ret == 0) {
            getCacheSize(isSong)
        } else {
            val errMap = mapOf(-1 to "sdk异常", -2 to "参数不合法", -200 to "播放进程未启动或已死亡")
            Toast.makeText(this,"设置失败:${errMap[ret]}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCacheSize(isSong: Boolean): Long {
        val cacheSize = if (isSong) {
            OpenApiSDK.getSongCacheApi().getCacheSize()
        } else {
            OpenApiSDK.getProviderByClass(EdgeMvProvider::class.java)?.getCachedMVSize() ?: 0
        }
        Log.d(TAG, "cache : $cacheSize cacheSize=${cacheSize / 1024 /1024} MB")
        Toast.makeText(this, "size: ${UiUtils.getFormatSize(cacheSize)}", Toast.LENGTH_SHORT).show()
        return cacheSize / 1024 / 1024
    }
}