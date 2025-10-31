package com.tencent.qqmusic.qplayer.ui.activity.main

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.tencent.qqmusic.openapisdk.business_common.cgi.CgiConfig
import com.tencent.qqmusic.openapisdk.business_common.utils.IPCSdkManager
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.qplayer.BaseFunctionManager
import com.tencent.qqmusic.qplayer.baselib.util.deviceid.DeviceInfoManager
import com.tencent.qqmusic.qplayer.ui.activity.MustInitConfig
import com.tencent.qqmusic.qplayer.ui.activity.login.WebViewActivity
import com.tencent.qqmusic.qplayer.utils.UiUtils
import com.tencent.qqmusiccommon.ConfigPreferences

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Scaffold(topBar = { TopBar("关于") }) {
                AboutScreen()
            }
        }
    }
}


@SuppressLint("CommitPrefEdits")
@Preview
@Composable
fun AboutScreen() {
    val tag = "AboutScreen"
    val activity = LocalContext.current as Activity
    val sharedPreferences: SharedPreferences? = LocalContext.current.getSharedPreferences("OpenApiSDKEnv", Context.MODE_PRIVATE)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState())
    ) {
        CopyableItem(title = "包名", content = "com.tencent.qqmusicrecognition")
        CopyableItem(title = "SDK版本号", content = OpenApiSDK.getSdkVersion())
        CopyableItem(title = "APPID", content = MustInitConfig.APP_ID)
        EditorView(title = "ChannelId", content = BaseFunctionManager.proxy.getChannelId()) {
            sharedPreferences?.edit { putString("DemoChannelId", it) }
        }
        EditorView(title = "合作方设备id(DeviceId)", content = sharedPreferences?.getString("demoOpiDeviceId", "123456789") ?: "123456789"){
            if(it.isNotBlank()) {
                sharedPreferences?.edit { putString("demoOpiDeviceId", it) }
            } else {
                UiUtils.showToast("请输入DeviceId")
            }
        }
        EditorView(title = "车型(car_type)", content = sharedPreferences?.getString("demoHardwareInfo", "L9") ?: "L9"){
            sharedPreferences?.edit { putString("demoHardwareInfo", it) }
        }
        EditorView(title = "品牌信息(brand)", content = sharedPreferences?.getString("demoBrand", "Xiaomi") ?: "Xiaomi"){
            sharedPreferences?.edit { putString("demoBrand", it) }
        }
        // 内部方法 仅用于测试。谨慎调用
        CopyableItem(
            title = "特殊模式",
            content = "低内存模式(lowMemoryMode):${sharedPreferences?.getBoolean("lowMemoryMode", false) ?: false}\n" +
                    "IPC模式(IpcMode):${IPCSdkManager.useIpc}"
        )
        CopyableItem(title = (if (OpenApiSDK.getLoginApi().hasLogin()) "openId" else "设备id")+"(日志上报Id)",
            content = CgiConfig.uin())
        CopyableItem(title = "wnsId", content = ConfigPreferences.getInstance().wnsWid.toString())
        CopyableItem(title = "qimei36", content = DeviceInfoManager.q36)
        CopyableItem(title = "协议", content = if (OpenApiSDK.isNewProtocol) "新协议" else "旧协议")
        CopyableItem(title = "架构", content = Build.SUPPORTED_ABIS.first())
        PrivacyView()

        Button(onClick = {
            OpenApiSDK.getLogApi().uploadLog(activity) { code, tips, uuid ->
                if (code == 7) {
                    UiUtils.showToast("$tips")
                    return@uploadLog
                }
                Log.i(tag, "OtherScreen: code $code, tips $tips, uuid $uuid")
                UiUtils.showToast("日志上传结果, code:$code, msg:$tips, uuid $uuid")
            }
        }, modifier = Modifier.padding(5.dp)) {
            Text(text = "日志上传")
        }
    }
}

@Composable
fun CopyableItem(title: String, content: String) {
    val clipboardManager = LocalClipboardManager.current
    Column {
        Text(
            text = title,
            style = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.Black
            ),
            modifier = Modifier.padding(start = 16.dp, top = 10.dp)
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
                    .clickable {
                        clipboardManager.setText(AnnotatedString(content))
                        UiUtils.showToast("已复制到剪切板")
                    }
            )
        }
    }
}

@Composable
fun CopyableText(title: String?=null, content: String?=null, copyText: String?= null, color: Color = Color.Black) {
    val clipboardManager = LocalClipboardManager.current
    Row(verticalAlignment = Alignment.Top) {
        title?.let {
            Text(
                text = "$it:",
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = color
                ),
                modifier = Modifier.wrapContentSize()
            )
        }
        SelectionContainer {
            Text(
                text = content?:"null",
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = color
                ),
                modifier = Modifier
                    .wrapContentSize()
                    .clickable {
                        val copyText2 = copyText?:content
                        copyText2?.let {
                            clipboardManager.setText(AnnotatedString(it))
                            UiUtils.showToast("已复制到剪切板")
                        }
                    }
            )
        }

    }
}


@Composable
fun PrivacyView() {
    Column {
        Text(
            text = "协议",
            style = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.Black
            ),
            modifier = Modifier.padding(start = 16.dp)
        )

        val scrollState = rememberScrollState()
        Row(modifier = Modifier.horizontalScroll(scrollState)) {
            val privacyTag = "隐私保护指引"
            val privacyText = "https://privacy.qq.com/document/preview/240edaf9672d4df0a7060a38fc91fa1d"

            val serviceTag = "服务许可协议"
            val serviceText = "https://y.qq.com/y/static/protocol/car_service.html"

            val userinfoTag = "个人信息保护规则"
            val userinfoText = "https://privacy.qq.com/document/preview/8c8086a85e344bc8bad50fd3e554ee9e"

            val deviceWidthTag = "屏幕宽度"
            val deviceWidthText = "file:///android_asset/window-width.html"

            UrlSpanView(privacyTag, privacyText)
            UrlSpanView(serviceTag, serviceText)
            UrlSpanView(userinfoTag, userinfoText)
            UrlSpanView(deviceWidthTag, deviceWidthText)
        }

    }
}

@Composable
fun UrlSpanView(tag: String, text: String, modifier: Modifier = Modifier
    .padding(horizontal = 16.dp)
    .height(60.dp)) {
    val spannedText = buildAnnotatedString {
        withStyle(style = SpanStyle(color = Color.Blue)) {
            pushStringAnnotation(tag = text, annotation = tag)
            append(tag)
        }
    }

    val context = LocalContext.current

    ClickableText(
        text = spannedText,
        style = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            color = Color.Black
        ),
        modifier = modifier)
    { offset->
        spannedText.getStringAnnotations(offset, offset).firstOrNull()?.let { span->
            WebViewActivity.start(context, span.tag)
        }
    }
}

@Composable
fun EditorView(title: String, content: String, updateHandler: (String) -> Unit) {
    // 可点击编辑的视图
    val context = LocalContext.current
    Column {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )
            )
            IconButton(
                onClick = {
                    // 点击后弹出编辑对话框
                    val editText = android.widget.EditText(context).apply {
                        setText(content)
                    }
                    val inputDialog = androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle("编辑")
                        .setView(editText)
                        .setPositiveButton("确定") { _, _ ->
                            val newValue = editText.text.toString()
                            updateHandler.invoke(newValue)
                            UiUtils.showToast("更新成功，重启生效")
                        }.setNegativeButton("取消", null)
                        .create()
                    inputDialog.show()
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                androidx.compose.material.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = Color.Gray
                )
            }
        }
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
            )
        }
    }
}