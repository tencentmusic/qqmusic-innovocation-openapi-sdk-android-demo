package com.tencent.qqmusic.qplayer.ui.activity.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodecList
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tencent.qqmusic.innovation.common.util.DeviceUtils
import com.tencent.qqmusic.innovation.common.util.UtilContext
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.business_common.login.OpenIdInfo
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.login.AuthType
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.qplayer.report.report.LaunchReport
import com.tencent.qqmusic.qplayer.ui.activity.MustInitConfig
import com.tencent.qqmusic.qplayer.ui.activity.OpenApiDemoActivity
import com.tencent.qqmusic.qplayer.ui.activity.SongCacheDemoActivity
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerActivity
import com.tencent.qqmusic.sharedfileaccessor.SPBridge

class OtherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OtherScreen()
        }
    }
}

@Composable
fun OtherScreen() {
    val activity = LocalContext.current as Activity
    var text by remember { mutableStateOf("当前为${if (OpenApiSDK.isNewProtocol) "新协议" else "旧协议"}") }
    val sharedPreferences: SharedPreferences? = try {
        SPBridge.get().getSharedPreferences("OpenApiSDKEnv", Context.MODE_PRIVATE)
    } catch (e: Exception) {
        QLog.e("OtherScreen", "getSharedPreferences error e = ${e.message}")
        null
    }
    val isTestEnv = sharedPreferences?.getBoolean("isTestEnv", false) ?: false
    var isOfficialEnv by remember { mutableStateOf(!isTestEnv) }
    val padding = 5.dp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Button(onClick = {
            val openIdInfo = OpenIdInfo(
                expireTime = 1662361845L,
                accessToken = "auh148e418a6a50c19d5e17c21b535e02ff5fec7cedf73e002c56af56cf28fb2be4",
                openId = "12951783807858803786",
                refreshToken = "4989176387247f2a3ff438df2bd189fa2cf4a7dc8922f07c901675b8016bb834",
                type = AuthType.WX
            )
            Global.getLoginModuleApi().updateOpenIdInfo(AuthType.WX, openIdInfo)
            OpenApiSDK.getOpenApi().fetchCollectedFolder {

            }
        }, modifier = Modifier.padding(padding)) {
            Text(text = "测试登录态过期")
        }
        Button(onClick = {
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

        }, modifier = Modifier.padding(6.dp)) {
            Text(text = "测试是否支持杜比")
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
            AppScope.launchUI {
                try {
                    activity.startActivity(Intent(activity, PlayProcessReportTestActivity::class.java).apply {
                        putExtra("extra", Bundle().apply { putString("key", "crash") })
                        flags = flags xor Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                } catch (e: Exception) {
                }
            }
        }, modifier = Modifier.padding(padding)) {
            Text(text = "播放进程立刻Crash！！")
        }

        Button(onClick = {
            throw IllegalStateException("test crash")
        }, modifier = Modifier.padding(padding)) {
            Text(text = "立即Crash！")
        }

        Button(onClick = {
            activity.startActivity(Intent(activity, OpenApiDemoActivity::class.java))
        }, modifier = Modifier.padding(padding)) {
            Text(text = "OpenApi接口")
        }

        Text(text = "当前为${if (OpenApiSDK.isNewProtocol) "新协议" else "旧协议"}")

        Button(onClick = {
            activity.startActivity(Intent(activity, SongCacheDemoActivity::class.java))
        }, modifier = Modifier.padding(padding)) {
            Text(text = "缓存接口demo")
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

        Button(onClick = {
            val status = sharedPreferences?.getBoolean("restore_play_list", true) ?: true
            sharedPreferences?.edit()?.putBoolean("restore_play_list", status.not())?.apply()
            Toast.makeText(activity, "请重启应用", Toast.LENGTH_SHORT).show()
            state.value = status.not()
        }, modifier = Modifier.padding(padding)) {
            Text(text = "切换播放列表恢复功能开关，当前状态 ${if (state.value) "开启" else "关闭"}")
        }


        Button(onClick = {
            val next_value = sharedPreferences?.getBoolean("error_auto_next", true) ?: true
            sharedPreferences?.edit()?.putBoolean("error_auto_next", next_value.not())?.apply()
            Toast.makeText(activity, "请重启应用", Toast.LENGTH_SHORT).show()
            auto_next_state.value = next_value.not()
        }, modifier = Modifier.padding(padding)) {
            Text(text = "播放错误自动下一曲开关 ${if (auto_next_state.value) "开启" else "关闭"}")
        }

        Button(onClick = {
            val next_value = sharedPreferences?.getBoolean("isUseForegroundService", true) ?: true
            sharedPreferences?.edit()?.putBoolean("isUseForegroundService", next_value.not())?.apply()
            Toast.makeText(activity, "请重启应用", Toast.LENGTH_SHORT).show()
            isUseForegroundService.value = next_value.not()
        }, modifier = Modifier.padding(padding)) {
            Text(text = "是否启用前台服务功能 ${if (isUseForegroundService.value) "开启" else "关闭"}")
        }

        val playWhenRequestFocusFailed: MutableState<Boolean> = remember {
            mutableStateOf(sharedPreferences?.getBoolean("playWhenRequestFocusFailed", true) ?: true)
        }

        Button(onClick = {
            val next_value = sharedPreferences?.getBoolean("playWhenRequestFocusFailed", true) ?: true
            sharedPreferences?.edit()?.putBoolean("playWhenRequestFocusFailed", next_value.not())?.apply()
            Toast.makeText(activity, "请重启应用", Toast.LENGTH_SHORT).show()
            playWhenRequestFocusFailed.value = next_value.not()
        }, modifier = Modifier.padding(padding)) {
            Text(text = "申请焦点失败继续播放 ${if (playWhenRequestFocusFailed.value) "开启" else "关闭"}")
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

        Row {
            Button(onClick = {
                OpenApiSDK.init(
                    activity.applicationContext,
                    MustInitConfig.APP_ID,
                    MustInitConfig.APP_KEY,
                    DeviceUtils.getAndroidID()
                )
                Toast.makeText(activity, "初始化成功", Toast.LENGTH_SHORT).show()
            }, modifier = Modifier.padding(padding)) {
                Text(text = "初始化SDK")
            }

            Button(onClick = {
                OpenApiSDK.destroy()
                Toast.makeText(activity, "销毁成功", Toast.LENGTH_SHORT).show()
            }, modifier = Modifier.padding(padding)) {
                Text(text = "销毁SDK")
            }
        }

        Button(onClick = {
            activity.startActivity(
                Intent(
                    activity,
                    PlayerActivity::class.java
                )
            )
        }, modifier = Modifier.padding(padding)) {
            Text(text = "进入播放测试界面")
        }

        Button(onClick = {
            val url = OpenApiSDK.getOpenApi().getVipPayUrl()
            Log.i("getVipPayUrl", "url:$url")
            activity.startActivity(Intent().apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }, modifier = Modifier.padding(padding)) {
            Text(text = "打开VIP购买页面")
        }

    }
}