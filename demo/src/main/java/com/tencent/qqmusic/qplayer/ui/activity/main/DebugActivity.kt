package com.tencent.qqmusic.qplayer.ui.activity.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import com.tencent.qqmusic.innovation.common.util.DeviceUtils
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.business_common.event.BusinessEventHandler
import com.tencent.qqmusic.openapisdk.business_common.event.event.LoginEvent
import com.tencent.qqmusic.openapisdk.business_common.login.OpenIdInfo
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.login.AuthType
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.qplayer.core.player.proxy.SPBridgeProxy
import com.tencent.qqmusic.qplayer.ui.activity.MustInitConfig
import com.tencent.qqmusic.qplayer.ui.activity.OpenApiDemoActivity

class DebugActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DebugScreen()
        }
    }
}

@Preview
@Composable
fun DebugScreen() {
    val activity = LocalContext.current as Activity
    val padding = 5.dp
    val strict = "严格模式"
    val no_strict = "无检查模式"
    val sharedPreferences: SharedPreferences? = try {
        SPBridgeProxy.getSharedPreferences("OpenApiSDKEnv", Context.MODE_PRIVATE)
    } catch (e: Exception) {
        QLog.e("DebugScreen", "getSharedPreferences error e = ${e.message}")
        null
    }
    val (showLoginDialog, setShowDialog) = remember { mutableStateOf(false) }
    val showLogDirDialog = remember { mutableStateOf(false) }
    val enableLog = sharedPreferences?.getBoolean("enableLog", true) ?: true
    var isEnableLog by remember { mutableStateOf(enableLog) }


    loginExpiredDialog(showDialog = showLoginDialog, setShowDialog = setShowDialog)
    DisposableEffect(Unit) {
        val listener =
            BusinessEventHandler { event ->
                when (event.code) {
                    LoginEvent.UserAccountLoginExpired -> {
                        setShowDialog(true)
                    }
                }
            }

        OpenApiSDK.registerBusinessEventHandler(listener)
        onDispose {
            OpenApiSDK.unregisterBusinessEventHandler(listener)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var showDialog by remember { mutableStateOf(false) }
        if (showDialog) {
            MyMultiSelectDialog(
                options = listOf(no_strict, strict),
                onOptionsSelected = { options ->
                    showDialog = false
                    MustInitConfig.setAppCheckMode(options == strict)
                    Toast.makeText(activity, "${options}被选择,重启应用后生效", Toast.LENGTH_SHORT).show()
                },
                onDismissRequest = { showDialog = false }
            )
        }

        SingleItem(title = "是否关闭日志打印功能", item = if (isEnableLog) "开启" else "关闭") {
            val value = isEnableLog.not()
            sharedPreferences?.edit()?.putBoolean("enableLog", value)?.apply()
            isEnableLog = value
            OpenApiSDK.getLogApi().setLogEnable(value)
            Toast.makeText(
                activity,
                "已切换为${if (value) "开启状态" else "关闭状态"}",
                Toast.LENGTH_SHORT
            ).show()
        }

        SingleItem(title = "OpenApi接口", item = "") {
            val intent = Intent(activity, OpenApiDemoActivity::class.java)
            intent.putExtra("isDebug", true)
            activity.startActivity(intent)
        }

        SingleItem(title = "设置Log存储路径", item = "") {
            showLogDirDialog.value = true
        }

        if (showLogDirDialog.value) {
            Dialog(onDismissRequest = {
                showLogDirDialog.value = false
            }) {
                val input = remember {
                    mutableStateOf(sharedPreferences?.getString("logFileDir", "") ?: "")
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.background(color = Color(248, 248, 248))
                ) {
                    TextField(value = input.value,
                        placeholder = {
                            Text(text = "输入路径")
                        }, onValueChange = {
                            input.value = it
                        }, modifier = Modifier.width(300.dp)
                    )
                    Button(onClick = {
                        showLogDirDialog.value = false
                        sharedPreferences?.edit { putString("logFileDir", input.value) }
                        Toast.makeText(
                            activity,
                            "请重启应用",
                            Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Text(text = "确定")
                    }
                }

            }
        }



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
            AppScope.launchUI {
                try {
                    activity.startActivity(Intent(activity, PlayProcessReportTestActivity::class.java).apply {
                        putExtra("extra", Bundle().apply { putString("key", "crash") })
                        flags = flags xor Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                } catch (_: Exception) {
                }
            }
        }, modifier = Modifier.padding(padding)) {
            Text(text = "播放进程立刻Crash！！")
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
    }
}