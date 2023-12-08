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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

        val auto_next_state: MutableState<Boolean> = remember {
            mutableStateOf(sharedPreferences?.getBoolean("error_auto_next", true) ?: true)
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

        SingleItem(title = "播放错误自动下一曲功能", item = if (auto_next_state.value) "开启" else "关闭") {
            val next_value = sharedPreferences?.getBoolean("error_auto_next", true) ?: true
            sharedPreferences?.edit()?.putBoolean("error_auto_next", next_value.not())?.apply()
            Toast.makeText(activity, "请重启应用", Toast.LENGTH_SHORT).show()
            auto_next_state.value = next_value.not()
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

        var number: Int? by remember {
            mutableStateOf(sharedPreferences?.getInt("restore_play_list_err_num", 0) ?: 0)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "自动重试的歌曲数量：")
            TextField(
                value = number?.toString() ?: "",
                onValueChange = {
                    sharedPreferences?.edit()?.putInt("restore_play_list_err_num", it.toIntOrNull() ?: 0)?.apply()
                    Toast.makeText(activity, "请重启应用", Toast.LENGTH_SHORT).show()
                    number = it.toIntOrNull()
                },
                modifier = Modifier.size(50.dp)
            )
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