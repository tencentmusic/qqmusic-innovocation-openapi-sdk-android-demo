package com.tencent.qqmusic.qplayer.ui.activity.player.voyage

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.Choreographer
import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.tencent.qqmusic.openapisdk.business_common.config.SongQualityManager.getSongHasQuality
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.IVoyageFunctionApi
import com.tencent.qqmusic.openapisdk.core.player.IVoyageOperaEventCallback
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums.Quality
import com.tencent.qqmusic.openapisdk.core.player.VoyageOperationResult
import com.tencent.qqmusic.openapisdk.voyage.VoyageImpl
import com.tencent.qqmusic.openapisdk.voyage.VoyageMotionEffectWidget
import com.tencent.qqmusic.playerinsight.util.coverErrorCode
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.qplayer.ui.activity.BaseActivity
import com.tencent.qqmusic.qplayer.ui.activity.player.CustomPlayerViewMode
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerObserver
import com.tencent.qqmusic.qplayer.utils.UiUtils
import com.tencent.qqmusic.qplayer.utils.UiUtils.getQualityName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import kotlin.random.Random

@SuppressLint("SetTextI18n")
class PlayerVoyageActivity : BaseActivity() {
    private val TAG = "PlayerVoyageActivity"
    private val rootView: PlayerVoyageView by lazy { PlayerVoyageView(this) }
    private var frameCount = 0
    private var lastFpsTime = System.currentTimeMillis()
    private var currentFps = 0
    private val choreographer = Choreographer.getInstance()

    private var currentWiperState = 0

