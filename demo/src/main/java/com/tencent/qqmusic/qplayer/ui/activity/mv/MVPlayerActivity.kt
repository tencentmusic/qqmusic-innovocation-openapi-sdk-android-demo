package com.tencent.qqmusic.qplayer.ui.activity.mv

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.tencent.qqmusic.edgemv.IEdgeMediaNetWork
import com.tencent.qqmusic.edgemv.IEdgeMediaPlayer
import com.tencent.qqmusic.edgemv.OperateMediaCollectCmd
import com.tencent.qqmusic.edgemv.data.BlockReason
import com.tencent.qqmusic.edgemv.data.MediaQuality
import com.tencent.qqmusic.edgemv.data.MediaResDetail
import com.tencent.qqmusic.edgemv.data.MediaSwitchBlockReason
import com.tencent.qqmusic.edgemv.data.QualityBlockReason
import com.tencent.qqmusic.edgemv.player.IPlayEventCallback
import com.tencent.qqmusic.edgemv.player.PlayError
import com.tencent.qqmusic.innovation.common.logging.MLog
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.openapisdk.hologram.EdgeMvProvider
import com.tencent.qqmusic.player.PlayerState
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MVPlayerActivity : AppCompatActivity() {
    companion object {
        const val MV_ID = "mvid"
        const val QUALITY_PRE = "quality"
    }

    private val TAG = "MVPlayerActivity"
    private var mVideoSurfaceView: SurfaceView? = null
    private var playButton: Button? = null
    private var mvTitle: TextView? = null
    private var mvSingers: TextView? = null
    private var qualityMv: Button? = null
    private var muteButton: Button? = null
    private var loopButton: Button? = null

    private var totalTime: TextView? = null
    private var currentTime: TextView? = null
    private var surfaceView: Surface? = null
    private var currentPlayTime: Long? = 0L
    private var seekBar: AppCompatSeekBar? = null
    private var constraintLayout: ConstraintLayout? = null

    private var collectButton: Button? = null

    private var isUserChangeProcess: Boolean = false
    private var mediaNetWork: IEdgeMediaNetWork? = null

    private var mPlayer: IEdgeMediaPlayer? = null

    private var provider: EdgeMvProvider? = null

    private var startIndex: MediaResDetail? = null

    private val sharedPreferences: SharedPreferences? by lazy {
        try {
            this.getSharedPreferences("OpenApiSDKEnv", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            QLog.e("OtherScreen", "getSharedPreferences error e = ${e.message}")
            null
        }
    }
    private val eventCallback = object : IPlayEventCallback {
        override fun onEvent(state: PlayerState, data: Bundle?) {
            MLog.d(TAG, "[PlayState] state $state")
            handleState(state)
        }

        override fun onVideoSizeChanged(width: Int?, height: Int?) {
            AppScope.launchUI {
                onConfigurationChanged(width, height)
            }
        }

        override fun onError(error: PlayError, data: Any?) {
            var showBuyQr = false
            val tip = when (error) {
                PlayError.NO_QUALITY_REQUIRED -> "该媒体没有要求的品质"
                PlayError.DEVICE_NOT_SUPPORT_THIS_QUALITY -> "设备不支持该品质播放"

                PlayError.MEDIA_HAVE_NOT_QUALITY_TO_PLAY_FOR_USER -> ""

                PlayError.CAN_NOT_PLAY -> {
                    when (val reason = data as? BlockReason) {
                        BlockReason.VIDEO_PURCHASE_BLOCK -> {
                            showBuyQr = true
                            "需要购买"
                        }

                        BlockReason.HUGE_VIP_MEMBER_BLOCK -> "需要超会"
                        BlockReason.SUPER_GREEN_VIP_MEMBER_BLOCK -> "需要豪华绿钻"
                        BlockReason.NO_COPYRIGHT -> "无版权"
                        else -> reason.toString()
                    }
                }

                PlayError.MEDIA_DOWNGRADE_PLAY -> {
                    "降级播放"
                }


                PlayError.MEDIA_RES_HAS_EXPIRED -> {
                    "资源已经过期，需要刷新，请刷新界面"
                }

                PlayError.NO_PERMISSION_TO_PLAY_QUALITY -> {
                    val start = "需要 "
                    var s = start
                    val reason = data as? List<*> ?: emptyList<QualityBlockReason>()
                    for (item in reason) {
                        if (s != start) {
                            s += " 或 "
                        }
                        when (item) {
                            QualityBlockReason.NEED_SUPER_VIP -> {
                                s += "超会"
                            }

                            QualityBlockReason.NEED_PAY_FOR_MEDIA -> {
                                showBuyQr = true
                                s += "手机端购买"
                            }

                            QualityBlockReason.NEED_IOT_VIP -> {
                                s += "IOT独立会员"
                            }

                            QualityBlockReason.NO_PERMISSION_FOR_UER_OR_DEVICE -> {
                                s = if (startIndex?.isHaveQuality(MediaQuality.EXCELLENT) == true && mPlayer?.getExpectQuality() == MediaQuality.EXCELLENT) {
                                    if (mPlayer?.isDeviceSupportHighFrame() == true) {
                                        "当前用户无权限播放"
                                    } else {
                                        "当前设备不支持高帧率资源播放"
                                    }
                                } else {
                                    "当前用户无权限播放"
                                }
                                break
                            }

                            QualityBlockReason.NEED_GREEN_VIP -> {
                                s += "绿钻"
                            }

                            QualityBlockReason.NEED_SUPER_GREEN_VIP -> {
                                s += "豪华绿钻"
                            }
                        }
                    }
                    s += " 才能播放"
                    s
                }

                else -> ""
            }
            AppScope.launchUI {
                Toast.makeText(this@MVPlayerActivity, tip, Toast.LENGTH_SHORT).show()

                if (showBuyQr) {
                    MvBuyQRDialog.showQRCodeDialog(this@MVPlayerActivity, startIndex?.getBuyQRUrl()) {
                        AppScope.launchUI {
                            Toast.makeText(this@MVPlayerActivity, "购买后请重新进入界面", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }


    private fun onConfigurationChanged(mVideoWidth: Int?, mVideoHeight: Int?) {
        val texture = mVideoSurfaceView
        if (texture != null && texture.height != 0 && texture.width != 0 && mVideoWidth != 0 && mVideoHeight != 0) {
            //1. 获取视频播放宽高比例
            val videoDelta: Float = (mVideoWidth ?: 0) * 1.0f / (mVideoHeight ?: 1)
            MLog.i(TAG, "[onConfigurationChanged]: videoDelta = $videoDelta")
            //2. 获取屏幕可以播放的视频区域的宽高比例
            val screenWidth: Float = constraintLayout?.width?.toFloat() ?: 0F
            val screenHeight: Float = constraintLayout?.height?.toFloat() ?: 1F
            val screenDelta = screenWidth / screenHeight
            //3. 根据视频比例与播放屏幕比例关系设置 TextureView 的播放大小
            val params: ViewGroup.LayoutParams = texture.layoutParams
            if (videoDelta <= screenDelta) {
                //3.1 视频比例小于等于屏幕可播放比例，代表 TextureView 的宽度要小于可播放区域的宽度，即左右有黑边
                val height = screenHeight.toInt()
                params.height = height
                params.width = (height * videoDelta).toInt()
            } else {
                //3.2 视频播放比例大于屏幕可播放比例，代表 TextureView 的高度要小于可播放区域的高度，即上下有黑边
                val width = screenWidth.toInt()
                params.width = width
                params.height = (screenWidth / videoDelta).toInt()
            }
            texture.layoutParams = params
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        provider = OpenApiSDK.getProviderByClass(EdgeMvProvider::class.java)
        mediaNetWork = provider?.getMediaNetWorkImpl()
        mPlayer = provider?.getMediaPlayer()

        AppScope.launchIO {
            try {
                val quality = sharedPreferences?.getInt(QUALITY_PRE, MediaQuality.SQ.value)
                val mvQua = MediaQuality.values().first { it.value == quality }
                mPlayer?.setExceptMvQuality(mvQua)
                val cacheSize = sharedPreferences?.getInt("MV_CACHE", 500) ?: 500
                provider?.setCacheSize(cacheSize)
            } catch (ignore: Exception) {
                MLog.d(TAG, "[onCreate] e  ")
            }
        }
        stopMusicPlay()
        setContentView(R.layout.activity_mvplayer)
        initView()
        mPlayer?.setEventCallback(eventCallback)
        val index = intent.getStringExtra(MV_ID) ?: ""
        startPlayVideo(index)
    }


    override fun onStart() {
        super.onStart()
        initSurface()
        updatePlayTime()
    }


    private fun initView() {
        constraintLayout = findViewById(R.id.root_con)

        collectButton = findViewById<Button?>(R.id.collect_button).apply {
            setOnClickListener {
                mPlayer?.getCurrentMediaRes()?.let { it ->
                    if (it.getSwitchBitPass(MediaSwitchBlockReason.FAVORITE).not()) {
                        AppScope.launchUI {
                            Toast.makeText(this@MVPlayerActivity, "该MV无收藏权限", Toast.LENGTH_SHORT).show()
                        }
                        return@let
                    }
                    mediaNetWork?.operateMediaCollectStatus(
                        listOf(it),
                        if (it.isCollect == 1) OperateMediaCollectCmd.UN_COLLECT else OperateMediaCollectCmd.COLLECT,
                    ) { resp ->
                        if (resp.data == true) {
                            mediaNetWork?.getMediaInfo(listOf(it.id ?: "")) {
                                if (it.isSuccess()) {
                                    it.data?.firstOrNull()?.let { item ->
                                        mPlayer?.setMediaRes(item)
                                        updateMVView()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        mvTitle = findViewById(R.id.mv_title)
        mvSingers = findViewById(R.id.singerName)
        seekBar = findViewById(R.id.play_time_seekbar)
        qualityMv = findViewById<Button?>(R.id.mv_quality).apply {
            setOnClickListener {
                changeQuality()
                updateQualityText()
            }
        }
        playButton = findViewById<Button>(R.id.play)?.apply {
            setOnClickListener {
                if (mPlayer?.isPlaying() == true) {
                    mPlayer?.pause()
                } else {
                    mPlayer?.play()
                }
            }
        }

        loopButton = findViewById<Button>(R.id.loop_play_button).apply {
            setOnClickListener {
                val curr = mPlayer?.getLoopStatus() ?: false
                mPlayer?.setLoop(curr.not())
                updateMVView()
            }
        }


        mVideoSurfaceView = findViewById(R.id.mv_view)
        totalTime = findViewById(R.id.total_time)
        currentTime = findViewById(R.id.current_time)
        currentTime?.text = convertSecondsToMinutesSeconds(0)
        muteButton = findViewById<Button?>(R.id.mute_button).apply {
            setOnClickListener {
                val isMute = mPlayer?.isMute() ?: false
                val mutePlay = isMute.not()
                mPlayer?.setMute(mutePlay)
                muteButton?.text = if (isMute) "静音" else "解除静音"
            }
        }
        muteButton?.text = if (mPlayer?.isMute() != true) "静音" else "解除静音"
        initSeekbar()
    }


    private fun initSeekbar() {
        seekBar?.let {
            it.max = mPlayer?.getCurrentMediaRes()?.playTime?.toIntOrNull() ?: 0
            it.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                var userSelectTime = 0
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        userSelectTime = progress
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    isUserChangeProcess = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    mPlayer?.seekTo(userSelectTime * 1000L)
                    isUserChangeProcess = false
                }
            })
        }
    }

    private fun updatePlayTime(time: Long?) {
        if (isUserChangeProcess.not()) {
            val progress = (time?.toInt() ?: 0) / 1000
            seekBar?.progress = progress
            currentTime?.text = convertSecondsToMinutesSeconds(progress)
        }
    }


    private fun updateQualityText() {
        val real = mPlayer?.getCurrentQuality()
        qualityMv?.text = MediaQualityDialog.qualityMap[real]
    }

    @SuppressLint("CommitPrefEdits")
    private fun changeQuality() {
        MediaQualityDialog.showQualityAlert(this@MVPlayerActivity, mPlayer?.getCurrentMediaRes(), mPlayer?.getDeviceSupportQualityList() ?: emptyList()) { quailty ->
            val mQuality = quailty ?: MediaQuality.LQ
            mPlayer?.setExceptMvQuality(mQuality)
            AppScope.launchIO {
                sharedPreferences?.edit()?.apply {
                    putInt(QUALITY_PRE, mQuality.value)
                    apply()
                }
            }
        }
    }

    private fun stopMusicPlay() {
        OpenApiSDK.getPlayerApi().apply {
            if (getPlayState() == PlayDefine.PlayState.MEDIAPLAYER_STATE_STARTED) {
                pause()
            }
        }
    }

    private fun startPlayVideo(mvids: String) {
        mediaNetWork?.getMediaInfo(listOf(mvids)) {
            it.data?.firstOrNull()?.let {
                startIndex = it
                AppScope.launchUI {
                    mPlayer?.setMediaRes(it)
                    mPlayer?.setSurface(surfaceView)
                    updateMVView()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateMVView() {
        AppScope.launchUI {
            val data = mPlayer?.getCurrentMediaRes()
            data?.let {
                updateQualityText()
                loopButton?.text = if (mPlayer?.getLoopStatus() == true) "循环播放" else "单次播放"
                collectButton?.text = if (it.isCollect == 0) "收藏" else "取消收藏"
                mvTitle?.text = it.title
                seekBar?.max = it.playTime?.toIntOrNull() ?: 0
                totalTime?.text = convertSecondsToMinutesSeconds(it.playTime?.toIntOrNull() ?: 0)
                val singer: String? = if ((it.singers?.size ?: 0) >= 2) {
                    it.singers?.joinToString(",") { it.name ?: "" }
                } else {
                    it.singerName
                }
                mvSingers?.text = "歌手 ：$singer"
            }
        }
    }

    private fun updatePlayTime() {
        lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                if (mPlayer?.isPlaying() == true) {
                    val time = mPlayer?.getPlayDuration() ?: 0
                    currentPlayTime = time
                    withContext(Dispatchers.Main) {
                        updatePlayTime(currentPlayTime)
                    }
                }
                delay(500)
            }
        }
    }


    private fun initSurface() {
        val mSurfaceHolderCallback = object : SurfaceHolder.Callback {
            override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
                val surface = surfaceHolder.surface
                this@MVPlayerActivity.surfaceView = surface
                mPlayer?.setSurface(surface)

            }

            override fun surfaceChanged(surfaceHolder: SurfaceHolder, format: Int, width: Int, height: Int) {
            }

            override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
                mPlayer?.setSurface(null)
            }
        }
        mVideoSurfaceView?.holder?.addCallback(mSurfaceHolderCallback)
    }

    private fun handleState(state: PlayerState) {
        AppScope.launchUI {
            muteButton?.text = if ((mPlayer?.isMute() == true).not()) "静音" else "解除静音"
            playButton?.text = if (mPlayer?.isPlaying() == true) "暂停" else "播放"
        }
        when (state) {
            PlayerState.SEEK_COMPLETED -> {
                AppScope.launchUI {
                    Toast.makeText(this@MVPlayerActivity, "进度调整完成", Toast.LENGTH_SHORT).show()
                }
            }

            PlayerState.PREPARED -> {
                updateMVView()
                mPlayer?.play()
            }

            else -> {}
        }
    }

    override fun onPause() {
        super.onPause()
        if (mPlayer?.isPlaying() == true) {
            mPlayer?.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mPlayer?.destroy()
        surfaceView?.release()
        surfaceView = null
        mPlayer = null
    }


    private fun convertSecondsToMinutesSeconds(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        val minutesString = minutes.toString().padStart(2, '0')
        val secondsString = remainingSeconds.toString().padStart(2, '0')
        return "$minutesString:$secondsString"
    }
}