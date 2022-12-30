package com.tencent.qqmusic.qplayer.ui.activity.lyric

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.tencent.qqmusic.openapisdk.core.view.lyric.LyricStateInterface
import com.tencent.qqmusic.openapisdk.core.view.lyric.QMLyricView
import com.tencent.qqmusic.openapisdk.core.view.lyric.TwoLineLyricLayout
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.baselib.util.QLog

class LyricActivity : ComponentActivity(), LyricStateInterface {
    companion object {
        private const val TAG = "LyricActivity"
    }

    private var lyricView: QMLyricView? = null
    private var loadStatusView: TextView? = null

    private var lyricMode = 0   // 0：仅歌词 1：歌词+翻译 2：歌词+音译
    set(value) {
        field = value
        when (field) {
            0-> {
                findViewById<Button>(R.id.switch_lyric_btn).text = "仅歌词"
                findViewById<QMLyricView>(R.id.scroll_lyric).showLyricType(false, false)
            }
            1-> {
                findViewById<Button>(R.id.switch_lyric_btn).text = "翻译歌词"
                findViewById<QMLyricView>(R.id.scroll_lyric).showTransLyric()
            }
            2-> {
                findViewById<Button>(R.id.switch_lyric_btn).text = "音译歌词"
                findViewById<QMLyricView>(R.id.scroll_lyric).showRomaLyric()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_lyric)
        lyricView = findViewById<QMLyricView>(R.id.scroll_lyric)
        loadStatusView = findViewById(R.id.loading_lyric_status)
        findViewById<TwoLineLyricLayout>(R.id.twoline_lyric_layout).showTransLyric = false
        lyricView?.addLyricStateInterface(this)
        findViewById<Button>(R.id.switch_lyric_btn).setOnClickListener {
            lyricMode = (lyricMode + 1) % 3
        }
        lyricMode = 0
    }

    override fun onLoadLyric(
        isSuccess: Boolean,
        hasLyric: Boolean,
        hasTransLyric: Boolean,
        hasRomaLyric: Boolean
    ) {
        // 具体看厂商如何使用，这里只print
        if (!isSuccess) {
            loadStatusView?.visibility = View.VISIBLE
            loadStatusView?.text = "歌词加载失败"
            lyricView?.visibility = View.GONE
        } else {
            loadStatusView?.visibility = View.GONE
            lyricView?.visibility = View.VISIBLE
        }
        QLog.i(TAG, "hasLyric: $hasLyric, hasTransLyric: $hasTransLyric, hasRomaLyric: $hasRomaLyric")
    }
}
