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
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.ChorusModeOperationResult
import com.tencent.qqmusic.openapisdk.core.player.OnVocalAccompanyStatusChangeListener
import com.tencent.qqmusic.openapisdk.core.player.VocalAccompanyType
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
    private val vocalAccompanyTypeSwitch: SwitchCompat by lazy {
        findViewById(R.id.vocalAccompanyType)
    }
    private val chorusModeSwitch: SwitchCompat by lazy { findViewById(R.id.chorusMode) }
    private val backButton : ImageButton by lazy { findViewById(R.id.btn_back) }
    private var chorusModeOperating = false
    private var updatingChorusModeSwitch = false
    // 仅在当前实例内记录上次成功设置的连续伴唱进度，退出页面后重新使用 SDK 默认值。
    private var vocalAccompanyProgress: Int? = null
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
        val vocalAccompanyApi = OpenApiSDK.getVocalAccompanyApi()
        if (vocalAccompanyProgress == null) {
            vocalAccompanyProgress = vocalAccompanyApi.currentVocalRadio().value
        }
        vocalAccompanyButton.setOnClickListener {
            val isVocalAccompanyOpened = vocalAccompanyApi.isVocalAccompanyOpened()
            val result = if (isVocalAccompanyOpened) {
                vocalAccompanyApi.disableVocalAccompany()
            } else {
                vocalAccompanyApi.setInitialVocalRadio(vocalAccompanyProgress!!)
                vocalAccompanyApi.enableVocalAccompany()
            }
            if (!result.isSuccess()) {
                UiUtils.showToast(result.msg)
            } else {
                onVocalAccompanyStatusChange(
                    vocalAccompanyProgress!!,
                    !isVocalAccompanyOpened
                )
            }
        }
        setupChorusModeSwitch()
        var isUpdatingVocalAccompanyType = false
        vocalAccompanyTypeSwitch.isChecked =
            vocalAccompanyApi.currentVocalAccompanyType() == VocalAccompanyType.GALAXY
        updateVocalAccompanyTypeSwitchText(vocalAccompanyTypeSwitch.isChecked)
        vocalAccompanyTypeSwitch.setOnCheckedChangeListener { _, useGalaxy ->
            if (isUpdatingVocalAccompanyType) {
                return@setOnCheckedChangeListener
            }
            val type = if (useGalaxy) {
                VocalAccompanyType.GALAXY
            } else {
                VocalAccompanyType.STEREO
            }
            val result = vocalAccompanyApi.setVocalAccompanyType(type)
            if (result.isSuccess()) {
                updateVocalAccompanyTypeSwitchText(useGalaxy)
                UiUtils.showToast(
                    "设置伴唱类型：${vocalAccompanyTypeName(type)}"
                )
            } else {
                isUpdatingVocalAccompanyType = true
                vocalAccompanyTypeSwitch.isChecked = !useGalaxy
                isUpdatingVocalAccompanyType = false
                UiUtils.showToast(result.msg)
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
        var realVocalPercent = vocalAccompanyProgress!!
        vocalAccompanySeekbar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(seekbar: SeekBar?, progress: Int, fromUser: Boolean) {
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
                val result = vocalAccompanyApi.adjustVocalRadio(realVocalPercent)
                if (result.isSuccess()) {
                    vocalAccompanyProgress = realVocalPercent
                } else {
                    UiUtils.showToast(result.msg)
                    seekbar?.progress = vocalAccompanyProgress!!
                }
                // 延迟隐藏进度文本
                progressTextView.postDelayed({
                    progressTextView.visibility = View.GONE
                }, 1000)
            }
        })
        onVocalAccompanyStatusChange(
            vocalAccompanyProgress!!,
            vocalAccompanyApi.isVocalAccompanyOpened()
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
                                    if (tryResult.isSuccess()) {
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

    override fun onResume() {
        super.onResume()
        syncChorusModeSwitchState()
    }

    private fun setupChorusModeSwitch() {
        syncChorusModeSwitchState()
        chorusModeSwitch.setOnCheckedChangeListener { _, enable ->
            if (updatingChorusModeSwitch || chorusModeOperating) {
                return@setOnCheckedChangeListener
            }
            val chorusModeApi = OpenApiSDK.getChorusModeApi()
            if (enable) {
                chorusModeOperating = true
                syncChorusModeSwitchState()
                chorusModeApi.enableChorusMode { result ->
                    runOnUiThread {
                        chorusModeOperating = false
                        syncChorusModeSwitchState()
                        if (result == ChorusModeOperationResult.SUCCESS) {
                            val message = if (chorusModeApi.isCurrentSongPlayingInChorusMode()) {
                                "副歌模式已开启，当前歌曲开始挑战"
                            } else {
                                "副歌模式已开启，下一首开始挑战"
                            }
                            UiUtils.showToast(message)
                        } else {
                            UiUtils.showToast(result.message)
                        }
                    }
                }
            } else {
                val result = chorusModeApi.disableChorusMode()
                syncChorusModeSwitchState()
                if (result == ChorusModeOperationResult.SUCCESS) {
                    UiUtils.showToast("副歌模式已关闭")
                } else {
                    UiUtils.showToast(result.message)
                }
            }
        }
    }

    private fun syncChorusModeSwitchState() {
        val enabled = OpenApiSDK.getChorusModeApi().isChorusModeEnabled()
        updatingChorusModeSwitch = true
        chorusModeSwitch.isChecked = enabled
        updatingChorusModeSwitch = false
        chorusModeSwitch.isEnabled = !chorusModeOperating
        chorusModeSwitch.text = when {
            chorusModeOperating -> "唱副歌模式（正在开启）"
            enabled -> "唱副歌模式（已开启）"
            else -> "唱副歌模式（已关闭）"
        }
    }

    private fun queryTryVocalAccompany() {
        val vocalAccompanyApi = OpenApiSDK.getVocalAccompanyApi()
        val type = vocalAccompanyApi.currentVocalAccompanyType()
        val songInfo = OpenApiSDK.getPlayerApi().getCurrentSongInfo()
        val canTry = if (type == VocalAccompanyType.GALAXY) {
            songInfo?.canTryGalaxyVocalAccompany() == true
        } else {
            songInfo?.canTryVocalAccompany() == true
        }
        UiUtils.showToast("当前歌曲${if (canTry) "拥有" else "没有"}${vocalAccompanyTypeName(type)}试用权益")
    }

    private fun updateVocalAccompanyTypeSwitchText(useGalaxy: Boolean) {
        vocalAccompanyTypeSwitch.text = if (useGalaxy) "全景声伴唱" else "立体声伴唱"
    }

    private fun vocalAccompanyTypeName(type: VocalAccompanyType): String {
        return if (type == VocalAccompanyType.GALAXY) "全景声伴唱" else "立体声伴唱"
    }

    override fun onVocalAccompanyStatusChange(vocalScale: Int, enable: Boolean) {
        runOnUiThread {
            if (enable) {
                vocalAccompanyProgress = vocalScale
            }
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
