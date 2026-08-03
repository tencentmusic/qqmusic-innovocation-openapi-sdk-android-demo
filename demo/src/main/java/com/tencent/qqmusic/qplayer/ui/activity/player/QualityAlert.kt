package com.tencent.qqmusic.qplayer.ui.activity.player

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tencent.qqmusic.openapisdk.business_common.utils.Utils
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums.Quality
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.playerinsight.util.coverErrorCode
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.utils.UiUtils
import com.tencent.qqmusic.qplayer.utils.UiUtils.getQualityName
import kotlin.concurrent.thread

object QualityAlert {
    private const val TAG = "QualityAlert"
    val qualityOrder =
        listOf(
            PlayerEnums.Quality.STANDARD,
            PlayerEnums.Quality.NAC,
            PlayerEnums.Quality.HQ,
            PlayerEnums.Quality.SQ,
            PlayerEnums.Quality.SQ_SR,
            PlayerEnums.Quality.DOLBY,
            PlayerEnums.Quality.HIRES,
            PlayerEnums.Quality.EXCELLENT,
            PlayerEnums.Quality.GALAXY,
            PlayerEnums.Quality.MASTER_TAPE,
            PlayerEnums.Quality.MASTER_SR,
            PlayerEnums.Quality.DTSC,
            PlayerEnums.Quality.DTSX,
            PlayerEnums.Quality.CUSTOM_QUALITY_1,
        )

    /** 获取音质副标题（仅文件大小 + 试听标记，不含权限标签） */
    private fun getQualitySubtitle(
        quality: Int,
        curSong: SongInfo?,
        isDownload: Boolean,
        checkSongAvailability: Boolean
    ): String {
        if (!checkSongAvailability) {
            return ""
        }
        val qualitySize = if (curSong == null) 0L
        else OpenApiSDK.getPlayerApi().getSongQualitySize(curSong, quality)
        val tryLabel = if (OpenApiSDK.getPlayerApi().canTryOpenQuality(curSong, quality)) " · 可试听" else ""

        return when (quality) {
            Quality.DOLBY -> UiUtils.getFormatSize(curSong?.getSizeDolby()?.toLong()) + tryLabel
            Quality.EXCELLENT -> if (isDownload) "不支持下载" else UiUtils.getFormatSize(qualitySize) + tryLabel
            Quality.WANOS -> if (isDownload) "不支持下载" else ""
            Quality.NAC -> if (isDownload) "不支持下载" else UiUtils.getFormatSize(qualitySize) + tryLabel
            else -> UiUtils.getFormatSize(qualitySize) + tryLabel
        }
    }

    /**
     * 获取音质的权限标签列表（VIP、SuperVIP 等），用于以标签形式展示
     */
    private fun getAccessTags(
        quality: Int,
        curSong: SongInfo?,
        isDownload: Boolean,
        checkSongAvailability: Boolean
    ): List<AccessTag> {
        if (!checkSongAvailability || curSong == null) return emptyList()
        val access = if (isDownload) {
            OpenApiSDK.getDownloadApi().getDownloadAccessByQuality(curSong, quality)
        } else {
            OpenApiSDK.getPlayerApi().getAccessByQuality(curSong, quality)
        } ?: return emptyList()

        val tags = mutableListOf<AccessTag>()
        if (access.vip) {
            tags.add(AccessTag("VIP", Color.parseColor("#99333333"), Color.parseColor("#57FF5B")))
        }
        if (!access.vip && access.hugeVip) {
            tags.add(AccessTag("SVIP", Color.parseColor("#99333333"), Color.parseColor("#FFEBAA")))
        }
        if (access.iotVip) {
            tags.add(AccessTag("IoT会员", Color.parseColor("#2196F3"), Color.parseColor("#E3F2FD")))
        }
        if (access.vipLongAudio) {
            tags.add(AccessTag("听书会员", Color.parseColor("#009688"), Color.parseColor("#E0F2F1")))
        }
        if (access.payTrack) {
            tags.add(AccessTag("付费单曲", Color.parseColor("#F44336"), Color.parseColor("#FFEBEE")))
        }
        if (access.payAlbum) {
            tags.add(AccessTag("付费专辑", Color.parseColor("#F44336"), Color.parseColor("#FFEBEE")))
        }
        if (access.unionVip) {
            tags.add(AccessTag("联合会员", Color.parseColor("#795548"), Color.parseColor("#EFEBE9")))
        }
        return tags
    }

    /** 权限标签数据类 */
    private data class AccessTag(val text: String, val textColor: Int, val bgColor: Int)

