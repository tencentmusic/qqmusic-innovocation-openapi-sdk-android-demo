package com.tencent.qqmusic.qplayer.ui.activity.download

import android.app.Activity
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.download.DownloadTask
import com.tencent.qqmusic.openapisdk.model.download.DownloadStatus
import com.tencent.qqmusic.qplayer.utils.QQMusicUtil
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlinx.coroutines.selects.select

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

    @Preview
    @Composable
    fun DownloadView() {
        val downloadTask = downloadViewModel.taskList
        val finishedSong = downloadTask.filter {
            it.task.getStatus() == DownloadStatus.SUCCESS
        }.map {
            it.task.getSongInfo()
        }
        val selectedTasks = arrayListOf<DownloadTask>()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "下载管理", fontSize = 18.sp) },
                    contentColor = Color.White
                )
            }
        ) {
            Column {
                if (downloadTask.isNotEmpty()) {
                    Button(onClick = {
                        if (selectedTasks.isEmpty()) {
                            Toast.makeText(this@DownloadActivity, "请先选择歌曲", Toast.LENGTH_SHORT).show()
                        } else {
                            OpenApiSDK.getDownloadApi().deleteDownloadSongList(selectedTasks.map { it.getSongInfo() }) {
                                val text = if (it) {
                                    "删除成功"
                                } else {
                                    "删除失败"
                                }
                                Toast.makeText(this@DownloadActivity, text, Toast.LENGTH_SHORT).show()
                            }
                            selectedTasks.clear()
                        }
                    }) {
                        Text(text = "删除")
                    }
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    this.items(downloadTask.size) { index ->
                        val task = downloadTask.elementAtOrNull(index) ?: return@items
                        DownloadItem(task, onClick = {
                            OpenApiSDK.getPlayerApi().playSongs(finishedSong, index)
                        }) {
                            if (it) {
                                selectedTasks.add(task.task)
                            } else {
                                selectedTasks.remove(task.task)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun DownloadItem(wrapper: DownloadTaskWrapper, onClick: () -> Unit, onChecked: (Boolean) -> Unit) {
        val activity = LocalContext.current as Activity
        val coroutineScope = rememberCoroutineScope()
        val item = wrapper.task
        val song = item.getSongInfo()
        val checked = remember {
            mutableStateOf(false)
        }

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
            val (checkBox, songInfo, progress, menu) = createRefs()
            Checkbox(checked = checked.value,
                modifier = Modifier.constrainAs(checkBox) {
                    start.linkTo(parent.start)
                },
                onCheckedChange = {
                    checked.value = checked.value.not()
                    onChecked.invoke(it)
            })
            Column(
                modifier = Modifier
                    .padding(start = 5.dp)
                    .constrainAs(songInfo) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(checkBox.end)
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
                        OpenApiSDK.getDownloadApi().deleteDownloadSong(item.getSongInfo()) {
                            val text = if (it) {
                                "删除成功"
                            } else {
                                "删除失败"
                            }
                            Toast.makeText(this@DownloadActivity, text, Toast.LENGTH_SHORT).show()
                        }
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