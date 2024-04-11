package com.tencent.qqmusic.qplayer.ui.activity.download

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.tencent.qqmusic.innovation.common.logging.MLog
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.download.DownloadError
import com.tencent.qqmusic.openapisdk.core.download.DownloadEvent
import com.tencent.qqmusic.openapisdk.core.download.DownloadListener
import com.tencent.qqmusic.openapisdk.core.download.DownloadTask
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.openapisdk.model.download.DownloadStatus

class DownloadViewModel: ViewModel() {

    private val TAG = "DownloadViewModel"

    val taskList = mutableStateListOf<DownloadTaskWrapper>()

    private val listener = object : DownloadListener {
        override fun onEvent(event: DownloadEvent, task: DownloadTask?) {
            MLog.d(TAG, "onEvent: $event, song: ${task?.getSongInfo()?.songName}")
            when(event) {
                DownloadEvent.DOWNLOAD_START -> {
                    updateTaskList(task)
                }
                DownloadEvent.DOWNLOAD_SIZE_CHANGE -> {
                    updateTaskList(task)
                }
                DownloadEvent.DOWNLOAD_PAUSED -> {
                    updateTaskList(task)
                }
                DownloadEvent.DOWNLOAD_SUCCESS -> {
                    updateTaskList(task)
                }
                DownloadEvent.DOWNLOAD_TASK_ADDED -> {
                    getDownloadTasks()
                }
                DownloadEvent.DOWNLOAD_TASK_REMOVED -> {
                    getDownloadTasks()
                }
                else -> {

                }
            }
        }

        override fun onCreateTaskError(song: SongInfo, err: DownloadError) {
            MLog.e(TAG, "onCreateTaskError, song: ${song.songName}")
        }

        override fun onDownloadError(task: DownloadTask, err: DownloadError) {
            MLog.e(TAG, "onDownloadError, song: ${task.getSongInfo().songName}")
            updateTaskList(task)
        }
    }

    private var getFinishTask = false

    fun init(getFinishTask: Boolean) {
        OpenApiSDK.getDownloadApi().registerDownloadListener(listener)
        this.getFinishTask = getFinishTask
        getDownloadTasks()
    }

    override fun onCleared() {
        OpenApiSDK.getDownloadApi().unregisterDownloadListener(listener)
    }

    private fun updateTaskList(task: DownloadTask?) {
        if (task == null) {
            return
        }
        taskList.forEachIndexed { index, downloadTaskWrapper ->
            if (downloadTaskWrapper.task.getSongInfo().songId == task.getSongInfo().songId) {
                taskList[index] = DownloadTaskWrapper(task, downloadTaskWrapper.changed.not())
                return
            }
        }
    }

    private fun getDownloadTasks() {
        taskList.clear()
        OpenApiSDK.getDownloadApi().getDownloadTasks { tmpTaskList ->
            val finalTask = if (getFinishTask) {
                tmpTaskList.filter {
                    it.getStatus() == DownloadStatus.SUCCESS
                }
            } else {
                tmpTaskList
            }
            taskList.addAll(finalTask.map {
                DownloadTaskWrapper(it, false)
            })
        }
    }
}

data class DownloadTaskWrapper(
    var task: DownloadTask,
    var changed: Boolean
)