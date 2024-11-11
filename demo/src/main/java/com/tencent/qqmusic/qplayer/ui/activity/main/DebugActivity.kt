package com.tencent.qqmusic.qplayer.ui.activity.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import com.tencent.qqmusic.innovation.common.util.DeviceUtils
import com.tencent.qqmusic.innovation.common.util.ToastUtils
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.business_common.event.BusinessEventHandler
import com.tencent.qqmusic.openapisdk.business_common.event.event.LoginEvent
import com.tencent.qqmusic.openapisdk.business_common.login.OpenIdInfo
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.login.AuthType
import com.tencent.qqmusic.openapisdk.hologram.HologramManager
import com.tencent.qqmusic.openapisdk.hologram.service.IFireEyeService
import com.tencent.qqmusic.qplayer.App
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


@Composable
fun DebugScreen() {
    val activity = LocalContext.current as Activity
    val padding = 5.dp
    val strict = "严格模式"
    val noStrict = "无检查模式"
    val sharedPreferences: SharedPreferences? = try {
        SPBridgeProxy.getSharedPreferences("OpenApiSDKEnv", Context.MODE_PRIVATE)
    } catch (e: Exception) {
        QLog.e("DebugScreen", "getSharedPreferences error e = ${e.message}")
        null
    }

    val (showLoginDialog, setShowDialog) = remember { mutableStateOf(false) }
    val showLogDirDialog = remember { mutableStateOf(false) }
    val showActivityDialog = remember { mutableStateOf(false) }
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
                options = listOf(noStrict, strict),
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

        val tryPauseFirst: MutableState<Boolean> = remember {
            mutableStateOf(sharedPreferences?.getBoolean("tryPauseFirst", false) ?: false)
        }
        SingleItem(title = "播放页操作前进行暂停", item = tryPauseFirst.value.toString()) {
            val nextValue = tryPauseFirst.value.not()
            sharedPreferences?.edit()?.putBoolean("tryPauseFirst", nextValue)?.apply()
            tryPauseFirst.value = nextValue
            Toast.makeText(activity, "设置成功，重启生效", Toast.LENGTH_SHORT).show()
        }

        SingleItem(title = "设置SessionActivity", item = "") {
            showActivityDialog.value = true
        }

        if (showActivityDialog.value) {
            Dialog(onDismissRequest = {
                showActivityDialog.value = false
            }) {
                val input = remember {
                    mutableStateOf(sharedPreferences?.getString("sessionActivity", "") ?: "")
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.background(color = Color(248, 248, 248))
                ) {
                    TextField(value = input.value,
                        placeholder = {
                            Text(text = "输入SessionActivity")
                        }, onValueChange = {
                            input.value = it
                        }, modifier = Modifier.width(300.dp)
                    )
                    Button(onClick = {
                        OpenApiSDK.getPlayerApi().setMediaSessionActivityName(input.value)
                        showActivityDialog.value = false
                        sharedPreferences?.edit { putString("sessionActivity", input.value) }
                        Toast.makeText(
                            activity,
                            "切歌生效",
                            Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Text(text = "确定")
                    }
                }

            }
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
                App.init(activity.applicationContext)
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