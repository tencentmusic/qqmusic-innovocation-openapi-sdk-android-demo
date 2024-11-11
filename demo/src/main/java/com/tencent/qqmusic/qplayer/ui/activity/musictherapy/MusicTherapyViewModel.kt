package com.tencent.qqmusic.qplayer.ui.activity.musictherapy

import android.os.Bundle
import android.view.Surface
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tencent.qqmusic.edgemv.data.MediaQuality
import com.tencent.qqmusic.edgemv.data.MediaResDetail
import com.tencent.qqmusic.edgemv.player.IPlayEventCallback
import com.tencent.qqmusic.edgemv.player.PlayError
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.musictherapy.AIMusicTherapyParam
import com.tencent.qqmusic.openapisdk.hologram.EdgeMvProvider
import com.tencent.qqmusic.openapisdk.model.musictherapy.MusicTherapyListData
import com.tencent.qqmusic.player.PlayerState
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.qplayer.core.player.proxy.PlayStateProxyHelper
import com.tencent.qqmusiccommon.SimpleMMKV
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class MusicTherapyViewModel : ViewModel() {
    companion object {
        private const val TAG = "MusicTherapyViewModel"
        private const val MV_VID = "MV_VID"
        const val LOADING = 1
        const val SUCCESS = 2
        const val ERROR = 3
    }
    val musicTherapyListData = mutableStateOf(
        MusicTherapyListData(),
        object : SnapshotMutationPolicy<MusicTherapyListData> {
            override fun equivalent(a: MusicTherapyListData, b: MusicTherapyListData): Boolean {
                return a === b
            }
        })
    val aiMusicTherapyParam = mutableStateOf(
        OpenApiSDK.getMusicTherapyApi().getCurrentPlayMusicTherapyParam().aiMusicTherapyParam,
        object : SnapshotMutationPolicy<AIMusicTherapyParam> {
            override fun equivalent(a: AIMusicTherapyParam, b: AIMusicTherapyParam): Boolean {
                return a === b
            }
        })
    val playState = mutableStateOf(OpenApiSDK.getMusicTherapyApi().getPlayState())
    val pageStatus = mutableStateOf(LOADING)
    val therapyInfo = mutableStateOf(OpenApiSDK.getMusicTherapyApi().getCurrentTherapyInfo())
    val therapyItem = mutableStateOf(OpenApiSDK.getMusicTherapyApi().getCurrentTherapyItem())
    val mvVid = mutableStateOf(SimpleMMKV.getCommonMMKV().getString(MV_VID, "") ?: "")
    val volumeArray = mutableStateOf(OpenApiSDK.getMusicTherapyApi().getCurrentPlayMusicTherapyParam().volumeArray)
    val enterInMusicTherapy = mutableStateOf(false)
    val playDuration = mutableStateOf(OpenApiSDK.getMusicTherapyApi().getCurrentPlayMusicTherapyParam().playDuration)
    var showDialog = mutableStateOf(false)
    private val provider = OpenApiSDK.getProviderByClass(EdgeMvProvider::class.java)
    private val mediaNetWork = provider?.getMediaNetWorkImpl()
    val mPlayer = provider?.getMediaPlayer()
    var isPrepared = AtomicBoolean(false)
    private val eventCallback = object : IPlayEventCallback {
        override fun onEvent(state: PlayerState, data: Bundle?) {
            QLog.i(TAG, "eventCallback state = $state")
            if (!isPrepared.get()) {
                isPrepared.set(state == PlayerState.PREPARED || state ==PlayerState.STARTED)
            }
            if (state == PlayerState.PREPARED) {
                if (PlayStateProxyHelper.isPlaying(OpenApiSDK.getMusicTherapyApi().getPlayState())) {
                    mPlayer?.play()
                }
            }
        }

        override fun onVideoSizeChanged(width: Int?, height: Int?) {
        }

        override fun onError(error: PlayError, data: Any?) {
        }

    }

    override fun onCleared() {
        super.onCleared()
        QLog.i(TAG, "onCleared")
        mPlayer?.destroy()
    }

    init {
        QLog.i(TAG, "init")
        viewModelScope.launch {
            delay(2000)
            OpenApiSDK.getMusicTherapyApi().canTryMusicTherapy {
                showDialog.value = it
            }
        }
        mPlayer?.apply {
            setEventCallback(eventCallback)
            setExceptMvQuality(MediaQuality.SQ)
            setLoop(true)
        }
        provider?.setCacheSize(500)
    }

    fun fetchMusicTherapyConfig() {
        if (pageStatus.value != LOADING) {
            pageStatus.value = LOADING
        }
        OpenApiSDK.getMusicTherapyApi().fetchMusicTherapyConfig {
            pageStatus.value = if (it.therapyInfoList.isNotEmpty()) {
                musicTherapyListData.value = it
                therapyInfo.value = if (therapyInfo.value != null) {
                    it.therapyInfoList.find { therapyInfo.value?.classId == it.classId } ?: it.therapyInfoList.firstOrNull()
                } else {
                    it.therapyInfoList.firstOrNull()
                }

                therapyItem.value = if (therapyItem.value != null) {
                    therapyInfo.value?.therapyItemList?.find { therapyItem.value?.id == it.id } ?: therapyInfo.value?.therapyItemList?.firstOrNull()
                } else {
                    therapyInfo.value?.therapyItemList?.firstOrNull()
                }
                updateVolumeArray()
                SUCCESS
            } else {
                ERROR
            }
            QLog.i(TAG, "fetchMusicTherapyConfig pageStatus = ${pageStatus.value}, threadName = ${Thread.currentThread().name}")
        }
    }

    fun playMedia(mvVid: String = this.mvVid.value, surface: Surface? = null) {
        QLog.i(TAG, "playMedia mvVid = $mvVid, surface = $surface")
        this.mvVid.value = mvVid
        SimpleMMKV.getCommonMMKV().putString(MV_VID, mvVid)
        if (mvVid.isNotEmpty()) {
            mediaNetWork?.getMediaInfo(listOf(mvVid)) {
                if (it.isSuccess()) {
                    it.data?.firstOrNull()?.let { item ->
                        startPlayVideo(item, surface)
                    }
                }
            }
        } else {
            QLog.e(TAG, "playMedia error mvVid is empty")
        }
    }

    private fun startPlayVideo(mediaResDetail: MediaResDetail?, surface: Surface?) {
        AppScope.launchUI {
            mPlayer?.setMediaRes(mediaResDetail)
            mPlayer?.setSurface(surface)
        }
    }

    fun updateVolumeArray() {
        volumeArray.value = therapyItem.value?.let { therapyItem ->
            OpenApiSDK.getMusicTherapyApi().getVolumeArray(therapyItem)
        } ?: FloatArray(0)
    }
}