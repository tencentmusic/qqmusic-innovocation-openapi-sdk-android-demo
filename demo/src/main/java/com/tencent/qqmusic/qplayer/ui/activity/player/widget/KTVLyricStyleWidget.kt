package com.tencent.qqmusic.qplayer.ui.activity.player.widget

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.IMediaEventListener
import com.tencent.qqmusic.openapisdk.core.player.PlayerEvent
import com.tencent.qqmusic.openapisdk.hologram.EdgeMvProvider
import com.tencent.qqmusic.openapisdk.playerui.view.ViewWidget
import com.tencent.qqmusic.openapisdk.playerui.view.background.PlayerKTVBackgroundVideoWidget
import com.tencent.qqmusic.openapisdk.playerui.view.lyric.TwoLineLyricViewWidget
import com.tencent.qqmusic.openapisdk.playerui.viewmode.PlayerViewModel
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerObserver

class KTVLyricStyleWidget(
    val lyricViewModel: PlayerViewModel,
    val rootView: ViewGroup
) : ViewWidget() {
    private val TAG = "KTVStyleWidget"

    private var videoContainer: FrameLayout? = null
    private var showContainer: ConstraintLayout? = null
    private val mediaNetWorkImpl = OpenApiSDK.getProviderByClass(EdgeMvProvider::class.java)?.getMediaNetWorkImpl()
    var videoWidget: PlayerKTVBackgroundVideoWidget? = null

    var lastPlayMvSongId = 0L

    private fun registerMvObserver() {
        lyricViewModel.playSongLiveData.observe(this) { song ->
            song ?: return@observe
            mediaNetWorkImpl?.getKTVMVInfo(listOf(song.songId)) { mvInfoRep ->
                if (mvInfoRep.isSuccess()) {
                    val mvInfo =
                        mvInfoRep.data?.firstOrNull { it.songId == song.songId } ?: return@getKTVMVInfo
                    mvInfo.mvPlayUrl?.let { path ->
                        videoWidget?.updateVideoPath(path)
                        lastPlayMvSongId = song.songId
                        val videoWidth = mvInfo.maxWidth?.takeIf { it > 0 } ?: return@getKTVMVInfo
                        val videoHeight = mvInfo.maxHeight?.takeIf { it > 0 } ?: return@getKTVMVInfo
                        Log.e(TAG, "resetVideoSize: $videoWidth, $videoHeight")
                        showContainer?.post {
                            showContainer?.layoutParams = showContainer?.layoutParams?.apply {
                                val widget = videoWidget ?: return@apply
                                val (width, height) = widget.getRecommendVideoShowWH(
                                    videoWidth,
                                    videoHeight
                                )
                                Log.d(TAG, "getRecommendVideoShowWH: $width, $height")
                                this.width = width
                                this.height = height
                            }
                        }
                    } ?: Log.e(TAG, "mvPlayUrl is null")
                } else {
                    Log.e(TAG, "getKTVMVInfo failed: ${mvInfoRep.errorMsg}")
                }
            }
        }

    }

    override fun onBind() {
        showContainer = rootView.findViewById(R.id.ktv_show_container)
        bindViewWidget()
        registerMvObserver()
    }


    fun bindViewWidget() {
        videoContainer = rootView.findViewById<FrameLayout>(R.id.mv_surface_container)
        videoContainer?.let {
            videoWidget = PlayerKTVBackgroundVideoWidget(lyricViewModel,it).also { widget ->
                bindWidget(widget)
            }
        }
        bindWidget(TwoLineLyricViewWidget(lyricViewModel, rootView.findViewById(R.id.twoline_lyric_layout)))
    }

    override fun onUnbind() {
        super.onUnbind()
    }
}