    fun showQualityAlert(
        activity: Activity,
        isDownload: Boolean,
        setBlock: (Int) -> Int,
        refresh: (Int) -> Unit,
        songInfo: SongInfo? = null,
        checkSongAvailability: Boolean = true
    ) {
        val playerApi = OpenApiSDK.getPlayerApi()
        val curSong = if (checkSongAvailability) {
            songInfo ?: playerApi.getCurrentSongInfo()
        } else {
            null
        }
        val displayQualities = when {
            !checkSongAvailability -> qualityOrder
            curSong != null && playerApi.getSongHasQuality(curSong, Quality.WANOS) -> listOf(Quality.WANOS)
            curSong != null && playerApi.getSongHasQuality(curSong, Quality.VINYL) -> listOf(Quality.VINYL)
            else -> qualityOrder
        }

        @Suppress("DEPRECATION")
        val currentQuality = try {
            if (checkSongAvailability) {
                playerApi.getCurrentPlaySongQuality() ?: -1
            } else {
                playerApi.getPreferSongQuality()
            }
        } catch (e: Exception) {
            -1
        }

        val dialog = BottomSheetDialog(activity)

        // ---- 根容器 ----
        val rootLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            val bg = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadii = floatArrayOf(24f, 24f, 24f, 24f, 0f, 0f, 0f, 0f)
            }
            background = bg
        }

        // ---- 顶部拖拽指示条 ----
        val handleBar = View(activity).apply {
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#E0E0E0"))
                cornerRadius = 4f
            }
            background = bg
        }
        val handleParams = LinearLayout.LayoutParams(80.dp(activity), 4.dp(activity)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = 5.dp(activity)
            bottomMargin = 2.dp(activity)
        }
        rootLayout.addView(handleBar, handleParams)

        // ---- 标题栏 ----
        val titleLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(20.dp(activity), 3.dp(activity), 16.dp(activity), 3.dp(activity))
        }
        val titleText = TextView(activity).apply {
            text = when {
                isDownload -> "选择下载音质"
                checkSongAvailability -> "选择播放音质"
                else -> "选择默认音质"
            }
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1A1A"))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val closeBtn = TextView(activity).apply {
            text = "✕"
            textSize = 16f
            setTextColor(Color.parseColor("#999999"))
            setPadding(12.dp(activity), 8.dp(activity), 12.dp(activity), 8.dp(activity))
            setOnClickListener { dialog.dismiss() }
        }
        titleLayout.addView(titleText)
        titleLayout.addView(closeBtn)
        rootLayout.addView(titleLayout)

        // ---- 分割线 ----
        rootLayout.addView(View(activity).apply {
            setBackgroundColor(Color.parseColor("#F0F0F0"))
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1))

        // ---- 音质列表（NestedScrollView 包裹，解决与 BottomSheetDialog 的手势冲突） ----
        val scrollView = NestedScrollView(activity).apply {
            isVerticalScrollBarEnabled = false
        }
        val listLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 4.dp(activity), 0, 16.dp(activity))
        }

        // 按是否拥有音质排序：有音质的排前面，没有的排后面
        val sortedQualities = if (checkSongAvailability && curSong != null) {
            displayQualities.sortedByDescending { playerApi.getSongHasQuality(curSong, it) }
        } else {
            displayQualities
        }

        sortedQualities.forEach { quality ->
            val hasQuality = if (checkSongAvailability && curSong != null) {
                playerApi.getSongHasQuality(curSong, quality)
            } else {
                true
            }
            val isCurrentQuality = quality == currentQuality
            val displayName = quality.getQualityName()
            val subtitle = getQualitySubtitle(quality, curSong, isDownload, checkSongAvailability)
            val accessTags = getAccessTags(quality, curSong, isDownload, checkSongAvailability)

            val itemView = buildQualityItem(
                activity = activity,
                displayName = displayName,
                subtitle = subtitle,
                hasQuality = hasQuality,
                isCurrentQuality = isCurrentQuality,
                accessTags = accessTags
            ) {
                dialog.dismiss()
                thread {
                    val nextQuality = quality
                    val ret = setBlock(nextQuality)
                    val msg = when (ret) {
                        PlayDefine.PlayError.PLAY_ERR_NONE -> {
                            refresh(nextQuality)
                            if (checkSongAvailability) "切换歌曲品质成功" else "设置默认音质成功"
                        }
                        PlayDefine.PlayError.PLAY_ERR_DEVICE_NO_SUPPORT -> "设备不支持 ${Utils.qualityToString(nextQuality)} 音质"
                        PlayDefine.PlayError.PLAY_ERR_NO_QUALITY -> if (checkSongAvailability) "没有对应音质" else "不支持此默认音质"
                        PlayDefine.PlayError.PLAY_ERR_PLAYER_ERROR -> "播放器异常"
                        PlayDefine.PlayError.PLAY_ERR_NEED_VIP -> "需要 VIP"
                        PlayDefine.PlayError.PLAY_ERR_CANNOT_PLAY -> "歌曲不能播放"
                        PlayDefine.PlayError.PLAY_ERR_NONETWORK -> "无网络"
                        PlayDefine.PlayError.PLAY_ERR_UNSUPPORT,
                        PlayDefine.PlayError.PLAY_ERR_CAN_NOT_SET_CURRENT_QUALITY -> "不支持切换此音质"
                        PlayDefine.PlayError.PLAY_ERR_NEED_SUPER_VIP -> "需要超级会员"
                        PlayDefine.PlayError.PLAY_ERR_NEED_PAY_ALBUM -> "需要专辑付费"
                        PlayDefine.PlayError.PLAY_ERR_NEED_PAY_TRACK -> "需要单曲付费"
                        PlayDefine.PlayError.PLAY_ERR_NEED_VIP_LONG_AUDIO -> "需要听书会员"
                        else -> "ret=$ret, ${coverErrorCode(ret)}"
                    }
                    activity.runOnUiThread {
                        if (!isDownload) {
                            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            listLayout.addView(itemView)
        }

        scrollView.addView(listLayout)
        rootLayout.addView(scrollView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        dialog.setContentView(rootLayout)
        dialog.window?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)?.let {
            it.setBackgroundColor(Color.TRANSPARENT)
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
        dialog.show()
    }

    /**
     * 构建单个音质条目 View
     */
    private fun buildQualityItem(
        activity: Activity,
        displayName: String,
        subtitle: String,
        hasQuality: Boolean,
        isCurrentQuality: Boolean,
        accessTags: List<AccessTag> = emptyList(),
        onClick: () -> Unit
    ): View {
        val itemLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(20.dp(activity), 8.dp(activity), 20.dp(activity), 8.dp(activity))
            isClickable = true
            isFocusable = true
            val attrs = intArrayOf(android.R.attr.selectableItemBackground)
            val ta = activity.obtainStyledAttributes(attrs)
            background = ta.getDrawable(0)
            ta.recycle()
            setOnClickListener { onClick() }
        }

        val textLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        // 名称行：名称 + 权限标签（横向排列）
        val nameRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val nameText = TextView(activity).apply {
            text = displayName
            textSize = 15f
            setTypeface(null, if (isCurrentQuality) Typeface.BOLD else Typeface.NORMAL)
            setTextColor(when {
                isCurrentQuality -> Color.parseColor("#1DB954")
                hasQuality -> Color.parseColor("#1A1A1A")
                else -> Color.parseColor("#BBBBBB")
            })
        }
        nameRow.addView(nameText)

        // 添加权限标签（VIP / SVIP 等）
        accessTags.forEach { tag ->
            val tagView = TextView(activity).apply {
                text = tag.text
                textSize = 9f
                setTypeface(null, Typeface.BOLD)
                setTextColor(tag.textColor)
                val tagBg = GradientDrawable().apply {
                    setColor(tag.bgColor)
                    cornerRadius = 6.dp(activity).toFloat()
                }
                background = tagBg
                setPadding(4.dp(activity), 1.dp(activity), 4.dp(activity), 1.dp(activity))
            }
            val tagParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 6.dp(activity)
            }
            nameRow.addView(tagView, tagParams)
        }

        val subtitleText = TextView(activity).apply {
            text = subtitle
            textSize = 12f
            setTextColor(when {
                isCurrentQuality -> Color.parseColor("#4CAF50")
                hasQuality -> Color.parseColor("#999999")
                else -> Color.parseColor("#CCCCCC")
            })
            setPadding(0, 2.dp(activity), 0, 0)
        }

        textLayout.addView(nameRow)
        if (subtitle.isNotBlank()) {
            textLayout.addView(subtitleText)
        }
        itemLayout.addView(textLayout)

        if (isCurrentQuality) {
            val checkMark = TextView(activity).apply {
                text = "▶ 当前"
                textSize = 11f
                setTextColor(Color.parseColor("#1DB954"))
                setTypeface(null, Typeface.BOLD)
                val bg = GradientDrawable().apply {
                    setColor(Color.parseColor("#E8F5E9"))
                    cornerRadius = 20f
                }
                background = bg
                setPadding(8.dp(activity), 3.dp(activity), 8.dp(activity), 3.dp(activity))
            }
            itemLayout.addView(checkMark)
        } else if (!hasQuality) {
            val lockMark = TextView(activity).apply {
                text = "暂无"
                textSize = 11f
                setTextColor(Color.parseColor("#BBBBBB"))
                val bg = GradientDrawable().apply {
                    setColor(Color.parseColor("#F5F5F5"))
                    cornerRadius = 20f
                }
                background = bg
                setPadding(8.dp(activity), 3.dp(activity), 8.dp(activity), 3.dp(activity))
            }
            itemLayout.addView(lockMark)
        }

        return itemLayout
    }

    /** dp 转 px 扩展函数 */
    private fun Int.dp(context: Context): Int =
        (this * context.resources.displayMetrics.density + 0.5f).toInt()

    class CustomArrayAdapter(context: Context, private val items: List<String>, val songInfo: SongInfo?) :
        ArrayAdapter<String>(context, android.R.layout.select_dialog_item, items) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)
            val textView = view.findViewById<TextView>(android.R.id.text1)
            if (songInfo == null || items.size != qualityOrder.size) {
                textView.setTextColor(context.resources.getColor(R.color.text_black))
            } else {
                val quality = qualityOrder[position]
                val hasQuality = OpenApiSDK.getPlayerApi().getSongHasQuality(songInfo, quality)
                if (!hasQuality) {
                    textView.setTextColor(context.resources.getColor(R.color.text_gray))
                } else {
                    textView.setTextColor(context.resources.getColor(R.color.text_black))
                }
            }
            return view
        }
    }
}