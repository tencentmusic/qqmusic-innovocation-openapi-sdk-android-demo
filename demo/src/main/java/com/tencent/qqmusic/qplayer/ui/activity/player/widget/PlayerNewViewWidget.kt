package com.tencent.qqmusic.qplayer.ui.activity.player.widget

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.lifecycle.Observer
import com.tencent.qqmusic.innovation.common.logging.MLog
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.view.lyric.MultiLineLyricView
import com.tencent.qqmusic.openapisdk.playerui.utils.Utils
import com.tencent.qqmusic.openapisdk.playerui.view.PlayerBackgroundViewWidget
import com.tencent.qqmusic.openapisdk.playerui.view.PlayerSpectrumViewWidget
import com.tencent.qqmusic.openapisdk.playerui.view.PlayerSpectrumViewWidget.Companion.STYLE_SPECTRUM_BAR
import com.tencent.qqmusic.openapisdk.playerui.view.PlayerVinylStyleViewWidget
import com.tencent.qqmusic.openapisdk.playerui.view.ViewWidget
import com.tencent.qqmusic.openapisdk.playerui.viewmode.PlayerViewModel
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.player.CustomVisualizer


class PlayerNewViewWidget(private val viewModel: PlayerViewModel, private val container: ViewGroup) : ViewWidget() {

    private val playAPI = OpenApiSDK.getPlayerApi()

    private val visualizer = CustomVisualizer()

    override fun onBind() {

        val playerContainerView = container.findViewById<ViewGroup>(R.id.player_song_middle_area)
        bindWidget(PlayerVinylStyleViewWidget(viewModel, playerContainerView))

        val backgroundView = container.findViewById<ViewGroup>(R.id.player_background)
        bindWidget(PlayerBackgroundViewWidget(viewModel, backgroundView))
        bindWidget(PlayerSpectrumViewWidget(viewModel, STYLE_SPECTRUM_BAR, container.findViewById(R.id.player_spectrum_bg)))

        initPlayerState()

    }

    private fun initPlayerState() {
        val playButton = container.findViewById<ImageView>(R.id.player_song_play_btn)
        val playButtonPre = container.findViewById<ImageView>(R.id.player_song_play_pre)
        val playButtonNext = container.findViewById<ImageView>(R.id.player_song_play_next)
        viewModel.playStateLiveData.observe(this, Observer {
            playButton?.setImageResource(if (playAPI.isPlaying()) R.drawable.ic_state_playing else R.drawable.ic_state_paused)
        })


        playButton?.setOnClickListener {
            if (playAPI.isPlaying()) {
                playAPI.pause()
            } else {
                playAPI.play()
            }
        }

        playButtonPre.setOnClickListener {
            playAPI.prev()
        }
        playButtonNext.setOnClickListener {
            playAPI.next()
        }
        val multiLineLyricView = container.findViewById<MultiLineLyricView>(R.id.scroll_lyric)
        val seekBar: SeekBar? = container.findViewById<SeekBar>(R.id.seek_bar)
        val songNameView = container.findViewById<TextView>(R.id.song_name)
        viewModel.magicColorLiveData.observe(this, Observer {
            MLog.i("PlayerNewViewWidget", "magic color $it")
            val porterDuffColorFilter = PorterDuffColorFilter(it.highlightColor, PorterDuff.Mode.SRC_ATOP)
            playButton.setColorFilter(it.highlightColor)
            playButtonPre.colorFilter = porterDuffColorFilter
            playButtonNext.colorFilter = porterDuffColorFilter
            multiLineLyricView.setHColor(it.highlightColor)
            seekBar?.progressDrawable?.colorFilter = porterDuffColorFilter
            seekBar?.thumb?.colorFilter = porterDuffColorFilter
            songNameView.setTextColor(it.highlightColor)
            multiLineLyricView.setColor((it.foregroundColor and 16777215 or (128 shl 24)))
        })

        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    playAPI.seekToPlay(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
        viewModel.progressLiveData.observe(this, Observer {
            seekBar?.progress = it.curPlayTime.toInt()
            seekBar?.max = it.totalTime.toInt()
        })

        viewModel.playSongLiveData.observe(this, Observer {
            songNameView.text = it?.songName
        })

        /**
         * 根据需要，改变背景底色
         */
        viewModel.playerStyleLiveData.observe(this, Observer {
            container.setBackgroundColor(Utils.parseColor(it.styleConfig?.vinyl?.color?.magicColor, 0))
        })

    }

    override fun onUnbind() {
        super.onUnbind()
    }

}