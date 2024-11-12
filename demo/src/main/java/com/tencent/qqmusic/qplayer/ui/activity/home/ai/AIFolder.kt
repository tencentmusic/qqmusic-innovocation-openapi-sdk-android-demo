package com.tencent.qqmusic.qplayer.ui.activity.home.ai

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.openapisdk.model.Folder
import com.tencent.qqmusic.qplayer.ui.activity.home.HomeViewModel
import com.tencent.qqmusic.qplayer.ui.activity.home.currentNewAiPage
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListActivity

val AIFloderHomePage = "base"
val newAi = "newAi"
val oldAi = "oldAi"

private var aiIndex = mutableStateOf(AIFloderHomePage)

@Composable
fun AIFolder(homeViewModel: HomeViewModel = viewModel(), backPrePage: () -> Unit) {
    val callback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (aiIndex.value == AIFloderHomePage) {
                    backPrePage.invoke()
                }
                aiIndex.value = AIFloderHomePage
            }
        }
    }
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(key1 = Unit, effect = {
        dispatcher?.addCallback(callback)
        homeViewModel.newAiFolder.clear()
        homeViewModel.fetchAiFolder()
        homeViewModel.fetchNewAiFolder(0)
        onDispose {
            callback.remove()
            aiIndex.value = AIFloderHomePage
        }
    })



    when (aiIndex.value) {
        AIFloderHomePage -> {
            Column {
                Text(text = "全类型 AI 歌单", modifier = Modifier
                    .height(40.dp)
                    .clickable {
                        aiIndex.value = newAi
                    })
                Text(text = "老版本歌单 ", modifier = Modifier
                    .height(40.dp)
                    .clickable {
                        aiIndex.value = oldAi
                    })
            }
        }

        newAi -> {
            newAiFolder(homeViewModel)
        }

        oldAi -> {
            oldAiFolder(data = homeViewModel.aiFolder)
        }
    }
}


@Composable
fun newAiFolder(homeViewModel: HomeViewModel) {
    val activity = LocalContext.current as Activity
    Column {
        LazyColumn {
            val data = homeViewModel.newAiFolder
            Log.d("wmy", "newAiFolder: ${data.size}")
            data.forEach {
                item {
                    val item = it
                    Box(modifier = Modifier
                        .wrapContentSize()
                        .padding(10.dp)
                        .clickable {
                            activity.startActivity(
                                Intent(activity, SongListActivity::class.java)
                                    .putExtra(SongListActivity.KEY_FOLDER_ID, item.id)
                            )
                        }) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Row {
                                Image(
                                    painter = rememberImagePainter(item.picUrl),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .padding(2.dp)
                                )
                                Column {
                                    Text(text = item.name)
                                    Text(text = "${(item.listenNum ?: 0) / 10000}万")
                                }
                            }
                            Text(text = item.introduction ?: "")
                            Divider(
                                thickness = 3.dp,
                                modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
                            )
                        }
                    }
                }
            }
            if (homeViewModel.showNewAiNextButton.value) {
                item {
                    Button(onClick = {
                        homeViewModel.fetchNewAiFolder(currentNewAiPage.value + 1)

                    }) {
                        Text(text = "下一页")
                    }
                }
            }
        }

    }
}


@Composable
fun oldAiFolder(data: List<Folder>) {
    val activity = LocalContext.current as Activity
    Column {
        Text(text = "歌单数量 ${data.size}")
        LazyColumn {
            items(data) {
                Box(modifier = Modifier
                    .wrapContentSize()
                    .padding(10.dp)
                    .clickable {
                        activity.startActivity(
                            Intent(activity, SongListActivity::class.java)
                                .putExtra(SongListActivity.KEY_FOLDER_ID, it.id)
                        )
                    }) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Row {
                            Image(
                                painter = rememberImagePainter(it.picUrl),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(50.dp)
                                    .padding(2.dp)
                            )
                            Column {
                                Text(text = it.name)
                                Text(text = "${(it.listenNum ?: 0) / 10000}万")
                            }
                        }
                        Divider(
                            thickness = 3.dp,
                            modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
                        )
                    }
                }
            }
        }
    }
}