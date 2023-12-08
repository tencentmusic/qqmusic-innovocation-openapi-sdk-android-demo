package com.tencent.qqmusic.qplayer.ui.activity.player

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.qplayer.utils.UiUtils

/**
 *
 * @author: kirkyao
 * @date: 2023/4/20
 */

private val TAG = "PlayControlTestPage"

@Preview
@Composable
fun PlayControlTestPage() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "播放调试", fontSize = 18.sp) },
                contentColor = Color.White,
                actions = {
                }
            )
        }
    ) {
        PlayControlArea()
    }
}

@Preview
@Composable
fun PlayControlArea() {
    var usage by remember { mutableStateOf(TextFieldValue("4")) }
    var contentType by remember { mutableStateOf(TextFieldValue("3")) }

    var streamType by remember { mutableStateOf(TextFieldValue("2")) }

    val padding = 5.dp

    Column(
        modifier = Modifier.padding(5.dp)
    ) {
        Text(text = "设置音频参数", modifier = Modifier.padding(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            TextField(
                value = usage,
                label = {
                    Text(text = "输入usage")
                },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                onValueChange = {
                    usage = it
                },
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight()
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.padding(10.dp))

            TextField(
                value = contentType,
                label = {
                    Text(text = "输入type")
                },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                onValueChange = {
                    contentType = it
                },
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight()
                    .fillMaxWidth()
            )
        }

        Button(
            onClick = {
                if (UiUtils.isStrInt(usage.text) && UiUtils.isStrInt(contentType.text)) {
                    val ret = Global.getPlayerModuleApi().setAudioUsageAndContentType(usage.text.toInt(), contentType.text.toInt())
                    Log.d(TAG, "setAudioUsageAndContentType, ret: $ret")
                } else {
                    UiUtils.showToast("该参数必须输入整数！")
                }
            }
        ) {
            Text("setUsageAndType")
        }

        Divider(modifier = Modifier
            .padding(top = 9.dp)
            .fillMaxWidth()
            .height(3.dp))

        var enableReplayGain by remember {
            mutableStateOf(Global.getPlayerModuleApi().getEnableReplayGain())
        }

        Button(onClick = {
            val curValue = Global.getPlayerModuleApi().getEnableReplayGain()
            Global.getPlayerModuleApi().setEnableReplayGain(curValue.not())
            enableReplayGain = curValue.not()
        }, modifier = Modifier.padding(padding)) {
            Text(text = "开启音量均衡 ${if (enableReplayGain) "开启" else "关闭"}")
        }
    }

}