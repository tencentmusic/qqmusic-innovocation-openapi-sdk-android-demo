package com.tencent.qqmusic.qplayer.ui.activity.mv.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.tencent.qqmusic.edgemv.IEdgeMediaNetWork
import com.tencent.qqmusic.edgemv.IEdgeMediaPlayer
import com.tencent.qqmusic.edgemv.data.BlockReason
import com.tencent.qqmusic.edgemv.data.MediaQuality
import com.tencent.qqmusic.edgemv.data.MediaResDetail
import com.tencent.qqmusic.edgemv.data.MediaSwitchBlockReason
import com.tencent.qqmusic.edgemv.data.QualityBlockReason
import com.tencent.qqmusic.edgemv.impl.OperateMediaCollectCmd
import com.tencent.qqmusic.edgemv.player.IPlayEventCallback
import com.tencent.qqmusic.edgemv.player.PlayError
import com.tencent.qqmusic.innovation.common.logging.MLog
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.player.PlayerState
import com.tencent.qqmusic.qplayer.App
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.qplayer.ui.activity.mv.MediaQualityDialog
import com.tencent.qqmusic.qplayer.ui.activity.mv.MvBuyQRDialog
import com.tencent.qqmusic.qplayer.ui.activity.mv.PlayerViewModel
import com.tencent.qqmusic.qplayer.utils.PerformanceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MVPlayerFragment(viewModelStoreOwner: ViewModelStoreOwner) : Fragment() {
    companion object {
        const val QUALITY_PRE = "quality"
        fun convertSecondsToMinutesSeconds(seconds: Int): String {
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            val minutesString = minutes.toString().padStart(2, '0')
            val secondsString = remainingSeconds.toString().padStart(2, '0')
            return "$minutesString:$secondsString"
        }
    }

    private val TAG = "MVPlayerFragment"
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
    private var loadingText: TextView? = null

    private var collectButton: Button? = null

    private var isUserChangeProcess: Boolean = false
    private var mediaNetWork: IEdgeMediaNetWork? = null
    private var mPlayer: IEdgeMediaPlayer? = null
    private var currentPlayState: PlayerState? = null

    private var buffering: Boolean = false


    private val playerViewModel: PlayerViewModel =
        ViewModelProvider(viewModelStoreOwner)[PlayerViewModel::class.java]

    private val sharedPreferences: SharedPreferences? by lazy {
        try {
            this.activity?.getSharedPreferences("OpenApiSDKEnv", Context.MODE_PRIVATE)
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

        @SuppressLint("UseRequireInsteadOfGet")
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
                                s = if (mPlayer?.getCurrentMediaRes()
                                        ?.isHaveQuality(MediaQuality.EXCELLENT) == true && mPlayer?.getExpectQuality() == MediaQuality.EXCELLENT
                                ) {
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
                Toast.makeText(this@MVPlayerFragment.context, tip, Toast.LENGTH_SHORT).show()
                if (showBuyQr) {
                    MvBuyQRDialog.showQRCodeDialog(
                        this@MVPlayerFragment.context!!,
                        playerViewModel.currentData.value?.getBuyQRUrl()
                    ) {
                        AppScope.launchUI {
                            Toast.makeText(
                                this@MVPlayerFragment.context,
                                "购买后请重新进入界面",
                                Toast.LENGTH_SHORT
                            ).show()
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
        mPlayer = playerViewModel.mPlayer
        mediaNetWork = playerViewModel.provider?.getMediaNetWorkImpl()
        AppScope.launchIO {
            try {
                val quality = sharedPreferences?.getInt(QUALITY_PRE, MediaQuality.SQ.value)
                val mvQua = MediaQuality.values().first { it.value == quality }
                mPlayer?.setExceptMvQuality(mvQua)
                val cacheSize = sharedPreferences?.getInt("MV_CACHE", 500) ?: 500
                playerViewModel.provider?.setCacheSize(cacheSize)
            } catch (ignore: Exception) {
                MLog.d(TAG, "[onCreate] e  ")
            }
        }
        stopMusicPlay()
        mPlayer?.setEventCallback(eventCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mvplayer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                playerViewModel.currentData.collectLatest {
                    startPlayVideo(it)
                }
            }
        }
    }


    override fun onStart() {
        super.onStart()
        initSurface()
        updatePlayTime()
    }


    private fun initView(view: View) {
        constraintLayout = view.findViewById(R.id.root_con)
        loadingText = view.findViewById(R.id.loading_state)
        collectButton = view.findViewById<Button?>(R.id.collect_button).apply {
            setOnClickListener {
                mPlayer?.getCurrentMediaRes()?.let { mediaResDetail ->
                    if (mediaResDetail.getSwitchBitPass(MediaSwitchBlockReason.FAVORITE).not()) {
                        AppScope.launchUI {
                            Toast.makeText(
                                this@MVPlayerFragment.context,
                                "该MV无收藏权限",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@let
                    }
                    mediaNetWork?.operateMediaCollectStatus(
                        listOf(mediaResDetail),
                        if (mediaResDetail.isCollect == 1) OperateMediaCollectCmd.UN_COLLECT else OperateMediaCollectCmd.COLLECT,
                    ) { resp ->
                        if (resp.data == true) {
                            mediaNetWork?.getMediaInfo(listOf(mediaResDetail.vid ?: "")) {
                                if (it.isSuccess()) {
                                    it.data?.firstOrNull()?.let { item ->
                                        playerViewModel.updateMedia(item)
                                        updateMVView()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        mvTitle = view.findViewById(R.id.mv_title)
        mvSingers = view.findViewById(R.id.singerName)
        seekBar = view.findViewById(R.id.play_time_seekbar)
        qualityMv = view.findViewById<Button?>(R.id.mv_quality).apply {
            setOnClickListener {
                changeQuality()
                updateQualityText()
            }
        }
        playButton = view.findViewById<Button>(R.id.play)?.apply {
            setOnClickListener {
                if (mPlayer?.isPlaying() == true) {
                    mPlayer?.pause()
                } else {
                    mPlayer?.play()
                }
            }
        }

        loopButton = view.findViewById<Button>(R.id.loop_play_button).apply {
            setOnClickListener {
                val curr = mPlayer?.getLoopStatus() ?: false
                mPlayer?.setLoop(curr.not())
                updateMVView()
            }
        }


        mVideoSurfaceView = view.findViewById(R.id.mv_view)
        totalTime = view.findViewById(R.id.total_time)
        currentTime = view.findViewById(R.id.current_time)
        currentTime?.text = convertSecondsToMinutesSeconds(0)
        muteButton = view.findViewById<Button?>(R.id.mute_button).apply {
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
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
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

    @SuppressLint("CommitPrefEdits", "UseRequireInsteadOfGet")
    private fun changeQuality() {
        MediaQualityDialog.showQualityAlert(
            this@MVPlayerFragment.activity!!,
            playerViewModel.currentData.value,
            mPlayer?.getDeviceSupportQualityList() ?: emptyList()
        ) { quailty ->
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

    private fun startPlayVideo(mediaResDetail: MediaResDetail?) {
        AppScope.launchUI {
            mPlayer?.setMediaRes(mediaResDetail)
            mPlayer?.setSurface(surfaceView)
            updateMVView(mediaResDetail)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateMVView(mediaResDetail: MediaResDetail? = mPlayer?.getCurrentMediaRes()) {
        AppScope.launchUI {
            mediaResDetail?.let {
                updateQualityText()
                loopButton?.text = if (mPlayer?.getLoopStatus() == true) "循环播放" else "单次播放"
                collectButton?.text = if (it.isCollect == 0) "收藏" else "取消收藏"
                mvTitle?.text = it.title
                seekBar?.max = it.playTime?.toIntOrNull() ?: 0
                totalTime?.text = convertSecondsToMinutesSeconds(it.playTime?.toIntOrNull() ?: 0)
                val singer: String? = if ((it.singers?.size ?: 0) >= 2) {
                    it.singers?.joinToString(",") { mediaSinger -> mediaSinger.name ?: "" }
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
                this@MVPlayerFragment.surfaceView = surface
                mPlayer?.setSurface(surface)

            }

            override fun surfaceChanged(
                surfaceHolder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
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
        if (state != PlayerState.SEEK_COMPLETED) {
            currentPlayState = state
        }

        when (state) {
            PlayerState.SEEK_COMPLETED -> {
                AppScope.launchUI {
                    Toast.makeText(
                        this@MVPlayerFragment.context,
                        "进度调整完成",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            PlayerState.PREPARED -> {
                PerformanceHelper.monitorMvPlay(TAG)
                updateMVView()
                loadingText?.isVisible = false
                mPlayer?.play()
            }


            PlayerState.BUFFERING_START -> {
                buffering = true
                AppScope.launchUI {
                    loadingText?.isVisible = true
                    loadingText?.text = "加载中"
                }
            }

            PlayerState.ERROR -> {
                if (buffering) {
                    loadingText?.isVisible = true
                    loadingText?.text =
                        if (isNetworkAvailable(requireContext())) "缓冲失败" else "网络错误，请检查网络设置"
                }
            }


            PlayerState.BUFFERING_END -> {
                AppScope.launchUI {
                    if (buffering) {
                        loadingText?.isVisible = false
                    }
                    buffering = false
                }

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
        surfaceView?.release()
        surfaceView = null
        mPlayer = null
    }


    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val actNw =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            return actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }

}