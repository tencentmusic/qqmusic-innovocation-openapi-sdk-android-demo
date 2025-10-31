package com.tencent.qqmusic.qplayer.ui.activity.player.voyage

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.utils.UiUtils.dp2pxf

class PlayerVoyageView(context: Context) : ConstraintLayout(context) {
    val FPSTextView = TextView(context).apply {
        id = View.generateViewId()
    }

    val playingSongView = TextView(context).apply {
        id = generateViewId()
        text = "歌曲信息:"
    }

    val enableButton: Button by lazy {
        Button(context).apply {
            id = View.generateViewId()
        }
    }

    val backButton: ImageButton by lazy {
        ImageButton(context).apply {
            id = View.generateViewId()
            setImageResource(R.drawable.ic_back_dark) // 设置图标资源
            scaleType = ImageView.ScaleType.FIT_CENTER // 设置缩放类型
            adjustViewBounds = true // 保持宽高比
            background = null // 移除默认背景
            contentDescription = "返回" // 无障碍描述
        }
    }

    val dayNightMode: Button by lazy {
        Button(context).apply {
            id = View.generateViewId()
            text = "白天"
        }
    }

    val wipersStatus: Button by lazy {
        Button(context).apply {
            id = View.generateViewId()
            text = "雨刷"
        }
    }

    val preSong: Button by lazy {
        Button(context).apply {
            id = View.generateViewId()
            text = "上一曲"
        }
    }

    val nextSong: Button by lazy {
        Button(context).apply {
            id = View.generateViewId()
            text = "下一曲"
        }
    }

    val voyageView: FrameLayout by lazy {
        FrameLayout(context).apply {
            id = View.generateViewId()
        }
    }

    val speedText = TextView(context).apply {
        id = View.generateViewId()
    }

    val leftButton: Button by lazy {
        Button(context).apply {
            id = View.generateViewId()
            text = "左"
        }
    }
    val rightButton: Button by lazy {
        Button(context).apply {
            id = View.generateViewId()
            text = "右"
        }
    }

    val topButton: Button by lazy {
        Button(context).apply {
            id = View.generateViewId()
            text = "加速"
        }
    }

    val bottomButton: Button by lazy {
        Button(context).apply {
            id = View.generateViewId()
            text = "减速"
        }
    }

    val brakeButton: Button by lazy {
        Button(context).apply {
            id = View.generateViewId()
            text = "刹车"
        }
    }

    val lyric: Button by lazy {
        Button(context).apply {
            id = View.generateViewId()
        }
    }

    val star: Button by lazy {
        Button(context).apply {
            id = View.generateViewId()
        }
    }

    val allEffectButton: Button by lazy {
        Button(context).apply {
            id = View.generateViewId()
        }
    }


    init {
        initView()
        localInitView()
    }


    private fun initView() {
        removeAllViews()
        addView(voyageView)
        addView(backButton)
        addView(enableButton)
        addView(playingSongView)
        addView(leftButton)
        addView(rightButton)
        addView(topButton)
        addView(bottomButton)
        addView(dayNightMode)
        addView(wipersStatus)
        addView(speedText)
        addView(preSong)
        addView(nextSong)
        addView(brakeButton)
        addView(FPSTextView)
        addView(lyric)
        addView(star)
        addView(allEffectButton)
    }

    private fun localInitView() {
        ConstraintSet().apply {
            clone(this@PlayerVoyageView)
            with(voyageView.id) {
                constrainWidth(this, ConstraintSet.MATCH_CONSTRAINT)
                constrainHeight(this, ConstraintSet.MATCH_CONSTRAINT)
                connect(this, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(this, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                connect(this, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
                connect(this, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            }

            with(playingSongView.id) {
                connect(this, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                connect(this, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                connect(this, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            }

            with(backButton.id) {
                constrainWidth(this, dp2pxf(40f).toInt())
                constrainHeight(this, dp2pxf(40f).toInt())
                connect(this, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(this, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, dp2pxf(10f).toInt())
            }

            with(FPSTextView.id) {
                connect(this, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                connect(this, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            }

            with(enableButton.id) {
                connect(this, ConstraintSet.START, backButton.id, ConstraintSet.END)
                connect(this, ConstraintSet.BOTTOM, backButton.id, ConstraintSet.BOTTOM)
            }

            with(dayNightMode.id) {
                connect(this, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(this, ConstraintSet.TOP, enableButton.id, ConstraintSet.BOTTOM, dp2pxf(8f).toInt())
            }

            with(lyric.id) {
                connect(this, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(this, ConstraintSet.TOP, dayNightMode.id, ConstraintSet.BOTTOM, dp2pxf(8f).toInt())
            }
            with(star.id) {
                connect(this, ConstraintSet.START, lyric.id, ConstraintSet.END)
                connect(this, ConstraintSet.BOTTOM, lyric.id, ConstraintSet.BOTTOM)
            }
            with(allEffectButton.id) {
                connect(this, ConstraintSet.START, star.id, ConstraintSet.END)
                connect(this, ConstraintSet.BOTTOM, star.id, ConstraintSet.BOTTOM)
            }

            with(wipersStatus.id) {
                connect(this, ConstraintSet.START, dayNightMode.id, ConstraintSet.END)
                connect(this, ConstraintSet.BOTTOM, dayNightMode.id, ConstraintSet.BOTTOM)
            }


            with(preSong.id) {
                connect(this, ConstraintSet.START, wipersStatus.id, ConstraintSet.END)
                connect(this, ConstraintSet.BOTTOM, wipersStatus.id, ConstraintSet.BOTTOM)
            }

            with(nextSong.id) {
                connect(this, ConstraintSet.START, preSong.id, ConstraintSet.END)
                connect(this, ConstraintSet.BOTTOM, preSong.id, ConstraintSet.BOTTOM)
            }

            with(speedText.id) {
                connect(this, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(this, ConstraintSet.TOP, lyric.id, ConstraintSet.BOTTOM, dp2pxf(10f).toInt())
            }

            with(leftButton.id) {
                connect(this, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, dp2pxf(10f).toInt())
                connect(this, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, dp2pxf(60f).toInt())
                connect(this, ConstraintSet.END, rightButton.id, ConstraintSet.START)
            }

            with(rightButton.id) {
                connect(this, ConstraintSet.START, leftButton.id, ConstraintSet.END)
                connect(this, ConstraintSet.BOTTOM, leftButton.id, ConstraintSet.BOTTOM)
                connect(this, ConstraintSet.END, topButton.id, ConstraintSet.START)
            }

            with(topButton.id) {
                connect(this, ConstraintSet.START, rightButton.id, ConstraintSet.END)
                connect(this, ConstraintSet.BOTTOM, rightButton.id, ConstraintSet.BOTTOM)
                connect(this, ConstraintSet.END, bottomButton.id, ConstraintSet.START)
            }

            with(bottomButton.id) {
                connect(this, ConstraintSet.START, topButton.id, ConstraintSet.END)
                connect(this, ConstraintSet.BOTTOM, topButton.id, ConstraintSet.BOTTOM)
                connect(this, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            }

            with(brakeButton.id) {
                connect(this, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, dp2pxf(10f).toInt())
                connect(this, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, dp2pxf(10f).toInt())
            }

            applyTo(this@PlayerVoyageView)
        }
    }


}