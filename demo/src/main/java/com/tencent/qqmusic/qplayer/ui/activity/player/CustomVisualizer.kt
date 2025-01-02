package com.tencent.qqmusic.qplayer.ui.activity.player

import com.tencent.qqmusic.innovation.common.logging.MLog
import com.tencent.qqmusic.openapisdk.core.player.visualizer.AudioFeature
import com.tencent.qqmusic.openapisdk.core.player.visualizer.VisualizerStrategy

/**
 * Created by silverfu on 2024/12/4.
 */
class CustomVisualizer : VisualizerStrategy() {
    override fun onUpdate(audioFeature: AudioFeature?) {
        MLog.i("CustomVisualizer", "audioFeature:$audioFeature")
    }
}