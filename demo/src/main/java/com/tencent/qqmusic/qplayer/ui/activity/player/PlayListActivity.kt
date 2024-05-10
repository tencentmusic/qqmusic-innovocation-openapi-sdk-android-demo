package com.tencent.qqmusic.qplayer.ui.activity.player

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.IMediaEventListener
import com.tencent.qqmusic.openapisdk.core.player.PlayerEvent
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

//
// Created by tylertan on 2021/11/25
// Copyright (c) 2021 Tencent. All rights reserved.
//

class PlayListActivity : ComponentActivity() {

    private val TAG = "PlayListActivity"
    val count = mutableStateOf(0)
    private var isFirst = true
    private val eventListener = object : IMediaEventListener {
        override fun onEvent(event: String, arg: Bundle) {
            when (event) {
                PlayerEvent.Event.API_EVENT_PLAY_LIST_CHANGED -> {
                    Log.d(TAG, "onEvent: ${OpenApiSDK.getPlayerApi().getPlayList().size}")
                    count.value++
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContent {
            SongListEdit(count)
        }
        OpenApiSDK.getPlayerApi().registerEventListener(eventListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        OpenApiSDK.getPlayerApi().unregisterEventListener(eventListener)

    }


    private fun getSongStateList(): MutableList<Pair<SongState, SongInfo>> {
        val list = OpenApiSDK.getPlayerApi().getPlayList().map {
            Pair(SongState(false), it)
        }
        return list as MutableList<Pair<SongState, SongInfo>>
    }


    @Composable
    fun SongListEdit(count: MutableState<Int>) {
        val countState = rememberUpdatedState(count.value)
        val constraintsScope = rememberCoroutineScope()
        var songList = remember {
            getSongStateList()
        }
        var editFlag = remember {
            mutableStateOf(false)
        }

        val observer = remember {
            PlayerObserver
        }


        if (!editFlag.value) {
            songList.clear()
            songList.addAll(getSongStateList())
        }


        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { finish() }
                )
        ) {
            val (topButton, listContent, topbackGround) = createRefs()
            Box(
                modifier = Modifier
                    .background(Color(255, 255, 255, 204))
                    .clickable { finish() }
                    .constrainAs(topbackGround) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(topButton.top)
                        top.linkTo(parent.top)
                    }
            )

            Box(modifier = Modifier
                .height(40.dp)
                .background(Color(255, 255, 255, 242))
                .constrainAs(topButton) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(listContent.top)
                }) {
                Text(
                    text = "共${songList.size}首"
                )
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Text(modifier = Modifier.align(Alignment.CenterVertically).padding(end = 40.dp),
                        text = "${OpenApiSDK.getPlayerApi().getPlayList().size}首歌")

                    if (!editFlag.value) {
                        Button(onClick = {
                            editFlag.value = true
                        }) {
                            Text(text = "编辑")
                        }
                    } else {
                        Button(onClick = {
                            editFlag.value = false
                            songList.forEach { it.first.select = false }
                        }) {
                            Text(text = "取消")
                        }
                        Button(onClick = {
                            constraintsScope.launch {
                                editFlag.value = false
                                val list = songList.filter { it.first.select }.map { it.second }
                                OpenApiSDK.getPlayerApi().deleteSongList(list)
                            }
                        }) {
                            Text(text = "删除")
                        }
                    }

                    Button(onClick = {
                        OpenApiSDK.getPlayerApi().clearPlayList()
                        finish()
                    }) {
                        Text(text = "清除全部")
                    }
                }

            }

            Box(modifier = Modifier
                .background(Color(255, 255, 255, 242))
                .height(500.dp)
                .constrainAs(listContent) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                }) {
                val listState = rememberLazyListState()

                LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                    this.items(songList) { item ->
                        Column(modifier = Modifier.clickable {
                            if (editFlag.value) {
                                item.first.select = item.first.select.not()
                                count.value++
                            } else {
                                val index = songList.indexOf(item)
                                OpenApiSDK.getPlayerApi().playSongs(songList.map { it.second }, index)
                            }
                        }) {
                            Row(modifier = Modifier.height(45.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (editFlag.value) {
                                    Checkbox(checked = item.first.select, onCheckedChange = {
                                        item.first.select = item.first.select.not()
                                        count.value++
                                    })
                                }
                                Text(text = item.second.songName, modifier = Modifier.padding(start = 10.dp))
                                if (item.second.vip == 1) {
                                    Image(
                                        painter = painterResource(R.drawable.pay_icon_in_cell_old),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .padding(start = 10.dp)
                                            .width(18.dp)
                                            .height(10.dp)
                                    )
                                }
                                if (item.second.hasQualityHQ()) {
                                    Image(
                                        painter = painterResource(R.drawable.hq_icon), contentDescription = null, modifier = Modifier
                                            .padding(start = 10.dp)
                                            .width(18.dp)
                                            .height(10.dp)
                                    )
                                }
                                Text(text = item.second.singerName ?: "", modifier = Modifier.padding(start = 10.dp))
                                if (observer.currentSong?.songId == item.second.songId) {
                                    Image(
                                        painter = painterResource(R.drawable.list_icon_playing), contentDescription = null, modifier = Modifier
                                            .padding(start = 10.dp)
                                            .width(30.dp)
                                            .height(30.dp)
                                    )
                                }
                            }
                            Divider(color = Color.Gray, thickness = 1.dp)
                        }
                    }
                    if (editFlag.value.not() && isFirst) {
                        constraintsScope.launch {
                            val index = songList.indexOfFirst { it.second.songId == observer.currentSong?.songId }
                            listState.animateScrollToItem(minOf(index, maxOf(0, index - 5)))
                            isFirst = false
                        }
                    }
                }
            }


        }
    }


    data class SongState(var select: Boolean)

}