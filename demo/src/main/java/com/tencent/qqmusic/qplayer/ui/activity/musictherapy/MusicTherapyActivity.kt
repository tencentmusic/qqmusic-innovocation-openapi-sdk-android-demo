package com.tencent.qqmusic.qplayer.ui.activity.musictherapy

import android.os.Bundle
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintLayoutScope
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.innovation.common.util.ToastUtils
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine.PlayState
import com.tencent.qqmusic.openapisdk.core.player.musictherapy.AudioBrightnessEnum
import com.tencent.qqmusic.openapisdk.core.player.musictherapy.AudioDensityEnum
import com.tencent.qqmusic.openapisdk.core.player.musictherapy.AudioSpatialSenseEnum
import com.tencent.qqmusic.openapisdk.core.player.musictherapy.MusicTherapyElementEnum
import com.tencent.qqmusic.openapisdk.core.player.musictherapy.MusicTherapyParam
import com.tencent.qqmusic.openapisdk.core.player.musictherapy.MusicTherapyStatus
import com.tencent.qqmusic.openapisdk.core.player.musictherapy.OnMusicTherapyStateListener
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.qplayer.core.player.proxy.PlayStateProxyHelper

class MusicTherapyActivity : ComponentActivity(), OnMusicTherapyStateListener {
    companion object {
        private const val TAG = "MusicTherapyActivity"
    }
    private val musicTherapyViewModel: MusicTherapyViewModel by viewModels()
    
