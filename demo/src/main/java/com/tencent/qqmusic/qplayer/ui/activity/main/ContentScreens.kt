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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tencent.qqmusic.innovation.common.util.UtilContext
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.business_common.login.OpenIdInfo
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.login.AuthType
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.qplayer.report.report.LaunchReport
import com.tencent.qqmusic.qplayer.ui.activity.MustInitConfig
import com.tencent.qqmusic.qplayer.ui.activity.OpenApiDemoActivity
import com.tencent.qqmusic.qplayer.ui.activity.SongCacheDemoActivity
import com.tencent.qqmusic.qplayer.ui.activity.home.HomePage
import com.tencent.qqmusic.qplayer.ui.activity.home.HomeViewModel
import com.tencent.qqmusic.qplayer.ui.activity.login.MinePage
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerActivity
import com.tencent.qqmusic.sharedfileaccessor.SPBridge
import com.tencent.qqmusicplayerprocess.util.DolbyUtil

@Composable
fun HomeScreen(categoryViewModel: HomeViewModel = viewModel()) {
    HomePage(categoryViewModel)
}

@Composable
fun RankScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.colorPrimaryDark))
            .wrapContentSize(Alignment.Center)
    ) {
        Text(
            text = "Rank View",
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
            fontSize = 25.sp
        )
    }
}

@Composable
fun RadioScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.colorPrimaryDark))
            .wrapContentSize(Alignment.Center)
    ) {
        Text(
            text = "Radio View",
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
            fontSize = 25.sp
        )
    }
}

@Composable
fun HiresScreen() {
    HiresSectionPage(viewModel())
}

@Composable
fun DolbyScreen() {
    DolbySectionPage(viewModel())
}

@Composable
fun SearchScreen(homeViewModel: HomeViewModel = viewModel()) {
    SearchPage(homeViewModel)
}

@Composable
fun MineScreen() {
    MinePage()
}

@Composable
fun OtherScreen() {
    val activity = LocalContext.current as Activity
    var text by remember { mutableStateOf("当前为${if (OpenApiSDK.isNewProtocol) "新协议" else "旧协议"}") }
    val padding = 5.dp
    Column(
        modifier = Modifier
            .padding(bottom = 46.dp)
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
            Log.d("OtherScreen", "minBufSize：$minBufSize")
            // Check whether there is an in-device Dolby AC-4 IMS decoder.z
            val mediaCodecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            val mediaCodecInfos = mediaCodecList.codecInfos
            mediaCodecInfos?.forEach {
                Log.d("OtherScreen", "codeInfo:${it.name}, isEncoder:${it.isEncoder} support:${it.supportedTypes?.contentToString()}")
            }
            if (OpenApiSDK.getPlayerApi().supportDolbyDecoder()) {
                Toast.makeText(UtilContext.getApp(),"支持杜比",Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(UtilContext.getApp(),"不支持杜比",Toast.LENGTH_SHORT).show()
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

        Row {
            Button(onClick = {
                OpenApiSDK.init(
                    activity.applicationContext,
                    MustInitConfig.APP_ID,
                    MustInitConfig.APP_KEY
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