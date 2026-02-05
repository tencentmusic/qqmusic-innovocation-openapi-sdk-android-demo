package com.tencent.qqmusic.qplayer.ui.activity.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodecList
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import com.tencent.qqmusic.innovation.common.util.Global
import com.tencent.qqmusic.innovation.common.util.GsonHelper
import com.tencent.qqmusic.innovation.common.util.ToastUtils
import com.tencent.qqmusic.innovation.common.util.UtilContext
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.network.NetworkTimeoutConfig
import com.tencent.qqmusic.openapisdk.core.player.transition.PlayerTransition
import com.tencent.qqmusic.playerinsight.util.coverErrorCode
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.qplayer.core.player.proxy.SPBridgeProxy
import com.tencent.qqmusic.qplayer.report.report.LaunchReport
import com.tencent.qqmusic.qplayer.ui.activity.OpenApiDemoActivity
import com.tencent.qqmusic.qplayer.ui.activity.SongCacheDemoActivity
import com.tencent.qqmusic.qplayer.ui.activity.login.WebViewActivity
import com.tencent.qqmusic.qplayer.ui.activity.mv.MvBuyQRDialog
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerActivity
import com.tencent.qqmusic.qplayer.utils.SettingsUtil
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.internal.toLongOrDefault
import java.text.SimpleDateFormat
import java.util.Locale

class OtherActivity : ComponentActivity() {

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Scaffold(topBar = { TopBar("其他设置页") },
                modifier = Modifier.semantics{ testTagsAsResourceId=true }) {
                OtherScreen()
            }
        }
    }
}

