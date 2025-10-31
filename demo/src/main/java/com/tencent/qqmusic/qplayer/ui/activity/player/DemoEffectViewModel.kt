package com.tencent.qqmusic.qplayer.ui.activity.player

import android.graphics.drawable.Drawable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.map
import com.tencent.qqmusic.innovation.common.logging.MLog
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.playerui.data.MHSongInfo
import com.tencent.qqmusic.openapisdk.playerui.data.EffectMagicColor
import com.tencent.qqmusic.openapisdk.playerui.data.Lyric
import com.tencent.qqmusic.openapisdk.playerui.data.EffectSongInfo
import com.tencent.qqmusic.openapisdk.playerui.viewmode.MagicColor
import com.tencent.qqmusic.openapisdk.playerui.viewmode.PlayerViewModel
import com.tencent.qqmusic.openapisdk.playerui.viewmodel.IEffectPlayerInfoViewModel

/**
 * Created by silverfu on 2025/6/11.
 */
class DemoEffectViewModel(val viewModel: PlayerViewModel) : IEffectPlayerInfoViewModel {

    override val playStateLiveData: LiveData<Int>
        get() = viewModel.playStateLiveData
    override val playSpeedLiveData: LiveData<Pair<Float, Long>>
        get() = viewModel.playSpeedLiveData
    override val playSongDrawableLiveData: LiveData<Drawable?>
        get() = viewModel.playSongDrawableLiveData

    private val _Effect_magicColorLiveData = MutableLiveData<EffectMagicColor>(EffectMagicColor())
    private val _playMHSongLiveData = MutableLiveData<MHSongInfo?>()
    private val _playLyricLiveData = MutableLiveData<Lyric>()
    private val _playSeekLiveData = MutableLiveData<Long>()

    override val playSeekLiveData: LiveData<Long>
        get() = _playSeekLiveData

    override val playLyricLiveData: LiveData<Lyric?>
        get() = _playLyricLiveData

    override val effectMagicColorLiveData: LiveData<EffectMagicColor>
        get() = _Effect_magicColorLiveData
    override val playSongLiveData: LiveData<EffectSongInfo?>
        get() = viewModel.playSongLiveData.map { songInfo ->
            songInfo ?: return@map null
            EffectSongInfo(
                songInfo.songId,
                songInfo.songName,
                songInfo.singerName ?: "",
                songInfo.bpm?.toLong() ?: 0L
            )
        }
    override val playMHSongLiveData: LiveData<MHSongInfo?>
        get() = _playMHSongLiveData

    private val magiColorObserver = Observer<MagicColor> { t ->
        _Effect_magicColorLiveData.postValue(
            EffectMagicColor(
                mapOf(
                    "firstColor" to t.backgroundColor,
                    "secondColor" to t.backgroundColor2
                )
            )
        )
        MLog.i(PlayerViewModel.TAG, "new magicColor1 ${_Effect_magicColorLiveData.value}")
    }

    override fun bind() {
        super.bind()
        viewModel.magicColorLiveData.observeForever(magiColorObserver)
    }

    override fun isPlaying(): Boolean {
        return OpenApiSDK.getPlayerApi().isPlaying()
    }

    override fun playTime(): Long {
        return OpenApiSDK.getPlayerApi().getCurrentPlayTime()
    }

    override fun doDecryptionLyric(p0: String?): String? {
        return ""
    }

    override fun unbind() {
        super.unbind()
        viewModel.magicColorLiveData.removeObserver(magiColorObserver)
    }
}