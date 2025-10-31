package com.tencent.qqmusic.qplayer.ui.activity.lyric

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.OnVocalAccompanyStatusChangeListener
import com.tencent.qqmusic.openapisdk.core.player.VocalAccompanyErrorStatus
import com.tencent.qqmusic.openapisdk.core.player.VocalPercent
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.utils.UiUtils

/**
 * 使用MultiLineLyricView歌词组件。推荐使用
 */
class LyricNewActivity : FragmentActivity(), OnVocalAccompanyStatusChangeListener {
    companion object {
        private const val TAG = "LyricActivity"
    }

    private val vocalAccompanyButton: Button by lazy { findViewById(R.id.vocalAccompany) }
    private val vocalAccompanySeekbar: AppCompatSeekBar by lazy { findViewById(R.id.seekbar) }
    private val backButton : ImageButton by lazy { findViewById(R.id.btn_back) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_lyric_new)
        // 绑定Toolbar并设置返回按钮
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
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
                UiUtils.showToast(result.msg)
            } else {
                onVocalAccompanyStatusChange(
                    OpenApiSDK.getVocalAccompanyApi().currentVocalRadio().value,
                    !isVocalAccompanyOpened
                )
            }
        }
        queryTryVocalAccompany()
        // 创建进度显示TextView
        val progressTextView = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            visibility = View.GONE
            setTypeface(Typeface.DEFAULT_BOLD)
            setBackgroundColor(Color.parseColor("#80000000"))
            setPadding(8, 4, 8, 4)
        }
        (vocalAccompanySeekbar.parent as? ViewGroup)?.addView(progressTextView)

        var closeVocalPercent = OpenApiSDK.getVocalAccompanyApi().currentVocalRadio()
        var realVocalPercent = OpenApiSDK.getVocalAccompanyApi().currentVocalValue()
        vocalAccompanySeekbar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(seekbar: SeekBar?, progress: Int, fromUser: Boolean) {
                closeVocalPercent =
                    OpenApiSDK.getVocalAccompanyApi().convertToCloseVocalRadio(progress)

                // 更新进度显示
                realVocalPercent = progress
                progressTextView.text =
                    when(progress){
                        VocalPercent.min.value -> "纯人声"
                        VocalPercent.max.value -> "纯伴奏"
                        VocalPercent.eighty.value -> "原声"
                        in 1..79 -> "${(progress / 80f * 100).toInt()}%伴奏"
                        else -> "${100 - ((progress - 80) / 80f * 100).toInt()}%人声"
                    }
                progressTextView.visibility = View.VISIBLE
                // 计算并更新文本位置（跟随滑块）
                seekbar?.let { sb ->
                    val thumbOffset = sb.thumbOffset
                    val pos = sb.x + (sb.width * progress / sb.max.toFloat()) - thumbOffset
                    progressTextView.x = pos - progressTextView.width / 2
                    progressTextView.y = sb.y - progressTextView.height - 16
                }
            }

            override fun onStartTrackingTouch(seekbar: SeekBar?) {
                progressTextView.visibility = View.VISIBLE
            }

            override fun onStopTrackingTouch(seekbar: SeekBar?) {
                val result = OpenApiSDK.getVocalAccompanyApi().adjustVocalRadio(realVocalPercent)
                UiUtils.showToast("code=${result.code},msg=${result.msg}", isLong = true)
                if (!result.isSuccess()) {
                    seekbar?.progress = (OpenApiSDK.getVocalAccompanyApi().currentVocalValue())
                }
                // 延迟隐藏进度文本
                progressTextView.postDelayed({
                    progressTextView.visibility = View.GONE
                }, 1000)
            }
        })
        onVocalAccompanyStatusChange(
            OpenApiSDK.getVocalAccompanyApi().currentVocalRadio().value,
            OpenApiSDK.getVocalAccompanyApi().isCurrentSongPlayWithVocalAccompany()
        )
        OpenApiSDK.getVocalAccompanyApi().canTryVocalAccompany { canTry ->
            if (canTry) {
                runOnUiThread {
                    AlertDialog.Builder(this).setTitle("提示")
                        .setMessage("恭喜您，可以领取伴唱试用权益")
                        .setNegativeButton(
                            "取消"
                        ) { dialog, _ ->
                            dialog.dismiss()
                        }.setPositiveButton("确认") { dialog, _ ->
                            dialog.dismiss()
                            OpenApiSDK.getVocalAccompanyApi()
                                .fetchVocalAccompanyTrialBenefits { tryResult ->
                                    if (tryResult == VocalAccompanyErrorStatus.SUCCESS) {
                                        UiUtils.showToast("领取伴唱试用权益成功")
                                    } else {
                                        UiUtils.showToast("领取伴唱试用权益失败：${tryResult.msg}")
                                    }
                                }
                        }.show()
                }
            }
        }
    }

    private fun queryTryVocalAccompany() {
        if (OpenApiSDK.getPlayerApi().getCurrentSongInfo()?.canTryVocalAccompany() == true) {
            UiUtils.showToast("当前歌曲拥有伴唱试用权益")
        } else {
            UiUtils.showToast("当前歌曲没有伴唱试用权益")
        }
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
