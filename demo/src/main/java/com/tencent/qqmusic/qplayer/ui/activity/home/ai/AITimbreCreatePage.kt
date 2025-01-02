package com.tencent.qqmusic.qplayer.ui.activity.home.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.ai.entity.AccInfo
import com.tencent.qqmusic.ai.function.base.IAudioRecord
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine.PlayState
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.qplayer.core.voiceplay.AICoverLinkPlayer
import com.tencent.qqmusic.qzdownloader.downloader.DownloadResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.regex.Matcher
import java.util.regex.Pattern


private const val TAG = "AITimbreCreatePage"
private var iAudioRecord: IAudioRecord? = null
private const val FILE_NAME = "timbreRecord.m4a"
private val pattern = Pattern.compile("(\\[\\d{2}:\\d{2}\\.\\d{2}\\])(.+)")
private var playTimeJob: Job? = null

@OptIn(ExperimentalCoilApi::class)
@Composable
fun AITimbreCreatePage(backPrePage: () -> Unit) {
    val aiViewModel: AIViewModel = viewModel()
    var taskStatus = remember {
        mutableStateOf(-1)
    }

    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Log.d(TAG, "handleOnBackPressed: ")
            backPrePage.invoke()
            remove()
        }
    }

    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(key1 = dispatcher) {
        dispatcher?.addCallback(callback)
        onDispose {
            callback.remove() // 移除回调
        }
    }

    val playStatus = remember {
        mutableStateOf(0)
    }

    val playLink = remember {
        mutableStateOf("")
    }


    LaunchedEffect(Unit) {
        var task = -1
        while (task == -1 || task == 1) {
            delay(1000)
            aiViewModel.queryPersonalTimbreGenerateStatus {
                task = it ?: -1
                taskStatus.value = task
            }
        }
        taskStatus.value = task
    }

    LaunchedEffect(key1 = Unit) {
        aiViewModel.getTimbreSongList()
    }

    var accinfo = remember {
        mutableStateOf<AccInfo?>(null)
    }

    val lyricList = remember {
        mutableStateOf<List<String>>(emptyList())
    }
    val playTime = remember {
        mutableStateOf(0L)
    }


    LaunchedEffect(accinfo.value) {
        aiViewModel.getWorkLink(accinfo.value?.songMid, null) {
            val mLyricList: MutableList<String> = mutableListOf()
            val lyric = it?.lyric ?: ""
            playLink.value = it?.audioLink ?: ""
            lyricList.value = lyric.split("\n")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(modifier = Modifier.padding(top = 10.dp)) {
            for (item in aiViewModel.createTimbreSongList) {
                item {
                    Box(modifier = Modifier
                        .padding(start = 10.dp, end = 10.dp)
                        .clickable {
                            accinfo.value = item
                        }) {
                        Column {
                            Text(text = item?.songName ?: "")
                            if (accinfo.value?.songId == item?.songId) {
                                Text(text = "已选中")
                            }
                        }
                    }
                }
            }
        }
        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp)
        ) {
            val (record, songCover, songTitle, upload, lyric) = createRefs()
            Image(
                painter = rememberImagePainter(accinfo.value?.cover ?: ""),
                "",
                modifier = Modifier
                    .width(60.dp)
                    .height(60.dp)
                    .zIndex(1f)
                    .clip(CircleShape)
                    .constrainAs(songCover) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                    }
            )

            Text(modifier = Modifier.constrainAs(songTitle) {
                top.linkTo(songCover.top)
                bottom.linkTo(songCover.bottom)
                start.linkTo(songCover.end, margin = 10.dp)
            }, text = accinfo.value?.songName ?: "")


            LazyColumn(modifier = Modifier
                .height(200.dp)
                .constrainAs(lyric) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(songTitle.bottom)
                    bottom.linkTo(record.top)
                }) {
                lyricList.value.map {
                    item { Text(it) }
                }
            }


            Box(modifier = Modifier
                .height(190.dp)
                .constrainAs(record) {
                    bottom.linkTo(upload.top, margin = 10.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }) {

                Column {
                    Button(onClick = {
                        if (playStatus.value == PlayState.MEDIAPLAYER_STATE_STARTED) {
                            aiViewModel.pause()
                        } else if (playStatus.value == PlayState.MEDIAPLAYER_STATE_PAUSED) {
                            aiViewModel.resume()
                            if (playStatus.value != 0) {
                                playStatus.value = 0
                                playTimeJob?.cancel()
                                aiViewModel.stopPlayCoverLink()
                            } else {
                                playLinkPage(aiViewModel, playLink, playStatus)
                            }
                        } else {
                            playLinkPage(aiViewModel, playLink, playStatus)
                        }
                    }) {
                        val text = when (playStatus.value) {
                            PlayState.MEDIAPLAYER_STATE_STARTED -> {
                                "暂停"
                            }

                            PlayState.MEDIAPLAYER_STATE_PAUSED -> {
                                "继续"
                            }

                            else -> {
                                "播放伴奏"
                            }
                        }
                        Text(text)
                    }
                    RequestAudioPermissionScreen(aiViewModel)
                }
            }
            Button(modifier = Modifier.constrainAs(upload) {
                start.linkTo(record.start)
                end.linkTo(record.end)
                bottom.linkTo(parent.bottom)
            }, onClick = {
                aiViewModel.generateTimbre(FILE_NAME, aiViewModel.fileDir)
            }) {
                if (taskStatus.value == 0) {
                    Text("未生成过音色，生成音色")
                } else if (taskStatus.value == 2) {
                    Text("音色已经生成，点击重新生成音色")
                } else if (taskStatus.value == 1) {
                    Text("音色生成中")
                } else {
                    Text("音色生成失败，点击重新生成音色")
                }
            }
        }
    }
}

