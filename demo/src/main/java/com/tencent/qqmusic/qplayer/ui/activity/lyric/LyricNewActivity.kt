package com.tencent.qqmusic.qplayer.ui.activity.lyric

import android.graphics.Typeface
import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.tencent.qqmusic.qplayer.R

/**
 * 使用MultiLineLyricView歌词组件。推荐使用
 */
class LyricNewActivity : FragmentActivity() {
    companion object {
        private const val TAG = "LyricActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_lyric_new)

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
    }

}
