package com.tencent.qqmusic.qplayer.ui.activity.download

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.innovation.common.logging.MLog
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.download.DownloadTask
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.openapisdk.model.download.DownloadStatus
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerActivity
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerObserver
import com.tencent.qqmusic.qplayer.ui.activity.songlist.itemUI
import com.tencent.qqmusic.qplayer.utils.QQMusicUtil
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DownloadActivity: ComponentActivity() {

    private val downloadViewModel: DownloadViewModel by viewModels()

    companion object {
        val FROM_DOWNLOAD_SONG_PAGE = "fromDownloadSong"
    }

    private var fromDownloadSongPage = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fromDownloadSongPage = intent?.getBooleanExtra(FROM_DOWNLOAD_SONG_PAGE, false) ?: false
        downloadViewModel.init(fromDownloadSongPage)
        setContent {
            DownloadView()
        }
    }

    @Composable
    fun DownloadView() {
        val downloadTask = downloadViewModel.taskList
        val finishedSong = downloadTask.filter {
            it.task.getStatus() == DownloadStatus.SUCCESS
        }.map {
            it.task.getSongInfo()
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            this.items(downloadTask.size) { index ->
                val task = downloadTask.elementAtOrNull(index) ?: return@items
                DownloadItem(task) {
                    OpenApiSDK.getPlayerApi().playSongs(finishedSong, index)
                }
            }
        }
    }

    @Composable
    fun DownloadItem(wrapper: DownloadTaskWrapper, onClick: () -> Unit) {
        val activity = LocalContext.current as Activity
        val coroutineScope = rememberCoroutineScope()
        val item = wrapper.task
        val song = item.getSongInfo()

        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
                .height(70.dp)
                .clickable(enabled = fromDownloadSongPage) {
                    if (item.getStatus() == DownloadStatus.SUCCESS) {
                        onClick.invoke()
                    }
                }
        ) {
            val (songInfo, progress, menu) = createRefs()
            Column(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .constrainAs(songInfo) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                    }
                , horizontalAlignment = Alignment.Start) {

                val txtColor = Color.Black
                Text(text = song.songName, color = txtColor)

                val downloadState = when (item.getStatus()) {
                    DownloadStatus.DOWNLOADING -> {
                        "正在下载"
                    }
                    DownloadStatus.WAITING -> {
                        "等待下载"
                    }
                    DownloadStatus.FAILED -> {
                        val errCode = item.getErrInfo()?.errCode
                        "下载失败，错误码：$errCode"
                    }
                    DownloadStatus.PAUSED -> {
                        "下载暂停"
                    }
                    DownloadStatus.CANCELED -> {
                        "已取消"
                    }
                    DownloadStatus.SUCCESS -> {
                        "下载成功"
                    }
                    else -> {
                        "未知状态"
                    }
                }

                Row {
                    Image(
                        painter = painterResource(id = UiUtils.getQualityIcon(item.getQuality())),
                        contentDescription = null,
                        modifier = Modifier
                            .size(30.dp)
                    )

                    Text(text = downloadState,
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(start = 10.dp)
                    )
                }
            }

            val status = item.getStatus()
            Column(modifier = Modifier
                .fillMaxHeight()
                .constrainAs(menu) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    end.linkTo(parent.end)
                }) {
                if (status != DownloadStatus.SUCCESS) {
                    TextButton(
                        modifier = Modifier.height(20.dp),
                        contentPadding = PaddingValues(0.dp),
                        onClick = {
                            OpenApiSDK.getDownloadApi().pause(item)
                        }) {
                        Text(text = "暂停", fontSize = 10.sp)
                    }
                    TextButton(modifier = Modifier.height(20.dp),
                        contentPadding = PaddingValues(0.dp),
                        onClick = {
                            OpenApiSDK.getDownloadApi().resume(item)
                        }
                    ) {
                        Text(text = "继续下载", fontSize = 10.sp)
                    }
                }
                TextButton(modifier = Modifier.height(20.dp),
                    contentPadding = PaddingValues(0.dp),
                    onClick = {
                        OpenApiSDK.getDownloadApi().deleteDownloadTask(item, true)
                    }
                ) {
                    Text(text = "删除", fontSize = 10.sp)
                }
            }

            if (status == DownloadStatus.DOWNLOADING) {
                Column(
                    modifier = Modifier.constrainAs(progress) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        end.linkTo(menu.start, margin = 10.dp)
                    }
                ) {
                    item.getSpeedInfo()?.let {
                        val textToShow = "${QQMusicUtil.formatSize(it.speed, 1)}/s"
                        Text(text = textToShow)
                        Text(text = "${it.downloadedSize}/${it.totalSize}")
                    }
                }
            }
        }
    }
}