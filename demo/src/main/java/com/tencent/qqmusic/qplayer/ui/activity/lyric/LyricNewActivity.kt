package com.tencent.qqmusic.qplayer.ui.activity.lyric

import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.OnVocalAccompanyStatusChangeListener
import com.tencent.qqmusic.openapisdk.core.player.VocalAccompanyErrorStatus
import com.tencent.qqmusic.qplayer.R

/**
 * 使用MultiLineLyricView歌词组件。推荐使用
 */
class LyricNewActivity : FragmentActivity(), OnVocalAccompanyStatusChangeListener {
    companion object {
        private const val TAG = "LyricActivity"
    }
    private val vocalAccompanyButton: Button by lazy { findViewById(R.id.vocalAccompany) }
    private val vocalAccompanySeekbar: AppCompatSeekBar by lazy { findViewById(R.id.seekbar) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_lyric_new)
        OpenApiSDK.getVocalAccompanyApi().addVocalAccompanyStatusChangeListener(this)
        val viewPage = findViewById<ViewPager2>(R.id.view_pager)
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        val adapter = LyricFragmentAdapter(this)
        viewPage.adapter = adapter
        viewPage.offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
        viewPage.currentItem = 2

        TabLayoutMediator(tabLayout, viewPage) { tab, pos ->
            tab.text = "样式${pos + 1}"
        }.attach()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.customView?.findViewById<TextView>(android.R.id.text1)?.apply {
                    setTypeface(Typeface.DEFAULT_BOLD)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.customView?.findViewById<TextView>(android.R.id.text1)?.apply {
                    setTypeface(Typeface.DEFAULT)
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

        })
        vocalAccompanyButton.setOnClickListener {
            val isVocalAccompanyOpened = OpenApiSDK.getVocalAccompanyApi().isVocalAccompanyOpened()
            val result = if (isVocalAccompanyOpened) {
                OpenApiSDK.getVocalAccompanyApi().disableVocalAccompany()
            } else {
                OpenApiSDK.getVocalAccompanyApi().enableVocalAccompany()
            }
            if (result != VocalAccompanyErrorStatus.SUCCESS) {
                Toast.makeText(this, result.msg, Toast.LENGTH_SHORT).show()
            } else {
                onVocalAccompanyStatusChange(OpenApiSDK.getVocalAccompanyApi().currentVocalRadio().value, !isVocalAccompanyOpened)
            }
        }
        var closeVocalPercent = OpenApiSDK.getVocalAccompanyApi().currentVocalRadio()
        vocalAccompanySeekbar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekbar: SeekBar?, progress: Int, fromUser: Boolean) {
                closeVocalPercent = OpenApiSDK.getVocalAccompanyApi().convertToCloseVocalRadio(progress)
            }

            override fun onStartTrackingTouch(seekbar: SeekBar?) {
                // do nothing
            }

            override fun onStopTrackingTouch(seekbar: SeekBar?) {
                OpenApiSDK.getVocalAccompanyApi().adjustVocalRadio(closeVocalPercent)
                Toast.makeText(this@LyricNewActivity, "切换比例: ${closeVocalPercent.value}", Toast.LENGTH_SHORT).show()
            }
        })
        onVocalAccompanyStatusChange(OpenApiSDK.getVocalAccompanyApi().currentVocalRadio().value, OpenApiSDK.getVocalAccompanyApi().isCurrentSongPlayWithVocalAccompany())
    }

    override fun onVocalAccompanyStatusChange(vocalScale: Int, enable: Boolean) {
        runOnUiThread {
            if (enable) {
                vocalAccompanyButton.text = "关闭伴唱"
            } else {
                vocalAccompanyButton.text = "开启伴唱"
            }
            vocalAccompanySeekbar.isVisible = enable
            vocalAccompanySeekbar.progress = vocalScale
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        OpenApiSDK.getVocalAccompanyApi().removeVocalAccompanyStatusChangeListener(this)
    }

}