    private var mSurface: Surface? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MusicTherapyView()
        }
        OpenApiSDK.getMusicTherapyApi().setOnMusicTherapyStateListener(this)
        musicTherapyViewModel.fetchMusicTherapyConfig()
    }

    override fun onDestroy() {
        super.onDestroy()
        mSurface?.release()
        mSurface = null
    }

    @Preview
    @Composable
    fun MusicTherapyView() {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text(text = "音乐疗愈", fontSize = 18.sp) }, contentColor = Color.White
            )
        }) {
            ConstraintLayout(modifier = Modifier.fillMaxSize()) {
                VideoPlayer()
                AnimatedVisibility(visible = (musicTherapyViewModel.mvVid.value.isEmpty())) {
                    Image(
                        painter = rememberImagePainter(musicTherapyViewModel.therapyItem.value?.backPic),
                        contentScale = ContentScale.FillBounds,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                PageLoadingView()
                PageModeView()
                PageAIContentView()
                PageRegularContentView()
                DialogView()
                PlayButton()
            }
        }
    }

    @Composable
    private fun VideoPlayer() {
        val context = LocalContext.current
        val surfaceView = remember {
            SurfaceView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        mSurface = holder.surface
                        musicTherapyViewModel.mPlayer?.setSurface(holder.surface)
                        musicTherapyViewModel.playMedia(surface = mSurface)
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) {
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        musicTherapyViewModel.mPlayer?.setSurface(null)
                    }
                })
            }
        }
        AndroidView(
            factory = {
                surfaceView
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    @Composable
    private fun PageRegularContentView() {
        AnimatedVisibility(visible = (musicTherapyViewModel.therapyItem.value?.isRegular() == true)) {
            musicTherapyViewModel.updateVolumeArray()
            ConstraintLayout(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.constrainAs(createRef()) {
                    top.linkTo(parent.top, margin = 230.dp)
                    start.linkTo(parent.start, margin = 10.dp)
                }) {
                    val floatArray = musicTherapyViewModel.volumeArray.value
                    this.items(floatArray.size) { index ->
                        val volume = musicTherapyViewModel.volumeArray.value[index]
                        if (index == 0) {
                            Text(text = "轻音乐音量调整:$volume")
                        } else {
                            Text(text = "白噪音-${musicTherapyViewModel.therapyItem.value?.whiteVoiceList?.get(index - 1)?.title}音量调整:$volume")
                        }
                        val newValue = remember { mutableStateOf(volume) }
                        newValue.value = volume
                        Slider(
                            value = newValue.value,
                            onValueChange = { changeValue ->
                                newValue.value = changeValue
                                musicTherapyViewModel.volumeArray.value[index] = changeValue
                                musicTherapyViewModel.volumeArray.value = floatArray
                            },
                            onValueChangeFinished = {
                                OpenApiSDK.getMusicTherapyApi().setVolume(index, newValue.value)
                            },
                            valueRange = 0f..100f,
                            steps = 100,
                            modifier = Modifier
                                .width(400.dp)
                                .padding(horizontal = 30.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ConstraintLayoutScope.PlayButton() {
        Button(shape = RoundedCornerShape(40.dp),
            modifier = Modifier
                .size(80.dp)
                .constrainAs(createRef()) {
                    bottom.linkTo(parent.bottom, margin = 10.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            onClick = {
                if (musicTherapyViewModel.playState.value == PlayState.MEDIAPLAYER_STATE_STARTED) {
                    OpenApiSDK.getMusicTherapyApi().pause()
                } else if (musicTherapyViewModel.playState.value == PlayState.MEDIAPLAYER_STATE_PAUSED) {
                    OpenApiSDK.getMusicTherapyApi().resume()
                } else {
                    if (musicTherapyViewModel.therapyItem.value == null || musicTherapyViewModel.therapyInfo.value == null) {
                        ToastUtils.showShort("请选择疗愈模式和疗愈场景后才能发起疗愈播放!!!!")
                        return@Button
                    }
                    OpenApiSDK.getMusicTherapyApi().play(
                        musicTherapyViewModel.therapyItem.value!!,
                        MusicTherapyParam(aiMusicTherapyParam = musicTherapyViewModel.aiMusicTherapyParam.value, volumeArray = musicTherapyViewModel.volumeArray.value, playDuration = musicTherapyViewModel.playDuration.value)
                    ) {
                        musicTherapyViewModel.playMedia(it.second, mSurface)
                        if (it.first.isSuccess()) {
                            ToastUtils.showShort("发起疗愈播放成功")
                        } else {
                            ToastUtils.showShort("发起疗愈播放失败，失败原因:${it.first.msg}")
                        }
                    }
                }
            }) {
            Text(text = if (musicTherapyViewModel.playState.value == PlayState.MEDIAPLAYER_STATE_STARTED) "暂停" else if (musicTherapyViewModel.playState.value == PlayState.MEDIAPLAYER_STATE_PAUSED) "继续" else "播放")
        }
    }

    @Composable
    private fun PageModeView() {
        ConstraintLayout {
            val (title, playMode, playScene, playDuration) = createRefs()
            Text(text = "疗愈模式", modifier = Modifier.constrainAs(title) {
                start.linkTo(parent.start, margin = 10.dp)
            })
            LazyRow(modifier = Modifier.constrainAs(playMode) {
                start.linkTo(parent.start, margin = 10.dp)
                top.linkTo(title.bottom, margin = 5.dp)
            }) {
                val therapyInfoList =
                    musicTherapyViewModel.musicTherapyListData.value.therapyInfoList
                this.items(therapyInfoList.size) { index ->
                    val selected =
                        musicTherapyViewModel.therapyInfo.value?.classId == therapyInfoList[index].classId
                    Button(modifier = Modifier.padding(5.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = if (selected) Color.Yellow else MaterialTheme.colors.primary),
                        onClick = {
                            val newValue = therapyInfoList[index]
                            if (newValue.classId != musicTherapyViewModel.therapyInfo.value?.classId) {
                                musicTherapyViewModel.mPlayer?.pause()
                                musicTherapyViewModel.playMedia("", mSurface)
                                OpenApiSDK.getMusicTherapyApi().stop()
                                musicTherapyViewModel.therapyInfo.value = newValue
                                musicTherapyViewModel.therapyItem.value =
                                    therapyInfoList[index].therapyItemList.first()
                                musicTherapyViewModel.updateVolumeArray()
                            }
                        }) {
                        Text(text = therapyInfoList[index].title)
                    }
                }
            }
            LazyRow(modifier = Modifier.constrainAs(playScene) {
                top.linkTo(playMode.bottom)
                start.linkTo(playMode.start)
            }) {
                val therapyInfoList =
                    musicTherapyViewModel.musicTherapyListData.value.therapyInfoList
                val therapyInfo =
                    therapyInfoList.find { it.classId == musicTherapyViewModel.therapyInfo.value?.classId }
                        ?: run {
                            therapyInfoList.firstOrNull()?.apply {
                                musicTherapyViewModel.therapyInfo.value = this
                                musicTherapyViewModel.therapyItem.value =
                                    this.therapyItemList.first()
                            }
                        }
                val playSceneList = therapyInfo?.therapyItemList ?: emptyList()
                this.items(playSceneList.size) { index ->
                    val selected =
                        musicTherapyViewModel.therapyItem.value?.id == playSceneList[index].id
                    Button(modifier = Modifier.padding(10.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = if (selected) Color.Yellow else MaterialTheme.colors.primary),
                        onClick = {
                            val newValue = playSceneList[index]
                            if (musicTherapyViewModel.therapyItem.value?.id != newValue.id) {
                                musicTherapyViewModel.mPlayer?.pause()
                                musicTherapyViewModel.playMedia("", mSurface)
                                OpenApiSDK.getMusicTherapyApi().stop()
                                musicTherapyViewModel.therapyItem.value = newValue
                            }
                        }) {
                        val playSceneWithInIndex = playSceneList[index]
                        val text = if (playSceneWithInIndex.type == 2) {
                            "【AI】${playSceneWithInIndex.title}"
                        } else {
                            playSceneWithInIndex.title
                        }
                        Text(text = text)
                    }
                }
            }

            TuningView("播放时长",
                arrayListOf("无限循环", "1分钟"),
                arrayListOf(if (musicTherapyViewModel.playDuration.value > 0) 1 else 0),
                modifier = Modifier.constrainAs(playDuration) {
                    top.linkTo(playScene.bottom)
                    start.linkTo(parent.start)
                }) {
                if (it == "无限循环") {
                    musicTherapyViewModel.playDuration.value = 0
                } else {
                    musicTherapyViewModel.playDuration.value = 60000
                }
                updateMusicTherapyParam()
            }
        }
    }

    @Composable
    private fun DialogView() {
        if (musicTherapyViewModel.showDialog.value) {
            AlertDialog(
                onDismissRequest = {
                    musicTherapyViewModel.showDialog.value = false
                },
                title = {
                    Text(text = "恭喜你！")
                },
                text = {
                    Text(
                        "棒！获取疗愈试用权益！"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            OpenApiSDK.getMusicTherapyApi().fetchMusicTherapyTrialBenefits {
                                musicTherapyViewModel.showDialog.value = false
                                if (it.isSuccess()) {
                                    ToastUtils.showShort("疗愈权益领取成功！")
                                } else {
                                    ToastUtils.showShort("疗愈权益领取失败，原因：${it.msg}")
                                }
                            }
                        }
                    ) {
                        Text("我要领取")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            musicTherapyViewModel.showDialog.value = false
                        }
                    ) {
                        Text("Dismiss")
                    }
                }
            )
        }
    }

    @Composable
    private fun ConstraintLayoutScope.PageLoadingView() {
        AnimatedVisibility(visible = (musicTherapyViewModel.pageStatus.value == MusicTherapyViewModel.LOADING),
            modifier = Modifier.constrainAs(createRef()) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
            }) {
            CircularProgressIndicator(
                modifier = Modifier.width(64.dp),
                color = MaterialTheme.colors.primary,
                strokeWidth = 4.dp,
            )
        }
        AnimatedVisibility(
            visible = (musicTherapyViewModel.pageStatus.value == MusicTherapyViewModel.ERROR),
            modifier = Modifier.constrainAs(createRef()) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
            },
        ) {
            Button(onClick = {
                musicTherapyViewModel.fetchMusicTherapyConfig()
            }, Modifier.background(Color.Transparent)) {
                Text(
                    text = "加载出错了，请点击重试",
                    color = Color.Red,
                    fontSize = 40.sp
                )
            }
        }
    }

    @Composable
    fun TuningView(
        type: String,
        levelList: ArrayList<String>,
        selectedIndexList: ArrayList<Int>,
        modifier: Modifier,
        onClick: (level: String) -> Unit
    ) {
        Column(modifier = modifier) {
            Text(text = type, modifier = Modifier.padding(start = 10.dp))
            LazyRow {
                this.items(levelList.size) { index ->
                    val selected = selectedIndexList.contains(index)
                    Button(modifier = Modifier.padding(10.dp), colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (selected) Color.Yellow else MaterialTheme.colors.primary
                    ), onClick = {
                        onClick.invoke(levelList[index])
                    }) {
                        Text(text = levelList[index])
                    }
                }
            }
        }
    }

    @Composable
    private fun PageAIContentView() {
        AnimatedVisibility(visible = (musicTherapyViewModel.therapyItem.value?.isAiType() == true)) {
            ConstraintLayout(modifier = Modifier.fillMaxSize()) {
                val (playButton, audioDensity, audioSpatialSense, audioBrightness, element, spatialAudio) = createRefs()
                TuningView("音频密度",
                    arrayListOf("低", "中", "高"),
                    arrayListOf(musicTherapyViewModel.aiMusicTherapyParam.value.audioDensity.ordinal),
                    modifier = Modifier.constrainAs(audioDensity) {
                        top.linkTo(parent.top, margin = 230.dp)
                        start.linkTo(parent.start)
                    }) {
                    val value = if (it == "低") {
                        AudioDensityEnum.LOW
                    } else if (it == "中") {
                        AudioDensityEnum.MIDDLE
                    } else if (it == "高") {
                        AudioDensityEnum.HIGH
                    } else {
                        AudioDensityEnum.LOW
                    }
                    musicTherapyViewModel.aiMusicTherapyParam.value.audioDensity = value
                    updateMusicTherapyParam()
                }
                TuningView("音频空间感",
                    arrayListOf("房间", "演奏厅", "广场"),
                    arrayListOf(musicTherapyViewModel.aiMusicTherapyParam.value.audioSpatialSense.ordinal),
                    modifier = Modifier.constrainAs(audioSpatialSense) {
                        top.linkTo(audioDensity.bottom)
                        start.linkTo(parent.start)
                    }) {
                    val value = if ("房间" == it) {
                        AudioSpatialSenseEnum.ROOM
                    } else if ("演奏厅" == it) {
                        AudioSpatialSenseEnum.CONCERT_HALL
                    } else if ("广场" == it) {
                        AudioSpatialSenseEnum.SQUARE
                    } else {
                        AudioSpatialSenseEnum.CONCERT_HALL
                    }
                    musicTherapyViewModel.aiMusicTherapyParam.value.audioSpatialSense = value
                    updateMusicTherapyParam()
                }
                TuningView("音频明亮度",
                    arrayListOf("厚重", "温和", "明亮"),
                    arrayListOf(musicTherapyViewModel.aiMusicTherapyParam.value.audioBrightness.ordinal),
                    modifier = Modifier.constrainAs(audioBrightness) {
                        top.linkTo(audioSpatialSense.bottom)
                        start.linkTo(parent.start)
                    }) {
                    val value = if ("厚重" == it) {
                        AudioBrightnessEnum.HEAVY
                    } else if ("温和" == it) {
                        AudioBrightnessEnum.MILDNESS
                    } else if ("明亮" == it) {
                        AudioBrightnessEnum.BRIGHTNESS
                    } else {
                        AudioBrightnessEnum.BRIGHTNESS
                    }
                    musicTherapyViewModel.aiMusicTherapyParam.value.audioBrightness = value
                    updateMusicTherapyParam()
                }
                TuningView("疗愈元素",
                    arrayListOf("颂钵", "木鱼", "古琴"),
                    arrayListOf<Int>().apply {
                        addAll(musicTherapyViewModel.aiMusicTherapyParam.value.musicTherapyElementList.map {
                            it.ordinal
                        }.toMutableList())
                    },
                    modifier = Modifier.constrainAs(element) {
                        top.linkTo(audioBrightness.bottom)
                        start.linkTo(parent.start)
                    }) {
                    val value = if (it == "颂钵") {
                        MusicTherapyElementEnum.STANDING_BELL
                    } else if (it == "木鱼") {
                        MusicTherapyElementEnum.WOODEN_FISH
                    } else if (it == "古琴") {
                        MusicTherapyElementEnum.GU_QIN
                    } else {
                        MusicTherapyElementEnum.STANDING_BELL
                    }
                    val musicTherapyElementList: ArrayList<MusicTherapyElementEnum> =
                        ArrayList<MusicTherapyElementEnum>().apply {
                            addAll(musicTherapyViewModel.aiMusicTherapyParam.value.musicTherapyElementList)
                        }
                    if (musicTherapyElementList.contains(value)) {
                        musicTherapyElementList.remove(value)
                    } else {
                        musicTherapyElementList.add(value)
                    }
                    musicTherapyViewModel.aiMusicTherapyParam.value.musicTherapyElementList =
                        musicTherapyElementList
                    updateMusicTherapyParam()

                }

                Button(onClick = {
                    val setValue =
                        !musicTherapyViewModel.aiMusicTherapyParam.value.enableSpatialAudio
                    musicTherapyViewModel.aiMusicTherapyParam.value.enableSpatialAudio =
                        setValue
                    updateMusicTherapyParam()
                }, colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (musicTherapyViewModel.aiMusicTherapyParam.value.enableSpatialAudio) Color.Yellow else MaterialTheme.colors.primary
                ), modifier = Modifier
                    .padding(start = 10.dp)
                    .constrainAs(spatialAudio) {
                        top.linkTo(element.bottom)
                        start.linkTo(parent.start)
                    }) {
                    Text(text = if (musicTherapyViewModel.aiMusicTherapyParam.value.enableSpatialAudio) "关闭空间音频" else "打开空间音频")
                }
            }
        }
    }

    private fun updateMusicTherapyParam() {
        val result = OpenApiSDK.getMusicTherapyApi()
            .updateMusicTherapyParam(MusicTherapyParam(aiMusicTherapyParam = musicTherapyViewModel.aiMusicTherapyParam.value, volumeArray = musicTherapyViewModel.volumeArray.value, playDuration = musicTherapyViewModel.playDuration.value))
        if (result != MusicTherapyStatus.SUCCESS) {
            ToastUtils.showShort(result.msg)
        }
    }

    override fun onPlayStateChange(state: Int) {
        QLog.i(TAG, "onPlayStateChange state = $state")
        musicTherapyViewModel.playState.value = state
        if (state == PlayState.MEDIAPLAYER_STATE_STARTED) {
            musicTherapyViewModel.enterInMusicTherapy.value = true
        }

        if (!musicTherapyViewModel.isPrepared.get()) {
            QLog.i(TAG, "onPlayStateChange video is not isPrepared")
            return
        }

        if (PlayStateProxyHelper.isPlaying(state) && musicTherapyViewModel.mPlayer?.isPlaying() == false) {
            QLog.i(TAG, "onPlayStateChange video play")
            musicTherapyViewModel.mPlayer?.play()
        } else if (!PlayStateProxyHelper.isPlaying(state) && musicTherapyViewModel.mPlayer?.isPlaying() == true) {
            QLog.i(TAG, "onPlayStateChange video pause")
            musicTherapyViewModel.mPlayer?.pause()
        }
    }

    override fun onPlayParamChange(musicTherapyParam: MusicTherapyParam) {
        QLog.i(TAG, "onPlayParamChange musicTherapyParam = $musicTherapyParam")
        musicTherapyViewModel.aiMusicTherapyParam.value = musicTherapyParam.aiMusicTherapyParam
        musicTherapyViewModel.playDuration.value = musicTherapyParam.playDuration
        musicTherapyViewModel.volumeArray.value = musicTherapyParam.volumeArray
    }


}