private fun getRecord(fileName: String, aiViewModel: AIViewModel): IAudioRecord? {
    return aiViewModel.getRecord(fileName, aiViewModel.fileDir)
}

@Composable
fun RequestAudioPermissionScreen(aiViewModel: AIViewModel) {
    val context = LocalContext.current
    var hasAudioPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasAudioPermission.value = isGranted
    }

    val recordState = remember {
        mutableStateOf(false)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (hasAudioPermission.value) {
            Text("录音权限已授予，可以进行录音操作")
            Button(modifier = Modifier, onClick = {
                if (iAudioRecord == null) {
                    iAudioRecord = getRecord(FILE_NAME, aiViewModel)
                    iAudioRecord?.start()
                    recordState.value = true
                } else {
                    iAudioRecord?.stop()
                    iAudioRecord = null
                    recordState.value = false
                }

            }) {
                if (recordState.value) {
                    Text("停止")
                } else {
                    Text("录制")
                }
            }
        } else {
            Text("录音权限未授予")
            Button(onClick = {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }) {
                Text("请求录音权限")
            }
        }

    }
}


private fun timeStrToMs(timeStr: String): Long {
    val timeList = timeStr.split(":")
    val minute = timeList[0].toInt()
    val (second, millisecond) = timeList[1].split(".").map { it.toInt() }
    return minute * 60 * 1000 + second * 1000L + millisecond
}

private fun playLinkPage(aiViewModel: AIViewModel, link: MutableState<String>, playStatus: MutableState<Int>) {
    aiViewModel.playLink(link.value, null, null, object : AICoverLinkPlayer.PlayEventListener {
        override fun onDownloadFailed(url: String?, result: DownloadResult?) {

        }

        override fun onDownloadSucceed(url: String?, result: DownloadResult?) {

        }

        override fun onDownloadProgress(url: String?, totalSize: Long, downSize: Long, writeSize: Long) {

        }

        override fun onPlayStateChange(state: Int) {
            playStatus.value = state
        }

        override fun onPlayProgressChange(curPlayTime: Int, duration: Int) {

        }
    })

}