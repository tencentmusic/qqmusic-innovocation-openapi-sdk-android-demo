package com.tencent.qqmusic.qplayer.ui.activity.player

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.tencent.qqmusic.innovation.common.logging.MLog
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.openapi.OpenApiCallback
import com.tencent.qqmusic.openapisdk.core.openapi.OpenApiResponse
import com.tencent.qqmusic.openapisdk.core.player.IMediaEventListener
import com.tencent.qqmusic.openapisdk.core.player.IProgressChangeListener
import com.tencent.qqmusic.openapisdk.core.player.PlayerEvent
import com.tencent.qqmusic.openapisdk.model.LyricImage
import com.tencent.qqmusic.qplayer.App
import com.tencent.qqmusic.qplayer.baselib.util.AppScope

/**
 * Created by silverfu on 2024/9/9.
 */
object LyricImageManager {
    const val TAG = "LyricImageManager"
    private const val REQ_LYRIC_RESOLUTION_TYPE = "2"
    private var lyricImages: List<LyricImage>? = null
    private var mCurrentLyricImageUrl: String? = null
    private var hasLoading = false

    var lyricImageLiveData  = MutableLiveData<Drawable>()

    private val event = object : IMediaEventListener {
        override fun onEvent(event: String, arg: Bundle) {
            when (event) {
                PlayerEvent.Event.API_EVENT_PLAY_SONG_CHANGED -> {

                    //控制请求时机
                    OpenApiSDK.getOpenApi().fetchLyricImages(
                        PlayerObserver.currentSong?.songId ?: 0,
                        resolutionType = REQ_LYRIC_RESOLUTION_TYPE,
                        callback = lyricImageResponseCallback
                    )
                }
            }
        }
    }

    init {
        OpenApiSDK.getPlayerApi().registerEventListener(event)
        OpenApiSDK.getPlayerApi().registerProgressChangedListener(object : IProgressChangeListener {
            override fun progressChanged(curPlayTime: Long, totalTime: Long, bufferLength: Long, totalLength: Long) {
                loadLyricImage(curPlayTime)
            }
        })
    }

    fun loadLyricImage(curPlayTime: Long) {
        val filteredImages = lyricImages?.filter { curPlayTime >= it.startTimestamp }
        if (!filteredImages.isNullOrEmpty()) {
            val latestImage = filteredImages.maxByOrNull { it.startTimestamp }
            if (latestImage?.url != mCurrentLyricImageUrl) {
                mCurrentLyricImageUrl = latestImage?.url
                MLog.i(TAG, "start loadLyricImage $mCurrentLyricImageUrl")
                AppScope.launchUI {
                    if (hasLoading) {
                        return@launchUI
                    }
                    hasLoading = true
                    Glide.with(App.context).asBitmap().load(mCurrentLyricImageUrl).into(object : SimpleTarget<Bitmap?>() {

                        override fun onResourceReady(bitmap: Bitmap, p1: Transition<in Bitmap?>?) {
                            val bitmapDrawable = BitmapDrawable(bitmap)
                            lyricImageLiveData.postValue(bitmapDrawable)
                            MLog.i(TAG,"onResourceReady $bitmapDrawable")
                            hasLoading = false
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            super.onLoadFailed(errorDrawable)
                            MLog.i(TAG,"onLoadFailed")
                            hasLoading = false
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            super.onLoadCleared(placeholder)
                            hasLoading = false
                        }
                    })
                }
            }
        }
    }

    private val lyricImageResponseCallback: OpenApiCallback<OpenApiResponse<Map<String, List<LyricImage>>?>> = {
        reset()
        val imageList = it.data?.get(REQ_LYRIC_RESOLUTION_TYPE)
        MLog.i(TAG,"lyricImageResponseCallback $imageList")
        lyricImages = imageList
    }

    private fun reset() {
        lyricImageLiveData.postValue(null)
        hasLoading = false
        mCurrentLyricImageUrl = null
    }

}