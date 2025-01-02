package com.tencent.qqmusic.qplayer.ui.activity.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.openapisdk.model.SoundEffectItem
import com.tencent.qqmusic.openapisdk.model.VipType
import com.tencent.qqmusic.openapisdk.model.soundeffect.SoundEffect3DParam
import com.tencent.qqmusic.openapisdk.model.soundeffect.SoundEffect51Param
import com.tencent.qqmusic.openapisdk.model.soundeffect.SoundEffectSuperBassParam
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.main.TopBar
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Created by tannyli on 2022/11/24.
 * Copyright (c) 2022 TME. All rights reserved.
 */
class SoundEffectActivity : ComponentActivity() {

    private val vm: SoundEffectViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SoundEffectScreen(flow = vm.pagingEffectList())
        }
    }

    override fun onBackPressed() {
        if (vm.currentSelectSoundEffectItem.value == null) {
            super.onBackPressed()
        } else {
            vm.currentSelectSoundEffectItem.value = null
        }
    }

    @Composable
    fun SoundEffectScreen(flow: Flow<PagingData<SoundEffectItem>>) {
        Scaffold(topBar = { TopBar(vm.currentSelectSoundEffectItem.value?.name ?: "音效设置") }) {

            AnimatedVisibility(visible = (vm.currentSelectSoundEffectItem.value?.canSetSoundEffectParam() != true )) {
                soundEffectList(flow)
            }
            AnimatedVisibility(visible = (vm.currentSelectSoundEffectItem.value?.is3DEffect() == true)) {
                adjust3DParam()
            }
            AnimatedVisibility(visible = (vm.currentSelectSoundEffectItem.value?.isSuperBass() == true)) {
                adjustSuperBassParam()
            }
            AnimatedVisibility(visible = (vm.currentSelectSoundEffectItem.value?.is51Surround() == true)) {
                adjust51Param()
            }
        }
    }

    @Composable
    fun adjust51Param() {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Button(modifier = Modifier.padding(10.dp), onClick = {
                vm.currentSelectSoundEffectItem.value?.let {
                    OpenApiSDK.getSoundEffectApi().resetSoundEffectParam(it)
                }
                vm.soundEffect51Param.value = SoundEffect51Param.DEFAULT
            }) {
                Text(text = "重置")
            }
            TuningView(
                "环绕空间调节",
                arrayListOf("小", "中", "大"),
                arrayListOf(vm.soundEffect51Param.value.distance.ordinal),
                modifier = Modifier.padding(0.dp)) {
                val copy = vm.soundEffect51Param.value.copy()
                copy.distance = it
                vm.soundEffect51Param.value = copy
                vm.currentSelectSoundEffectItem.value?.let {
                    OpenApiSDK.getSoundEffectApi().setSoundEffectParam(it, vm.soundEffect51Param.value)
                }
            }
            Text("前置音箱-左-音量")
            Slider(
                value = vm.soundEffect51Param.value.leftFrontWeight.toFloat(),
                valueRange = 0f..100f,
                onValueChange = { newValue ->
                    val copy = vm.soundEffect51Param.value.copy()
                    copy.leftFrontWeight = newValue.roundToInt()
                    vm.soundEffect51Param.value =copy
                },
                onValueChangeFinished = {
                    vm.currentSelectSoundEffectItem.value?.let {
                        OpenApiSDK.getSoundEffectApi().setSoundEffectParam(it, vm.soundEffect51Param.value)
                    }
                },
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Text("中央音箱-音量")
            Slider(
                value = vm.soundEffect51Param.value.centerWeight.toFloat(),
                valueRange = 0f..100f,
                onValueChange = { newValue ->
                    val copy = vm.soundEffect51Param.value.copy()
                    copy.centerWeight = newValue.roundToInt()
                    vm.soundEffect51Param.value =copy
                },
                onValueChangeFinished = {
                    vm.currentSelectSoundEffectItem.value?.let {
                        OpenApiSDK.getSoundEffectApi().setSoundEffectParam(it, vm.soundEffect51Param.value)
                    }
                },
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Text("前置音箱-右-音量")
            Slider(
                value = vm.soundEffect51Param.value.rightFrontWeight.toFloat(),
                valueRange = 0f..100f,
                onValueChange = { newValue ->
                    val copy = vm.soundEffect51Param.value.copy()
                    copy.rightFrontWeight = newValue.roundToInt()
                    vm.soundEffect51Param.value =copy
                },
                onValueChangeFinished = {
                    vm.currentSelectSoundEffectItem.value?.let {
                        OpenApiSDK.getSoundEffectApi().setSoundEffectParam(it, vm.soundEffect51Param.value)
                    }
                },
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Text("重低音音箱-音量")
            Slider(
                value = vm.soundEffect51Param.value.bassWeight.toFloat(),
                valueRange = 0f..100f,
                onValueChange = { newValue ->
                    val copy = vm.soundEffect51Param.value.copy()
                    copy.bassWeight = newValue.roundToInt()
                    vm.soundEffect51Param.value =copy
                },
                onValueChangeFinished = {
                    vm.currentSelectSoundEffectItem.value?.let {
                        OpenApiSDK.getSoundEffectApi().setSoundEffectParam(it, vm.soundEffect51Param.value)
                    }
                },
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Text("后置音箱-左-音量")
            Slider(
                value = vm.soundEffect51Param.value.leftRearWeight.toFloat(),
                valueRange = 0f..100f,
                onValueChange = { newValue ->
                    val copy = vm.soundEffect51Param.value.copy()
                    copy.leftRearWeight = newValue.roundToInt()
                    vm.soundEffect51Param.value =copy
                },
                onValueChangeFinished = {
                    vm.currentSelectSoundEffectItem.value?.let {
                        OpenApiSDK.getSoundEffectApi().setSoundEffectParam(it, vm.soundEffect51Param.value)
                    }
                },
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Text("后置音箱-右-音量")
            Slider(
                value = vm.soundEffect51Param.value.rightRearWeight.toFloat(),
                valueRange = 0f..100f,
                onValueChange = { newValue ->
                    val copy = vm.soundEffect51Param.value.copy()
                    copy.rightRearWeight = newValue.roundToInt()
                    vm.soundEffect51Param.value =copy
                },
                onValueChangeFinished = {
                    vm.currentSelectSoundEffectItem.value?.let {
                        OpenApiSDK.getSoundEffectApi().setSoundEffectParam(it, vm.soundEffect51Param.value)
                    }
                },
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            TuningView(
                "前置音箱-角度",
                arrayListOf("小", "中", "大"),
                arrayListOf(vm.soundEffect51Param.value.frontAngle.ordinal),
                modifier = Modifier.padding(0.dp)) {
                val copy = vm.soundEffect51Param.value.copy()
                copy.frontAngle = it
                vm.soundEffect51Param.value = copy
                vm.currentSelectSoundEffectItem.value?.let {
                    OpenApiSDK.getSoundEffectApi().setSoundEffectParam(it, vm.soundEffect51Param.value)
                }
            }
            TuningView(
                "后置音箱-角度",
                arrayListOf("小", "中", "大"),
                arrayListOf(vm.soundEffect51Param.value.rearAngle.ordinal),
                modifier = Modifier.padding(0.dp)) {
                val copy = vm.soundEffect51Param.value.copy()
                copy.rearAngle = it
                vm.soundEffect51Param.value = copy
                vm.currentSelectSoundEffectItem.value?.let {
                    OpenApiSDK.getSoundEffectApi().setSoundEffectParam(it, vm.soundEffect51Param.value)
                }
            }
        }
    }

    private fun transformToLevel(level: String): SoundEffect51Param.Level {
        return if (level == "小") {
            SoundEffect51Param.Level.MIN
        } else if (level == "大") {
            SoundEffect51Param.Level.MAX
        } else {
            SoundEffect51Param.Level.MIDDLE
        }
    }

    @Composable
    fun TuningView(
        type: String,
        levelList: ArrayList<String>,
        selectedIndexList: ArrayList<Int>,
        modifier: Modifier = Modifier.padding(10.dp),
        onClick: (level: SoundEffect51Param.Level) -> Unit
    ) {
        Column(modifier = modifier) {
            Text(text = type, modifier = Modifier.padding(start = 10.dp))
            LazyRow {
                this.items(levelList.size) { index ->
                    val selected = selectedIndexList.contains(index)
                    Button(modifier = Modifier.padding(10.dp), colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (selected) Color.Yellow else MaterialTheme.colors.primary
                    ), onClick = {
                        onClick.invoke(transformToLevel(levelList[index]))
                    }) {
                        Text(text = levelList[index])
                    }
                }
            }
        }
    }

    @Composable
    fun adjustSuperBassParam() {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Button(modifier = Modifier.padding(10.dp), onClick = {
                vm.currentSelectSoundEffectItem.value?.let {
                    OpenApiSDK.getSoundEffectApi().resetSoundEffectParam(it)
                }
                vm.soundEffectSuperBassParam.value = SoundEffectSuperBassParam.NATURAL_IMMERSION
            }) {
                Text(text = "重置")
            }
            ClickableText(
                text = AnnotatedString("自然沉浸"),
                modifier = Modifier.padding(80.dp, 20.dp),
                style = TextStyle.Default.copy(color = Color.Green),
                onClick = {
                    vm.soundEffectSuperBassParam.value = SoundEffectSuperBassParam.NATURAL_IMMERSION
                    vm.currentSelectSoundEffectItem.value?.let {
                        OpenApiSDK.getSoundEffectApi().setSoundEffectParam(it,
                            SoundEffectSuperBassParam.NATURAL_IMMERSION)
                    }
                }
            )
            ClickableText(
                text = AnnotatedString("超低震撼"),
                modifier = Modifier.padding(80.dp, 20.dp),
                style = TextStyle.Default.copy(color = Color.Green),
                onClick = {
                    vm.soundEffectSuperBassParam.value = SoundEffectSuperBassParam.ULTRA_LOW_SHOCK
                    vm.currentSelectSoundEffectItem.value?.let {
                        OpenApiSDK.getSoundEffectApi().setSoundEffectParam(it,
                            SoundEffectSuperBassParam.ULTRA_LOW_SHOCK)
                    }
                }
            )
            ClickableText(
                text = AnnotatedString("虚拟低音炮"),
                modifier = Modifier.padding(80.dp, 20.dp),
                style = TextStyle.Default.copy(color = Color.Green),
                onClick = {
                    vm.soundEffectSuperBassParam.value = SoundEffectSuperBassParam.VIRTUAL_SUBWOOFER
                    vm.currentSelectSoundEffectItem.value?.let {
                        OpenApiSDK.getSoundEffectApi().setSoundEffectParam(it,
                            SoundEffectSuperBassParam.VIRTUAL_SUBWOOFER)
                    }
                }
            )
            Text("强度")
            Slider(
                value = vm.soundEffectSuperBassParam.value.gain.toFloat(),
                valueRange = 0f..100f,
                onValueChange = { newValue ->
                    vm.soundEffectSuperBassParam.value = SoundEffectSuperBassParam(newValue.roundToInt(), vm.soundEffectSuperBassParam.value.freqCut)
                },
                onValueChangeFinished = {
                    vm.currentSelectSoundEffectItem.value?.let {
                        OpenApiSDK.getSoundEffectApi().setSoundEffectParam(it, vm.soundEffectSuperBassParam.value)
                    }
                },
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Text("深度")
            Slider(
                value = vm.soundEffectSuperBassParam.value.freqCut.toFloat(),
                valueRange = 0f..100f,
                onValueChange = { newValue ->
                    vm.soundEffectSuperBassParam.value = SoundEffectSuperBassParam(vm.soundEffectSuperBassParam.value.gain, newValue.roundToInt())
                },
                onValueChangeFinished = {
                    vm.currentSelectSoundEffectItem.value?.let {
                        OpenApiSDK.getSoundEffectApi().setSoundEffectParam(it, vm.soundEffectSuperBassParam.value)
                    }
                },
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
    }

    @Composable
    fun adjust3DParam() {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Button(modifier = Modifier.padding(10.dp), onClick = {
                vm.currentSelectSoundEffectItem.value?.let {
                    OpenApiSDK.getSoundEffectApi().resetSoundEffectParam(it)
                }
                vm.soundEffect3DParam.value = SoundEffect3DParam.DEFAULT
            }) {
                Text(text = "重置")
            }
            Text("音源远近")
            Slider(
                value = vm.soundEffect3DParam.value.distance.toFloat(),
                valueRange = 0f..100f,
                onValueChange = { newValue ->
                    val copy = SoundEffect3DParam(newValue.roundToInt(), vm.soundEffect3DParam.value.speed)
                    vm.soundEffect3DParam.value = copy
                },
                onValueChangeFinished = {
                    vm.currentSelectSoundEffectItem.value?.let {
                        OpenApiSDK.getSoundEffectApi().setSoundEffectParam(it, vm.soundEffect3DParam.value)
                    }
                },
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Text("环绕速度")
            Slider(
                value = vm.soundEffect3DParam.value.speed.toFloat(),
                valueRange = 0f..100f,
                onValueChange = { newValue ->
                    val copy = SoundEffect3DParam(vm.soundEffect3DParam.value.distance, newValue.roundToInt())
                    vm.soundEffect3DParam.value = copy
                },
                onValueChangeFinished = {
                    vm.currentSelectSoundEffectItem.value?.let {
                        OpenApiSDK.getSoundEffectApi().setSoundEffectParam(it, vm.soundEffect3DParam.value)
                    }
                },
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
    }

    @Composable
    private fun soundEffectList(flow: Flow<PagingData<SoundEffectItem>>) {
        val effects = flow.collectAsLazyPagingItems()
        val curId = remember { mutableStateOf(vm.currentEffect()?.sdkId ?: -1) }
        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
            this.items(effects) { effect ->
                effect ?: return@items
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable {
                        if (effect.sdkId == curId.value) {
                            OpenApiSDK
                                .getPlayerApi()
                                .setSoundEffectType(null)
                            UiUtils.showToast("取消音效->${effect.name}")
                            curId.value = -1
                            return@clickable
                        }
                        val ret = OpenApiSDK
                            .getPlayerApi()
                            .setSoundEffectType(effect) { status ->
                                vm.viewModelScope.launch {
                                    withContext(Dispatchers.Main) {
                                        when (status) {
                                            1 -> UiUtils.showToast("设置音效->开始下载音效")
                                            2 -> UiUtils.showToast("设置音效->下载完成")
                                            3 -> UiUtils.showToast("设置音效->下载失败")
                                            4 -> UiUtils.showToast("设置音效->已在下载中")
                                        }
                                    }
                                }
                                true
                            }
                        if (ret == 0) {
                            UiUtils.showToast("设置音效->${effect.name}")
                            curId.value = effect.sdkId
                        }
                        else if (ret == PlayDefine.PlayError.PLAY_ERR_UNSUPPORT_WANOS) {
                            UiUtils.showToast("为保证您的听歌体验，WANOS歌曲播放中不建议叠加其他音效等效果")
                        }
                        else if (ret == PlayDefine.PlayError.PLAY_ERR_UNSUPPORT_DOLBY) {
                            UiUtils.showToast("为保证您的听歌体验，杜比音质播放中不建议叠加其他音效等效果")
                        }
                        else if (ret == PlayDefine.PlayError.PLAY_ERR_UNSUPPORT_EXCELLENT) {
                            UiUtils.showToast("为保证您的听歌体验，臻品2.0播放中不建议叠加其他音效等效果")
                        }
                        else if (ret == PlayDefine.PlayError.PLAY_ERR_UNSUPPORT_GALAXY) {
                            UiUtils.showToast("为保证您的听歌体验，臻品全景声音质播放中不建议叠加其他音效等效果")
                        }
                        else if (ret == PlayDefine.PlayError.PLAY_ERR_UNSUPPORT_MASTER_TAPE) {
                            UiUtils.showToast("为保证您的听歌体验，臻品母带音质播放中不建议叠加其他音效等效果")
                        }
                        else if (ret == PlayDefine.PlayError.PLAY_ERR_NEED_VIP){
                            UiUtils.showToast("需要vip")
                        } else if (ret == PlayDefine.PlayError.PLAY_ERR_NEED_SUPER_VIP) {
                            UiUtils.showToast("需要超级会员")
                        } else {
                            UiUtils.showToast("ret:$ret")
                        }
                    }) {
                    Row(modifier = Modifier.weight(1f)) {
                        Text(text = effect.name)
                        if (effect.isCustomEffect()) {
                            Text(
                                text = " 定制音效",
                                modifier = Modifier.padding(5.dp, 0.dp)
                            )
                            if (effect.vipFlag == 2) {
                                Text(
                                    text = " 超级会员",
                                    modifier = Modifier.padding(5.dp, 0.dp),
                                    color = Color.Yellow,
                                )
                            } else if (effect.vipFlag == 1) {
                                Text(
                                    text = " 豪华绿钻",
                                    modifier = Modifier.padding(5.dp, 0.dp),
                                    color = Color.Green,
                                )
                            }
                        } else if (effect.vipFlag == 1) {
                            val vipType = OpenApiSDK.getPlayerApi().getSoundEffectVipType()
                            val soundEffectVipText = when (vipType) {
                                VipType.SUPER_VIP -> {
                                    " 超级会员"
                                }
                                VipType.GREEN_VIP -> {
                                    " 豪华绿钻"
                                }
                                else -> {
                                    ""
                                }
                            }
                            Text(
                                text = soundEffectVipText,
                                modifier = Modifier.padding(5.dp, 0.dp),
                                color = Color.Green,
                            )
                        }
                        if (effect.canSetSoundEffectParam()) {
                            ClickableText(
                                text = AnnotatedString("调整参数"),
                                modifier = Modifier.padding(80.dp, 0.dp),
                                style = TextStyle.Default.copy(color = Color.Red),
                                onClick = {
                                    if (effect.is3DEffect()) {
                                        vm.soundEffect3DParam.value = (OpenApiSDK.getSoundEffectApi().getSoundEffectParam(effect) as? SoundEffect3DParam) ?: SoundEffect3DParam.DEFAULT
                                        vm.currentSelectSoundEffectItem.value = effect
                                    } else if (effect.is51Surround()) {
                                        vm.soundEffect51Param.value = (OpenApiSDK.getSoundEffectApi().getSoundEffectParam(effect) as? SoundEffect51Param) ?: SoundEffect51Param.DEFAULT
                                        vm.currentSelectSoundEffectItem.value = effect
                                    } else if (effect.isSuperBass()) {
                                        vm.soundEffectSuperBassParam.value = (OpenApiSDK.getSoundEffectApi().getSoundEffectParam(effect) as? SoundEffectSuperBassParam) ?: SoundEffectSuperBassParam.NATURAL_IMMERSION
                                        vm.currentSelectSoundEffectItem.value = effect
                                    }
                                }
                            )
                        }
                    }
                    if (curId.value == effect.sdkId) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_cheked),
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                        )
                    }

                }
            }
        }
    }

    class SoundEffectViewModel: ViewModel() {
        companion object {
            private const val TAG = "MusicTherapyViewModel"
            private const val MV_VID = "MV_VID"
            const val NORMAL = 0
        }
        val soundEffect3DParam = mutableStateOf(SoundEffect3DParam.DEFAULT)
        val soundEffectSuperBassParam = mutableStateOf(SoundEffectSuperBassParam.NATURAL_IMMERSION)
        val soundEffect51Param = mutableStateOf(SoundEffect51Param.DEFAULT)
        val currentSelectSoundEffectItem = mutableStateOf<SoundEffectItem?>(null)
        fun pagingEffectList() =  Pager(PagingConfig(50)) {
            SoundEffectPagingSource()
        }.flow

        fun currentEffect() = OpenApiSDK.getPlayerApi().getCurSoundEffect()
    }

    class SoundEffectPagingSource: PagingSource<Int, SoundEffectItem>() {
        override fun getRefreshKey(state: PagingState<Int, SoundEffectItem>): Int? {
            return state.anchorPosition
        }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SoundEffectItem> {
            return try {
                withContext(Dispatchers.IO) {
                    val dataList = OpenApiSDK.getOpenApi().blockingGet<List<SoundEffectItem>> {
                        this.fetchSoundEffectConfig(it)
                    }.data ?: emptyList()
                    val prevKey = null
                    val nextKey = null
                    LoadResult.Page<Int, SoundEffectItem>(
                        data = dataList,
                        prevKey = prevKey,
                        nextKey = nextKey
                    )
                }
            } catch (e: Exception) {
                return LoadResult.Error(e)
            }
        }

    }

}