@Preview
@Composable
fun OtherScreen() {
    val activity = LocalContext.current as Activity
    val sharedPreferences: SharedPreferences? = try {
        SPBridgeProxy.getSharedPreferences("OpenApiSDKEnv", Context.MODE_PRIVATE)
    } catch (e: Exception) {
        QLog.e("OtherScreen", "getSharedPreferences error e = ${e.message}")
        null
    }
    val isUseNewPlayerPage = remember { mutableStateOf(UiUtils.getUseNewPlayPageValue()) }

    val padding = 5.dp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SingleItem(title = "是否启用新版本播放页面", item = if (isUseNewPlayerPage.value) "新版播放页面" else "旧版播放页面") {
            val isNewPlayerPage: Boolean = UiUtils.getUseNewPlayPageValue()
            sharedPreferences?.edit { putBoolean("newPlayerPage", isNewPlayerPage.not()) }
            isUseNewPlayerPage.value = isNewPlayerPage.not()
        }

        SingleItem(title = "OpenApi测试界面", item = "") {
            activity.startActivity(Intent(activity, OpenApiDemoActivity::class.java))
        }

        SingleItem(title = "缓存测试界面", item = "") {
            activity.startActivity(Intent(activity, SongCacheDemoActivity::class.java))
        }

        SingleItem(title = "播放测试界面", item = "") {
            activity.startActivity(Intent(activity, PlayerActivity::class.java))
        }

        SingleItem(title = "试用/限免相关", item = "") {
            activity.startActivity(Intent(activity, FreeLimitedTimeActivity::class.java))
        }
        SingleItem(
            title = "是否支持杜比",
            item = if (OpenApiSDK.getPlayerApi().supportDolbyDecoder()) "支持"
            else "不支持"
        ) {
            val minBufSize = AudioTrack.getMinBufferSize(
                48000,
                1020,
                AudioFormat.ENCODING_PCM_16BIT
            )
            QLog.d("OtherScreen", "minBufSize：$minBufSize")
            // Check whether there is an in-device Dolby AC-4 IMS decoder.z
            val mediaCodecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            val mediaCodecInfos = mediaCodecList.codecInfos
            mediaCodecInfos?.forEach {
                QLog.d("OtherScreen", "codeInfo:${it.name}, isEncoder:${it.isEncoder} support:${it.supportedTypes?.contentToString()}")
            }
            if (OpenApiSDK.getPlayerApi().supportDolbyDecoder()) {
                Toast.makeText(UtilContext.getApp(), "支持杜比", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(UtilContext.getApp(), "不支持杜比", Toast.LENGTH_SHORT).show()
            }
        }

        val state: MutableState<Boolean> = remember {
            mutableStateOf(sharedPreferences?.getBoolean("restore_play_list",  true) != false)
        }
        val isAutoNext: MutableState<Boolean> = remember {
            mutableStateOf(sharedPreferences?.getBoolean("restore_next_song",  true) != false)
        }
        val launchAutoPlay: MutableState<Boolean> = remember {
            mutableStateOf(sharedPreferences?.getBoolean("launch_auto_play",  true) != false)
        }


        val isUseForegroundService: MutableState<Boolean> = remember {
            mutableStateOf(sharedPreferences?.getBoolean("isUseForegroundService",  true) != false)
        }

        SingleItem(title = "播放列表重启自动恢复功能", item = if (state.value) "开启" else "关闭") {
            val status = sharedPreferences?.getBoolean("restore_play_list",  true) != false
            sharedPreferences?.edit { putBoolean("restore_play_list", status.not()) }
            Toast.makeText(activity, "重启app生效", Toast.LENGTH_SHORT).show()
            state.value = status.not()
        }

        if (state.value) {
            SingleItem(title = "启动后自动播放音乐", item = if (launchAutoPlay.value) "开启" else "关闭") {
                val status = sharedPreferences?.getBoolean("launch_auto_play",  true) != false
                sharedPreferences?.edit { putBoolean("launch_auto_play", status.not()) }
                Toast.makeText(activity, "重启app生效", Toast.LENGTH_SHORT).show()
                launchAutoPlay.value = status.not()
            }
        }


        SingleItem(title = "正常播放情况下是否播放下一曲", item = if (isAutoNext.value) "开启" else "关闭") {
            val status = sharedPreferences?.getBoolean("restore_next_song",  true) != false
            sharedPreferences?.edit { putBoolean("restore_next_song", status.not()) }
            Toast.makeText(activity, "重启app生效", Toast.LENGTH_SHORT).show()
            isAutoNext.value = status.not()
        }

        var number: Int by remember {
            mutableIntStateOf(sharedPreferences?.getInt("restore_play_list_err_num", 0) ?: 0)
        }

        var autoPlayErrNum: String by remember {
            mutableStateOf(number.toString())
        }

        var showDialog by remember { mutableStateOf(false) }
        if (showDialog) {
            Dialog(onDismissRequest = { showDialog = false }) {
                // 对话框内容
                Column(modifier = Modifier.padding(16.dp)) {
                    TextField(
                        value = autoPlayErrNum,
                        onValueChange = { newText -> autoPlayErrNum = newText },
                        label = {
                            Text(
                                text = "请输入重试次数或自定义值",
                                style = MaterialTheme.typography.body1.merge()
                            )
                        },
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = Color.White,
                            textColor = Color.Black,
                            cursorColor = Color.Black
                        ),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
                    )
                    Button(onClick = {
                        // 点击确认按钮时的处理逻辑
                        // 这里可以处理 text 变量，例如发送数据、更新UI等
                        if (autoPlayErrNum.toIntOrNull() == null) {
                            Toast.makeText(activity, "请输入数字", Toast.LENGTH_SHORT).show()
                        } else {
                            showDialog = false // 关闭对话框
                            sharedPreferences?.edit { putInt("restore_play_list_err_num", autoPlayErrNum.toInt()) }
                            Toast.makeText(activity, "已设置为：${autoPlayErrNum},请重启应用", Toast.LENGTH_SHORT).show()
                            number = autoPlayErrNum.toInt()
                        }
                    }) {
                        Text("ok")
                    }
                }
            }

        }

        var showSelectDialog by remember { mutableStateOf(false) }
        if (showSelectDialog) {
            MyMultiSelectDialog(
                options = listOf("列表重试", "不重试", "重试n次", "自定义"),
                onOptionsSelected = { options ->
                    showSelectDialog = false
                    if (options == "重试n次" || options == "自定义") {
                        showDialog = true
                    } else {
                        number = if (options == "列表重试") 0 else -1
                        sharedPreferences?.edit { putInt("restore_play_list_err_num", number) }
                        Toast.makeText(activity, "已设置为：${number},请重启应用", Toast.LENGTH_SHORT).show()
                    }
                },
                onDismissRequest = { showSelectDialog = false }
            )
        }

        SingleItem(
            title = "播放错误自动重试配置", item = when {
                number < 0 -> "当前值:${number}->不重试"
                number > 0 -> "当前值:${number}->重试${number}次"
                else -> "当前值:0->列表重试"
            }
        ) {
            showSelectDialog = true
        }


        SingleItem(title = "前台服务功能", item = if (isUseForegroundService.value) "开启" else "关闭") {
            val nextBool = sharedPreferences?.getBoolean("isUseForegroundService",  true) != false
            sharedPreferences?.edit { putBoolean("isUseForegroundService", nextBool.not()) }
            Toast.makeText(activity, "请重启应用", Toast.LENGTH_SHORT).show()
            isUseForegroundService.value = nextBool.not()
        }

        val playWhenRequestFocusFailed: MutableState<Boolean> = remember {
            mutableStateOf(sharedPreferences?.getBoolean("playWhenRequestFocusFailed",  true) != false)
        }

        SingleItem(title = "申请焦点失败继续播放功能", item = if (playWhenRequestFocusFailed.value) "开启" else "关闭") {
            val nextBool = sharedPreferences?.getBoolean("playWhenRequestFocusFailed",  true) != false
            sharedPreferences?.edit { putBoolean("playWhenRequestFocusFailed", nextBool.not()) }
            Toast.makeText(activity, "请重启应用", Toast.LENGTH_SHORT).show()
            playWhenRequestFocusFailed.value = nextBool.not()
        }

        val playHigherQualityCache: MutableState<Boolean> = remember {
            mutableStateOf(sharedPreferences?.getBoolean("playHigherQualityCache",  true) != false)
        }

        SingleItem(title = "优先播放高音质完整缓存", item = if (playHigherQualityCache.value) "开启" else "关闭") {
            val nextBool = sharedPreferences?.getBoolean("playHigherQualityCache",  true) != false
            sharedPreferences?.edit { putBoolean("playHigherQualityCache", nextBool.not()) }
            Toast.makeText(activity, "请重启应用", Toast.LENGTH_SHORT).show()
            playHigherQualityCache.value = nextBool.not()
        }

        val filterAddToNextOneSong: MutableState<Boolean> = remember {
            mutableStateOf(sharedPreferences?.getBoolean("filterAddToNextOneSong", false) ?: false)
        }
        SingleItem(title = "addToNext过滤相同歌曲", item = if (filterAddToNextOneSong.value) "开启" else "关闭") {
            val nextBool = sharedPreferences?.getBoolean("filterAddToNextOneSong", false) == true
            sharedPreferences?.edit { putBoolean("filterAddToNextOneSong", nextBool.not()) }
            Toast.makeText(activity, "请重启应用", Toast.LENGTH_SHORT).show()
            filterAddToNextOneSong.value = nextBool.not()
        }

        val needFadeWhenPlay: MutableState<Boolean> = remember {
            mutableStateOf(sharedPreferences?.getBoolean("needFadeWhenPlay",  true) != false)
        }
        SingleItem(title = "开启淡入淡出", item = if (needFadeWhenPlay.value) "开启" else "关闭") {
            val nextBool = needFadeWhenPlay.value.not()
            sharedPreferences?.edit { putBoolean("needFadeWhenPlay", nextBool) }
            Toast.makeText(activity, "设置成功，重启生效", Toast.LENGTH_SHORT).show()
            needFadeWhenPlay.value = nextBool
        }

        val showTimeoutDialog = remember { mutableStateOf(false) }
        SingleItem(title = "设置接口超时时间", item = "") {
            showTimeoutDialog.value = true
        }
        if (showTimeoutDialog.value) {
            sharedPreferences?.let {
                NetworkTimeoutDialog(activity, it, showTimeoutDialog)
            }
        }

        val enableMediaButton: MutableState<Boolean> = remember {
            mutableStateOf(sharedPreferences?.getBoolean("enableMediaButton",  true) != false)
        }
        SingleItem(title = "是否处理MediaButton事件", item = if (enableMediaButton.value) "开启" else "关闭") {
            val nextValue = enableMediaButton.value.not()
            sharedPreferences?.edit { putBoolean("enableMediaButton", nextValue) }
            enableMediaButton.value = nextValue
            OpenApiSDK.getPlayerApi().setEnableMediaButton(enableMediaButton.value)
            Toast.makeText(activity, "设置成功，立即生效", Toast.LENGTH_SHORT).show()
        }

        val enableAccountPartner: MutableState<Boolean> = remember {
            mutableStateOf(sharedPreferences?.getBoolean("accountModePartner", false) == true)
        }
        SingleItem(title = "是否使用三方账号独立登录模式(TV暗账号)", item = if (enableAccountPartner.value) "开启" else "关闭") {
            val nextValue = enableAccountPartner.value.not()
            sharedPreferences?.edit { putBoolean("accountModePartner", nextValue) }
            enableAccountPartner.value = nextValue
            Toast.makeText(activity, "设置成功，重启生效", Toast.LENGTH_SHORT).show()
        }

        val enableMemoryMode: MutableState<Boolean> = remember {
            mutableStateOf(sharedPreferences?.getBoolean("lowMemoryMode", false) == true)
        }
        SingleItem(title = "是否使用低内存模式(部分音质屏蔽)", item = if (enableMemoryMode.value) "开启" else "关闭") {
            val nextValue = enableMemoryMode.value.not()
            sharedPreferences?.edit { putBoolean("lowMemoryMode", nextValue) }
            enableMemoryMode.value = nextValue
            Toast.makeText(activity, "设置成功，重启生效", Toast.LENGTH_SHORT).show()
        }

        val useMediaPlayer: MutableState<Boolean> = remember {
            mutableStateOf(sharedPreferences?.getBoolean("useMediaPlayerWhenPlayDolby", false) == true)
        }
        SingleItem(title = "是否用系统播放器来播放杜比", item = if (useMediaPlayer.value) "MediaPlayer" else "MediaCodec") {
            val nextValue = useMediaPlayer.value.not()
            sharedPreferences?.edit { putBoolean("useMediaPlayerWhenPlayDolby", nextValue) }
            useMediaPlayer.value = nextValue
            Toast.makeText(activity, "设置成功，重启生效", Toast.LENGTH_SHORT).show()
        }
        val delayGetAudioFocus: MutableState<Boolean> = remember {
            mutableStateOf(sharedPreferences?.getBoolean("delayGetAudioFocus", false) == true)
        }
        SingleItem(title = "是否延迟获取焦点", item = if (delayGetAudioFocus.value) "开启" else "关闭") {
            val nextValue = delayGetAudioFocus.value.not()
            sharedPreferences?.edit { putBoolean("delayGetAudioFocus", nextValue) }
            delayGetAudioFocus.value = nextValue
            Toast.makeText(activity, "设置成功，立刻生效，效果需查看日志", Toast.LENGTH_SHORT).show()
        }

        val useCustomNetworkCheck: MutableState<Boolean> = remember {
            mutableStateOf(sharedPreferences?.getBoolean("useCustomNetworkCheck", false) == true)
        }
        SingleItem(title = "是否自定义判断网络连接状态", item = if(useCustomNetworkCheck.value) "打开" else "关闭") {
            val nextValue = useCustomNetworkCheck.value.not()
            sharedPreferences?.edit { putBoolean("useCustomNetworkCheck", nextValue) }
            useCustomNetworkCheck.value = nextValue
            Toast.makeText(activity, "设置成功，重启生效", Toast.LENGTH_SHORT).show()
        }
        if (useCustomNetworkCheck.value) {
            val networkAvailable: MutableState<Boolean> = remember {
                mutableStateOf(sharedPreferences?.getBoolean("networkAvailable",  true) != false)
            }
            SingleItem(title = "网络是否可用", item = if(networkAvailable.value) "可用" else "不可用") {
                val nextValue = networkAvailable.value.not()
                sharedPreferences?.edit { putBoolean("networkAvailable", nextValue) }
                networkAvailable.value = nextValue
                SettingsUtil.isNetworkAvailable = nextValue
                Toast.makeText(activity, "设置成功，立即生效", Toast.LENGTH_SHORT).show()
            }
        }

        val enableWns: MutableState<Boolean> = remember {
            mutableStateOf(sharedPreferences?.getBoolean("enableWns", true) != false)
        }
        SingleItem(title = "是否启用WNS", item = if(enableWns.value) "打开" else "关闭") {
            val nextValue = enableWns.value.not()
            sharedPreferences?.edit { putBoolean("enableWns", nextValue) }
            enableWns.value = nextValue
            Toast.makeText(activity, "设置成功，重启生效", Toast.LENGTH_SHORT).show()
        }

        val autoPlaySwitchQuality: MutableState<Boolean> = remember {
            mutableStateOf(sharedPreferences?.getBoolean("autoPlaySwitchQuality", true) != false)
        }
        SingleItem(title = "切换音质是否自动播放", item = if(autoPlaySwitchQuality.value) "打开" else "关闭") {
            val nextValue = autoPlaySwitchQuality.value.not()
            sharedPreferences?.edit { putBoolean("autoPlaySwitchQuality", nextValue) }
            autoPlaySwitchQuality.value = nextValue
            Toast.makeText(activity, "设置成功，重启生效", Toast.LENGTH_SHORT).show()
        }

        // 智能混音
        val autoMixNameMap = mapOf(
            PlayerTransition.NONE to "无",
            PlayerTransition.AUTO_MIX to "自动混音",
            PlayerTransition.SEAMLESS to "无缝播放",
            PlayerTransition.CROSS_FADE to "淡入淡出"
        )
        val currAutoMixStatus = remember {
            mutableStateOf(OpenApiSDK.getPlayerTransitionApi().getPlayerTransition())
        }
        var showAutoMixDialog by remember { mutableStateOf(false) }
        if (showAutoMixDialog) {
            MyMultiSelectDialog(
                options = autoMixNameMap.values.toList(),
                onOptionsSelected = { options ->
                    val indexOptions = autoMixNameMap.values.toList().indexOf(options)
                    val choice = autoMixNameMap.keys.toList()[indexOptions]
                    val ret = OpenApiSDK.getPlayerTransitionApi().setPlayerTransition(choice)
                    currAutoMixStatus.value = OpenApiSDK.getPlayerTransitionApi().getPlayerTransition()
                    UiUtils.showToast("智能混音:${ret},${coverErrorCode(ret)}")
                },
                onDismissRequest = { showAutoMixDialog = false }
            )
        }
        SingleItem(title = "智能混音AutoMix", item = autoMixNameMap.getOrDefault(currAutoMixStatus.value,"未知")) {
            showAutoMixDialog = true
        }

        Button(onClick = {
            LaunchReport.coldLaunch().report()
        }, modifier = Modifier.padding(padding)) {
            Text(text = "冷启动事件数据上报")
        }

        var text by remember { mutableStateOf(TextFieldValue("208")) }
        var textScene by remember { mutableStateOf(TextFieldValue("svip_privilege")) }
        Column {

            OutlinedTextField(
                value = text,
                onValueChange = { newValue ->
                    val filteredText = newValue.text
                        .filter { it.isDigit() }
                    text = newValue.copy(text = filteredText)
                },
                label = {Text(text = "受阻错误码:0-默认,200-绿豪,208-超会", fontSize = 12.sp)},
                modifier = Modifier.wrapContentSize()
            )


            OutlinedTextField(
                value = textScene,
                onValueChange = { newValue ->
                    val filteredText = newValue.text
                    textScene = newValue.copy(text = filteredText)
                },
                label = {Text(text = "获取链接使用场景", fontSize = 12.sp)},
                modifier = Modifier.wrapContentSize()
            )
        }

        Row {
            Button(onClick = {
                val value = try {
                    text.text.toInt()
                } catch (_: NumberFormatException) {
                    ToastUtils.showLong("错误码请输入数字")
                    return@Button
                }
                OpenApiSDK.getOpenApi().getCheckoutUrl("demoCheckoutOrderId", value, scene = textScene.text) {
                    if (it.isSuccess()) {
                        WebViewActivity.start(activity, it.data?.payUrl ?: "")
                    } else {
                        UiUtils.showToast(it.errorMsg ?: "")
                    }
                    Log.d("getCheckoutUrl", "res:$it")
                }
            }, modifier = Modifier
                .padding(padding)
                .wrapContentWidth()) {
                Text(text = "车载收银台H5", fontSize = 12.sp)
            }
            Button(onClick = {
                val value = try {
                    text.text.toInt()
                } catch (_: NumberFormatException) {
                    ToastUtils.showLong("错误码请输入数字")
                    return@Button
                }
                OpenApiSDK.getOpenApi().getPayUrlInfo("demoOrderId", value, textScene.text) { resp->
                    if (resp.isSuccess()) {
                        val job = CoroutineScope(Dispatchers.IO).launch {
                            resp.data?.expireSeconds?.let {
                                delay(it.times(1000).toLong())
                                MvBuyQRDialog.showTextDialog(
                                    activity,
                                    "通知","收银台链接已过期")
                            }
                        }
                        MvBuyQRDialog.showQRCodeDialog(activity, resp.data?.payUrl ?: ""){
                            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            MvBuyQRDialog.showTextDialog(
                                activity,
                                "获取参数",
                                "收银台链接过期倒计时：${resp.data?.expireSeconds}s\n" +
                                        "收银台链接准确的过期时间:${formatter.format(resp.data?.expireAt?.times(1000))}")
                            job.cancel()
                        }
                    } else {
                        UiUtils.showToast(resp.errorMsg ?: "")
                    }
                }
            }, modifier = Modifier
                .padding(padding)
                .wrapContentWidth()) {
                Text(text = "二维码短链(爱趣听)", fontSize = 12.sp)
            }

        }
    }
}

