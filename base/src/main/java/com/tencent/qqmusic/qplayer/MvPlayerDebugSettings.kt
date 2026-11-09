package com.tencent.qqmusic.qplayer

import com.tencent.qqmusic.MediaPlayerConfig
import com.tencent.qqmusic.edgemv.IEdgeMediaPlayer
import com.tencent.qqmusic.openapisdk.hologram.EdgeMvProvider
import com.tencent.qqmusiccommon.SimpleMMKV

object MvPlayerDebugSettings {
    private const val KEY_AUDIO_FIRST_MODE_ENABLED = "demo_mv_audio_first_mode_enabled"

    var audioFirstModeEnabled: Boolean
        get() = SimpleMMKV.getCommonMMKV().getBoolean(KEY_AUDIO_FIRST_MODE_ENABLED, false)
        set(value) {
            SimpleMMKV.getCommonMMKV().putBoolean(KEY_AUDIO_FIRST_MODE_ENABLED, value)
        }

    fun createMediaPlayer(provider: EdgeMvProvider): IEdgeMediaPlayer? {
        // ThumbPlayer 在创建时读取配置；关闭开关时也显式设置 false，支持对照测试。
        val config = MediaPlayerConfig().apply {
            openAudioFirstMode = audioFirstModeEnabled
        }
        provider.setPlayerConfig(config)
        return provider.getMediaPlayer()
    }
}
