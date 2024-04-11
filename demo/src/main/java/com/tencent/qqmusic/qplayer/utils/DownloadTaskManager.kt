package com.tencent.qqmusic.qplayer.utils

import android.widget.Toast
import com.tencent.qqmusic.innovation.common.logging.MLog
import com.tencent.qqmusic.innovation.common.util.UtilContext
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.download.DownloadError
import com.tencent.qqmusic.openapisdk.core.download.DownloadEvent
import com.tencent.qqmusic.openapisdk.core.download.DownloadListener
import com.tencent.qqmusic.openapisdk.core.download.DownloadTask
import com.tencent.qqmusic.openapisdk.model.SongInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object DownloadTaskManager {

    private const val TAG = "DownloadTaskManager"

    private val listener = object : DownloadListener {
        override fun onEvent(event: DownloadEvent, task: DownloadTask?) {
            if (event == DownloadEvent.DOWNLOAD_TASK_ADDED) {
                showToast("${task?.getSongInfo()?.songName} 下载任务创建成功")
            }
        }

        override fun onCreateTaskError(song: SongInfo, err: DownloadError) {
            MLog.e(TAG, "onCreateTaskError, song: ${song.songName}")
            val txt = "${song.songName} 下载失败，错误码：$err"
            showToast(txt)
        }

        override fun onDownloadError(task: DownloadTask, err: DownloadError) {
            MLog.e(TAG, "onDownloadError, song: ${task.getSongInfo().songName}")
        }
    }

    fun init() {
        OpenApiSDK.getDownloadApi().registerDownloadListener(listener)
    }

    private fun showToast(txt: String) {
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(UtilContext.getApp(), txt, Toast.LENGTH_LONG).show()
        }
    }
}