@Composable
private fun NetworkTimeoutDialog(activity: Activity, sp: SharedPreferences, showDialog: MutableState<Boolean>) {
    var readTimeout by rememberSaveable { mutableStateOf("") }
    var writeTimeout by rememberSaveable { mutableStateOf("") }
    var connectTimeout by rememberSaveable { mutableStateOf("") }
    var callTimeout by rememberSaveable { mutableStateOf("") }
    Dialog(onDismissRequest = {
        showDialog.value = false
    }) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(5.dp)
        ) {
            OutlinedTextField(
                value = readTimeout,
                onValueChange = {
                    readTimeout = it
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = "请输入readTimeout") }
            )
            OutlinedTextField(
                value = writeTimeout,
                onValueChange = {
                    writeTimeout = it
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = "请输入writeTimeout") }
            )
            OutlinedTextField(
                value = connectTimeout,
                onValueChange = {
                    connectTimeout = it
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = "请输入connectTimeout") }
            )
            OutlinedTextField(
                value = callTimeout,
                onValueChange = {
                    callTimeout = it
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = "请输入callTimeout") }
            )
            OutlinedButton(onClick = {
                val config = NetworkTimeoutConfig(
                    readTimeout.toLongOrDefault(10L),
                    writeTimeout.toLongOrDefault(10L),
                    connectTimeout.toLongOrDefault(10L),
                    callTimeout.toLongOrDefault(40L)
                )
                sp.edit { putString("NetworkTimeoutConfig", GsonHelper.toJson(config).toString()).apply() }
                Toast.makeText(activity, "设置成功，重启应用后生效", Toast.LENGTH_SHORT).show()
                showDialog.value = false
            }) {
                Text(text = "设置")
            }
        }
    }
}