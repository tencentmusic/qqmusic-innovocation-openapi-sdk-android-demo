package com.tencent.qqmusic.qplayer.ui.activity.main

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tencent.qqmusic.openapisdk.business_common.cgi.CgiConfig
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.qplayer.BuildConfig
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.qplayer.baselib.util.deviceid.DeviceInfoManager
import com.tencent.qqmusic.qplayer.ui.activity.MustInitConfig

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AboutScreen()
        }
    }
}

@Preview
@Composable
fun AboutScreen() {
    val tag = "AboutScreen"
    val activity = LocalContext.current as Activity
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState())
    ){
        CopyableText(title = "包名", content = "com.tencent.qqmusicrecognition")
        CopyableText(title = "SDK版本号",content = BuildConfig.VERSION_NAME)
        CopyableText(title = "uin",content = CgiConfig.uin())
        CopyableText(title = "qime36",content = DeviceInfoManager.q36)
        CopyableText(title = "协议", content = if (OpenApiSDK.isNewProtocol) "新协议" else "旧协议")
        CopyableText(title = "APPID", content = MustInitConfig.APP_ID)
        CopyableText(title = "APPKEY", content = MustInitConfig.APP_KEY)
        CopyableText(title = "架构", content = Build.SUPPORTED_ABIS.first())
        Button(onClick = {
            OpenApiSDK.getLogApi().uploadLog(activity) { code, tips, uuid ->
                Log.i(tag, "OtherScreen: code $code, tips $tips, uuid $uuid")
                AppScope.launchUI {
                    Toast.makeText(
                        activity,
                        "日志上传结果, code:$code, msg:$tips, uuid $uuid",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }, modifier = Modifier.padding(5.dp)) {
            Text(text = "日志上传")
        }
    }
}

@Composable
fun CopyableText(title: String, content: String) {
    Column {
        Text(
            text = title,
            style = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.Black
            ),
            modifier = Modifier.padding(start = 16.dp)
        )
        SelectionContainer {
            Text(
                text = content,
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = Color.Black
                ),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .height(60.dp)
            )
        }
    }
}