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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tencent.qqmusic.innovation.common.util.UtilContext
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.qplayer.report.report.LaunchReport
import com.tencent.qqmusic.qplayer.ui.activity.OpenApiDemoActivity
import com.tencent.qqmusic.qplayer.ui.activity.SongCacheDemoActivity
import com.tencent.qqmusic.qplayer.ui.activity.login.WebViewActivity
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerActivity
import com.tencent.qqmusic.qplayer.utils.UiUtils
import com.tencent.qqmusic.sharedfileaccessor.SPBridge

class OtherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OtherScreen()
        }
    }
}

@Preview
@Composable
fun OtherScreen() {
    val activity = LocalContext.current as Activity
    val sharedPreferences: SharedPreferences? = try {
        SPBridge.get().getSharedPreferences("OpenApiSDKEnv", Context.MODE_PRIVATE)
    } catch (e: Exception) {
        QLog.e("OtherScreen", "getSharedPreferences error e = ${e.message}")
        null
    }
    val padding = 5.dp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SingleItem(title = "OpenApi测试界面", item = "") {
            activity.startActivity(Intent(activity, OpenApiDemoActivity::class.java))
        }

        SingleItem(title = "缓存测试界面", item = "") {
            activity.startActivity(Intent(activity, SongCacheDemoActivity::class.java))
        }

        SingleItem(title = "播放测试界面", item = "") {
            activity.startActivity(Intent(activity, PlayerActivity::class.java))
        }

        SingleItem(title = "是否支持杜比",
            item = if (OpenApiSDK.getPlayerApi().supportDolbyDecoder()) "支持"
            else "不支持") {
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
            mutableStateOf(sharedPreferences?.getBoolean("restore_play_list", true) ?: true)
        }
        val isplaynext: MutableState<Boolean> = remember {
            mutableStateOf(sharedPreferences?.getBoolean("restore_next_song", true) ?: true)
        }


        val isUseForegroundService: MutableState<Boolean> = remember {
            mutableStateOf(sharedPreferences?.getBoolean("isUseForegroundService", true) ?: true)
        }

        SingleItem(title = "播放列表重启自动恢复功能", item = if (state.value) "开启" else "关闭") {
            val status = sharedPreferences?.getBoolean("restore_play_list", true) ?: true
            sharedPreferences?.edit()?.putBoolean("restore_play_list", status.not())?.apply()
            Toast.makeText(activity, "重启app生效", Toast.LENGTH_SHORT).show()
            state.value = status.not()
        }


        SingleItem(title = "正常播放情况下是否播放下一曲", item = if (isplaynext.value) "开启" else "关闭") {
            val status = sharedPreferences?.getBoolean("restore_next_song", true) ?: true
            sharedPreferences?.edit()?.putBoolean("restore_next_song", status.not())?.apply()
            Toast.makeText(activity, "重启app生效", Toast.LENGTH_SHORT).show()
            isplaynext.value = status.not()
        }

        var number: Int by remember {
            mutableStateOf(sharedPreferences?.getInt("restore_play_list_err_num", 0) ?: 0)
        }

        var autoPlayErrNum: String by remember {
            mutableStateOf(number.toString())
        }

        var showDialog by remember { mutableStateOf(false) }
        if (showDialog){
            Dialog(onDismissRequest = { showDialog = false }) {
                // 对话框内容
                Column(modifier = Modifier.padding(16.dp)) {
                    TextField(
                        value = autoPlayErrNum,
                        onValueChange = { newText -> autoPlayErrNum = newText},
                        label = { Text(
                            text = "请输入重试次数或自定义值",
                            style = MaterialTheme.typography.body1.merge()) },
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = Color.White,
                            textColor = Color.Black,
                            cursorColor = Color.Black),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
                    )
                    Button(onClick = {
                        // 点击确认按钮时的处理逻辑
                        // 这里可以处理 text 变量，例如发送数据、更新UI等
                        if (autoPlayErrNum.toIntOrNull() == null){
                            Toast.makeText(activity,"请输入数字",Toast.LENGTH_SHORT).show()
                        }else{
                            showDialog = false // 关闭对话框
                            sharedPreferences?.edit()?.putInt("restore_play_list_err_num", autoPlayErrNum.toInt())?.apply()
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
                    if (options=="重试n次" || options=="自定义") {
                        showDialog = true
                    }
                    else{
                        number = if (options=="列表重试") 0 else -1
                        sharedPreferences?.edit()?.putInt("restore_play_list_err_num", number)?.apply()
                        Toast.makeText(activity, "已设置为：${number},请重启应用", Toast.LENGTH_SHORT).show()
                    }
                },
                onDismissRequest = { showSelectDialog = false }
            )
        }

        SingleItem(title = "播放错误自动重试配置", item = when {
            number < 0 -> "当前值:${number}->不重试"
            number > 0 -> "当前值:${number}->重试${number}次"
            else -> "当前值:0->列表重试"
        }) {
            showSelectDialog = true
        }


        SingleItem(title = "前台服务功能", item = if (isUseForegroundService.value) "开启" else "关闭") {
            val next_value = sharedPreferences?.getBoolean("isUseForegroundService", true) ?: true
            sharedPreferences?.edit()?.putBoolean("isUseForegroundService", next_value.not())?.apply()
            Toast.makeText(activity, "请重启应用", Toast.LENGTH_SHORT).show()
            isUseForegroundService.value = next_value.not()
        }

        val playWhenRequestFocusFailed: MutableState<Boolean> = remember {
            mutableStateOf(sharedPreferences?.getBoolean("playWhenRequestFocusFailed", true) ?: true)
        }

        SingleItem(title = "申请焦点失败继续播放功能", item = if (playWhenRequestFocusFailed.value) "开启" else "关闭") {
            val next_value = sharedPreferences?.getBoolean("playWhenRequestFocusFailed", true) ?: true
            sharedPreferences?.edit()?.putBoolean("playWhenRequestFocusFailed", next_value.not())?.apply()
            Toast.makeText(activity, "请重启应用", Toast.LENGTH_SHORT).show()
            playWhenRequestFocusFailed.value = next_value.not()
        }

        Button(onClick = {
            OpenApiSDK.getLogApi().uploadLog(activity) { code, tips, uuid ->
                Log.i("OtherScreen", "OtherScreen: code $code, tips $tips, uuid $uuid")
                AppScope.launchUI {
                    Toast.makeText(
                        activity,
                        "日志上传结果, code:$code, msg:$tips, uuid $uuid",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }, modifier = Modifier.padding(padding)) {
            Text(text = "日志上传")
        }

        Button(onClick = {
            LaunchReport.coldLaunch().report()
        }, modifier = Modifier.padding(padding)) {
            Text(text = "冷启动事件数据上报")
        }
        
        Button(onClick = {
            Global.getOpenApi().getPayUrl("demoOrderId") {
                val default = "https://developer.y.qq.com/docs/edge_android#/overview"
                if (it.isSuccess()) {
                    WebViewActivity.start(activity, it.data ?: default)
                } else {
                    WebViewActivity.start(activity, default)
                    UiUtils.showToast(it.errorMsg ?: "")
                }
            }
        }, modifier = Modifier.padding(padding)) {
            Text(text = "打开VIP购买页面")
        }
    }
}