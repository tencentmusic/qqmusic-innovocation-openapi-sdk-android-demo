package com.tencent.qqmusic.qplayer.utils

import android.content.Context
import android.media.AudioManager
import android.util.Log

class AudioFocusChangeHelper(var content: Context?) : AudioManager.OnAudioFocusChangeListener {

    private var mAudioManager: AudioManager? = null
    private var reason = AudioManager.AUDIOFOCUS_LOSS

    var audioFocusChangeListener: AudioFocusChangeListener? = null

    private var hasRequest = false

    var enableAudioFocus = false

    companion object {
        const val TAG = "AudioFocusChangeHelper"
    }

    fun requestFocus(): Boolean {
        Log.i(TAG, "[requestFocus]，hasRequest=$hasRequest,enableAudioFocus=$enableAudioFocus")
        try {
            if (!hasRequest) {
                if (mAudioManager == null) {
                    mAudioManager = content?.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
                }
                hasRequest = AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager?.requestAudioFocus(
                        this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
            }
            enableAudioFocus = true
            return hasRequest
        } catch (e: Exception) {
            Log.e(TAG, "requestFocus", e)
        }
        return false
    }

    fun abandonFocus(): Boolean {
        Log.i(TAG, "[abandonFocus]，hasRequest=$hasRequest,enableAudioFocus=$enableAudioFocus")
        try {
            hasRequest = false
            enableAudioFocus = false
            return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager?.abandonAudioFocus(this)
        } catch (e: Exception) {
            Log.e(TAG, "abandonFocus", e)
        }
        return false
    }

    override fun onAudioFocusChange(focusChange: Int) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            hasRequest = false
        }
        Log.i(TAG, "[onAudioFocusChange]:$focusChange,hasRequest=$hasRequest,enableAudioFocus:${enableAudioFocus}")
        if (!enableAudioFocus) {
            return
        }
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // 长时间丢失焦点除非重新主动获取
                reason = AudioManager.AUDIOFOCUS_LOSS
                audioFocusChangeListener?.audioFocusLoss()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // 暂时丢失焦点，可重新获得焦点。闹铃是这个监听
                reason = AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                audioFocusChangeListener?.audioFocusLossTransient()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // 短暂丢失焦点，压低后台音量
                reason = AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
                audioFocusChangeListener?.audioFocusLossTransientCanDuck()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // 恢复播放
                audioFocusChangeListener?.audioFocusGain(reason)
                reason = AudioManager.AUDIOFOCUS_GAIN
            }
        }
    }

    interface AudioFocusChangeListener {
        fun audioFocusLoss()
        fun audioFocusLossTransient()
        fun audioFocusLossTransientCanDuck()
        fun audioFocusGain(reason: Int)
    }

}