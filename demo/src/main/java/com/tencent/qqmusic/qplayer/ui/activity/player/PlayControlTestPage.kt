package com.tencent.qqmusic.qplayer.ui.activity.player

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tencent.qqmusic.innovation.common.util.UtilContext
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayCallback
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
        var vipInfo by remember { mutableStateOf(Global.getLoginModuleApi().vipInfo) }
        var canTryExcellentQuality by remember { mutableStateOf(false) }
        var canTryGalaxyQuality by remember { mutableStateOf(false) }
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

        Divider(
            modifier = Modifier
                .padding(top = 9.dp)
                .fillMaxWidth()
                .height(3.dp)
        )

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

        Divider(thickness = 3.dp, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))
        Text(
            text = "能否试听臻品音质：$canTryExcellentQuality, 是否试听过：${vipInfo?.excellentQualityTried}," +
                    "试听剩余时间：${vipInfo?.excellentQualityTryTimeLeft}"
        )

        Button(
            onClick = {
                Global.getOpenApi().canTryPlayExcellentQuality {
                    canTryExcellentQuality = it.data ?: true
                    vipInfo = Global.getLoginModuleApi().vipInfo
                }

            }
        ) {
            Text(text = "查看臻品音质试听状态")
        }
        Button(
            onClick = {
                OpenApiSDK.getPlayerApi().tryToOpenExcellentQuality(object : PlayCallback {
                    override fun onSuccess() {
                        Toast.makeText(UtilContext.getApp(), "试听成功！", Toast.LENGTH_SHORT).show()
                    }

                    override fun onFailure(errCode: Int, msg: String?) {
                        Toast.makeText(UtilContext.getApp(), "试听失败！$errCode, msg: $msg", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        ) {
            Text(text = "试听臻品音质")
        }

        Divider(thickness = 3.dp, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))

        Text(
            text = "能否试听臻品全景声：$canTryGalaxyQuality, 是否试听过：${vipInfo?.galaxyQualityTried}," +
                    "试听剩余时间：${vipInfo?.galaxyQualityTryTimeLeft}"
        )

        Button(
            onClick = {
                Global.getOpenApi().canTryPlayGalaxyQuality {
                    canTryGalaxyQuality = it.data ?: true
                    vipInfo = Global.getLoginModuleApi().vipInfo
                }

            }
        ) {
            Text(text = "查看臻品全景声试听状态")
        }
        Button(
            onClick = {
                OpenApiSDK.getPlayerApi().tryToOpenGalaxyQuality(object : PlayCallback {
                    override fun onSuccess() {
                        Toast.makeText(UtilContext.getApp(), "试听成功！", Toast.LENGTH_SHORT).show()
                    }

                    override fun onFailure(errCode: Int, msg: String?) {
                        Toast.makeText(UtilContext.getApp(), "试听失败！$errCode, msg: $msg", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        ) {
            Text(text = "试听臻品全景声")
        }

        Divider(thickness = 3.dp, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))
    }

}