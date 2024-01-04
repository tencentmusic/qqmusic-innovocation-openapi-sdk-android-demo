package com.tencent.qqmusic.qplayer.ui.activity.lyric

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.tencent.qqmusic.innovation.common.logging.MLog
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.view.lyric.LyricStateInterface
import com.tencent.qqmusic.openapisdk.core.view.lyric.MultiLineLyricView
import com.tencent.qqmusic.openapisdk.core.view.lyric.OnLyricContentClickListener
import com.tencent.qqmusic.openapisdk.core.view.lyric.OnLyricScrollChangeListener
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.baselib.util.QLog

/**
 * Created by tannyli on 2023/11/30.
 * Copyright (c) 2023 TME. All rights reserved.
 */
class BaseLyricFragment(val layoutId: Int = -1): Fragment(), LyricStateInterface {
    companion object {
        private const val TAG = "BaseLyricFragment"
    }

    private var lyricView: MultiLineLyricView? = null
    private var loadStatusView: TextView? = null

    private var rootView: View? = null
    private var seekPlayButton: Button? = null

    private var lyricMode = 0   // 0：仅歌词 1：歌词+翻译 2：歌词+音译
        set(value) {
            field = value
            when (field) {
                0-> {
                    rootView!!.findViewById<Button>(R.id.switch_lyric_btn).text = "仅歌词"
                    lyricView?.showLyricType(false, false)
                }
                1-> {
                    rootView!!.findViewById<Button>(R.id.switch_lyric_btn).text = "翻译歌词"
                    lyricView?.showLyricType(true, false)
                }
                2-> {
                    rootView!!.findViewById<Button>(R.id.switch_lyric_btn).text = "音译歌词"
                    lyricView?.showLyricType(false, true)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(getLayoutResInt(), container, false)
    }

    protected fun getLayoutResInt(): Int {
        if (layoutId != -1) {
            return layoutId
        }
        return R.layout.fragment_lyric_new
    }

    private val hideHighlightLyricInOffset = Runnable {
        seekPlayButton?.isVisible = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rootView = view
        lyricView = view.findViewById<MultiLineLyricView>(R.id.scroll_lyric)
        loadStatusView = view.findViewById(R.id.loading_lyric_status)
        lyricView?.addLyricStateInterface(this)
        seekPlayButton = rootView!!.findViewById<Button>(R.id.seek_play)

        lyricView?.setOnLyricScrollChangeListener(object : OnLyricScrollChangeListener {
            override fun onScrollChange(time: String) {
                MLog.i(TAG, "onScrollChange $time")
                seekPlayButton?.text = "播放:$time"
                seekPlayButton?.isVisible = true
                seekPlayButton?.handler?.removeCallbacksAndMessages(null)
                seekPlayButton?.postDelayed(hideHighlightLyricInOffset, 3000)
            }
        })
        lyricView?.setOnLyricContentClickListener(object : OnLyricContentClickListener {
            override fun onClick(startTime: Long) {
                MLog.i(TAG, "setOnLyricContentClickListener $startTime")
                onTapSeek(startTime)
            }
        })

        seekPlayButton?.setOnClickListener {
            lyricView?.getLastScrollLyricTime()?.let { it1 -> onTapSeek(it1) }
        }

        view.findViewById<Button>(R.id.switch_lyric_btn).setOnClickListener {
            lyricMode = (lyricMode + 1) % 3
        }
        lyricMode = 0

        view.findViewById<Button>(R.id.btn_old_style).setOnClickListener {
            this.startActivity(Intent(activity, LyricActivity::class.java))
        }
    }

    private fun onTapSeek(mStartTime: Long) {
        hideHighlightLyricInOffset.run()
        OpenApiSDK.getPlayerApi().seekToPlay(mStartTime)
    }
    override fun onLoadLyric(
        isSuccess: Boolean,
        hasLyric: Boolean,
        hasTransLyric: Boolean,
        hasRomaLyric: Boolean
    ) {
        rootView?.post {
            // 具体看厂商如何使用，这里只print
            if (!isSuccess) {
                loadStatusView?.visibility = View.VISIBLE
                loadStatusView?.text = "歌词加载失败"
            } else if (!hasLyric) {
                loadStatusView?.visibility = View.VISIBLE
                // 如果当前歌曲是长音频歌曲，这里可以设置为"暂无字幕"
                loadStatusView?.text = "暂无歌词"
            } else {
                loadStatusView?.visibility = View.GONE
                lyricView?.visibility = View.VISIBLE
            }
            QLog.i(TAG, "hasLyric: $hasLyric, hasTransLyric: $hasTransLyric, hasRomaLyric: $hasRomaLyric")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lyricView?.release()
    }

}