    companion object {
        private const val SAVE_SPEED_THRESHOLD = 40f // 安全阈值
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            frameCount++
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastFpsTime >= 1000) {
                currentFps = (frameCount.toFloat() / ((currentTime - lastFpsTime) / 1000f)).toInt()
                frameCount = 0
                lastFpsTime = currentTime
                updateFpsDisplay()
            }
            choreographer.postFrameCallback(this)
        }
    }
    private val voyageApi: IVoyageFunctionApi by lazy { OpenApiSDK.getVoyageApi() }
    private val playerImpl by lazy { OpenApiSDK.getPlayerApi() }
    private val speedControl by lazy {
        SpeedControl(this, voyageApi) { speed, angle ->
            lifecycleScope.launch(Dispatchers.Main) {
                rootView.speedText.text =
                    "speed=$speed km/h, angle=$angle f, 阈值=$SAVE_SPEED_THRESHOLD km/h"
            }
        }
    }

    private val eventListener = object : IVoyageOperaEventCallback {
        override fun onEvent(event: VoyageOperationResult, data: Any?) {
            QLog.d(TAG, "VoyageEvent: $event, data: $data")
            when (event) {
                VoyageOperationResult.UnsupportedSong -> {
                    Toast.makeText(
                        this@PlayerVoyageActivity,
                        "当前歌曲不支持voyage",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {}
            }
        }
    }

    val viewModel = ViewModelProvider(this)[CustomPlayerViewMode::class.java]

    private val widget by lazy { VoyageMotionEffectWidget(viewModel, rootView = rootView.voyageView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(rootView)
        bindWidget(widget)
    }

    override fun onStart() {
        super.onStart()
        initLogic()
        updateButton()
        widget.onStartStatus()
        voyageApi.setVoyageFunctionStatusCallback(eventListener)
        choreographer.postFrameCallback(frameCallback)
    }

    override fun onStop() {
        super.onStop()
        widget.onStopStatus()
        choreographer.removeFrameCallback(frameCallback)
        voyageApi.deleteVoyageFunctionStatusCallback(eventListener)
        QLog.d(TAG, "onStop")
    }

    private fun updateFpsDisplay() {
        lifecycleScope.launch(Dispatchers.Main) {
            rootView.FPSTextView.text = "FPS: $currentFps"
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initLogic() {
        // 监听 PlayerObserver 的状态变化
        lifecycleScope.launch(Dispatchers.Main) {
            snapshotFlow { Pair(PlayerObserver.currentState, PlayerObserver.currentSong) }
                .collect {
                    rootView.playingSongView.text = "《${PlayerObserver.currentSong?.songName}》\n" +
                            "支持voyage: ${
                                PlayerObserver.currentSong?.let {
                                    getSongHasQuality(
                                        it,
                                        quality = Quality.VOYAGE
                                    )
                                }
                            }\n" +
                            "音质回调:${PlayerObserver.mCurrentQuality?.getQualityName()}\n" +
                            "当前音质:${
                                playerImpl.getCurrentPlaySongQuality()?.getQualityName()
                            }\n" +
                            "音效:${playerImpl.getCurSoundEffect()?.name}"
                }
        }
        var isDay = rootView.dayNightMode.text == "白天"
        rootView.dayNightMode.text = if (isDay) "白天" else "夜晚"

        rootView.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        rootView.speedText.text = "speed=0 km/h, angle=0f, 阈值=$SAVE_SPEED_THRESHOLD km/h"

        rootView.enableButton.setOnClickListener {
            if (!voyageApi.getVoyageFunctionStatus()) {
                val res = voyageApi.openVoyageFunction()
                if (res.isFunctionAvailable().not()) {
                    UiUtils.showToast(res.tip)
                }
                voyageApi.updateIsDay(isDay)
                voyageApi.updateThreshold(SAVE_SPEED_THRESHOLD)
            } else {
                voyageApi.closeVoyageFunction()
            }
            updateButton()
        }
        rootView.FPSTextView.text = "FPS: 0.0"

        rootView.wipersStatus.setOnClickListener {
            if (currentWiperState >= 12) {
                currentWiperState = 0
            } else {
                currentWiperState += 1
            }
            voyageApi.updateWipersStatus(currentWiperState)
            updateButton()
        }

        rootView.dayNightMode.setOnClickListener {
            isDay = !isDay
            rootView.dayNightMode.text = if (isDay) "白天" else "夜晚"
            rootView.FPSTextView.setTextColor(if (isDay) Color.BLACK else Color.WHITE)
            updateButton()
        }

        rootView.leftButton.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                speedControl.left()
            } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                speedControl.resetTurningAngle()
            }
            return@setOnTouchListener false
        }

        rootView.rightButton.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                speedControl.right()
            } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                speedControl.resetTurningAngle()
            }
            return@setOnTouchListener false
        }

        rootView.preSong.setOnClickListener {
            UiUtils.showToast(coverErrorCode(OpenApiSDK.getPlayerApi().prev()))
        }

        rootView.nextSong.setOnClickListener {
            UiUtils.showToast(coverErrorCode(OpenApiSDK.getPlayerApi().next()))
        }

        rootView.topButton.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                speedControl.addSpeed()
            } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                speedControl.resetAcc()
            }
            return@setOnTouchListener false
        }

        rootView.bottomButton.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                speedControl.reduceSpeed()
            } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                speedControl.resetAcc()
            }
            return@setOnTouchListener false
        }

        rootView.lyric.setOnClickListener {
            voyageApi.enableLyricDisplay(!voyageApi.getLyricDisplayStatus())
            updateButton()
        }

        rootView.star.setOnClickListener {
            val value = voyageApi.getMeteorStatus()
            voyageApi.enableMeteor(!value)
            updateButton()
        }


    }


    private fun updateButton() {
        rootView.enableButton.text =
            if (voyageApi.getVoyageFunctionStatus()) "关闭voyage" else "开启voyage"
        if (rootView.dayNightMode.text == "白天") {
            voyageApi.updateIsDay(true)
            rootView.dayNightMode.text = "白天"
        } else {
            rootView.dayNightMode.text = "黑夜"
            voyageApi.updateIsDay(false)
        }
        rootView.star.text = if (voyageApi.getMeteorStatus()) "关闭流星" else "开启流星"
        rootView.lyric.text = if (voyageApi.getLyricDisplayStatus()) "关闭歌词" else "开启歌词"
        rootView.wipersStatus.text = "雨刷:$currentWiperState"
    }


    class SpeedControl(
        lifecycleOwner: LifecycleOwner,
        val api: IVoyageFunctionApi,
        val speedCall: (speed: Float, angle: Float) -> Unit
    ) {
        private var baseSpeed = 0.0f
        private var acc = 0f
        private val maxSpeed = 230f

        private var baseTurningAngle = 0f
        private var turningAngle = 0.0f
        private val zeroTurnAngle = 0.1f

        init {
            lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                while (true) {
                    delay(100)
                    if (baseSpeed <= maxSpeed && acc != 0f) {
                        val mAcc = if (acc > 0) {
                            Random.nextInt(30, 100) / 100f
                        } else {
                            -(Random.nextInt(30, 100) / 100f)
                        }
                        baseSpeed += ((mAcc * 0.1f) * 3.6f)
                        baseSpeed = baseSpeed.coerceIn(0f, maxSpeed)
                        api.updateAcc(mAcc)
                        api.updateSpeed(getSpeed())
                    }

                    if (baseTurningAngle != 0f || turningAngle != 0f) {
                        baseTurningAngle += (turningAngle)
                        if (baseTurningAngle > 0) {
                            baseTurningAngle =
                                (BigDecimal(baseTurningAngle.toString()) - BigDecimal(zeroTurnAngle.toString())).toFloat()
                        } else {
                            baseTurningAngle =
                                (BigDecimal(baseTurningAngle.toString()) + BigDecimal(zeroTurnAngle.toString())).toFloat()
                        }
                        baseTurningAngle = baseTurningAngle.coerceIn(-1f, 1f)
                    }
                    api.updateSteer(getTurningAngle())
                    speedCall(getSpeed(), getTurningAngle())
                }
            }
        }

        fun addSpeed() {
            acc = 2f
        }

        fun reduceSpeed() {
            acc = -2f
        }

        fun resetAcc() {
            acc = 0f
            api.updateAcc(acc)
        }


        fun left() {
            turningAngle = -0.3f
        }

        fun right() {
            turningAngle = 0.3f
        }

        fun resetTurningAngle() {
            turningAngle = 0f
        }

        fun getTurningAngle(): Float {
            return baseTurningAngle
        }


        fun getSpeed(): Float {
            return baseSpeed.coerceIn(0f, 230f)
        }


    }

}