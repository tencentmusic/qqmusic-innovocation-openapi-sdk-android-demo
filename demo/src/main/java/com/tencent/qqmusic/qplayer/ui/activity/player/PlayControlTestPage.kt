package com.tencent.qqmusic.qplayer.ui.activity.player

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tencent.qqmusic.innovation.common.util.UtilContext
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayCallback
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.openapisdk.core.player.VocalAccompanyConfig
import com.tencent.qqmusic.openapisdk.core.player.VocalPercent
import com.tencent.qqmusic.openapisdk.model.PlaySpeedType
import com.tencent.qqmusic.openapisdk.model.ProfitInfo
import com.tencent.qqmusic.qplayer.baselib.util.JobDispatcher
import com.tencent.qqmusic.qplayer.core.supersound.GalaxyFileQualityManager
import com.tencent.qqmusic.qplayer.core.supersound.MasterSRManager
import com.tencent.qqmusic.qplayer.ui.activity.TrafficActivity
import com.tencent.qqmusic.qplayer.ui.activity.SongCacheDemoActivity
import com.tencent.qqmusic.qplayer.core.supersound.SQSRManager
import com.tencent.qqmusic.qplayer.ui.activity.aiaccompany.AiListenTogetherActivity
import com.tencent.qqmusic.qplayer.ui.activity.download.DownloadActivity
import com.tencent.qqmusic.qplayer.ui.activity.musictherapy.MusicTherapyActivity
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.StringBuilder
import kotlin.concurrent.thread


/**
 *
 * @author: kirkyao
 * @date: 2023/4/20
 */

private val TAG = "PlayControlTestPage"

@RequiresApi(Build.VERSION_CODES.O)
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

@RequiresApi(Build.VERSION_CODES.O)
@Preview
@Composable
fun PlayControlArea() {
    var usage by remember { mutableStateOf(TextFieldValue("4")) }
    var contentType by remember { mutableStateOf(TextFieldValue("3")) }
    val activity = LocalContext.current as Activity
    var volum by remember { mutableStateOf(TextFieldValue("0.8")) }
    var volumTransient by remember { mutableStateOf(TextFieldValue("0.2")) }

    val padding = 5.dp
    Column(
        modifier = Modifier
            .padding(5.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // todo 改造成openApi接口获取
        var vipInfo by remember { mutableStateOf(Global.getLoginModuleApi().vipInfo) }
        var canTryExcellentQuality by remember { mutableStateOf(false) }
        var canTryGalaxyQuality by remember { mutableStateOf(false) }
        var canTryDolbyQuality by remember { mutableStateOf(false) }

        val scope = rememberCoroutineScope()
        var trafficMb by remember { mutableStateOf(0f) }

        Text(text = "今天已消耗流量：${trafficMb}MB")
        Row {
            Button(onClick = {
                scope.launch(Dispatchers.IO) {
                    val todayTs = UiUtils.getTodayTimestamps()
                    trafficMb = OpenApiSDK.getTrafficApi().getSDKTrafficMB(todayTs.first, todayTs.second)
                }
            }) {
                Text(text = "刷新")
            }

            Button(
                onClick = {
                    activity.startActivity(Intent(activity, TrafficActivity::class.java))
                },
                modifier = Modifier.padding(start = 10.dp)
            ) {
                Text(text = "前往流量查询页面")
            }
        }
        Divider(modifier = Modifier
            .padding(top = 9.dp)
            .fillMaxWidth()
            .height(3.dp))

        Text(
            text = "最大音量比例：${PlayerObserver.maxVolumeRatio}",
            fontFamily = FontFamily.Monospace
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "0",
                fontFamily = FontFamily.Monospace
            )

            Slider(
                value = PlayerObserver.toOneDigits(PlayerObserver.maxVolumeRatio),
                valueRange = 0f..1.0f,
                onValueChange = {
                    PlayerObserver.maxVolumeRatio = PlayerObserver.toOneDigits(it)
                },
                onValueChangeFinished = {
                    val ret = OpenApiSDK.getPlayerApi().setVolumeRatio(PlayerObserver.maxVolumeRatio)
                    Toast.makeText(UtilContext.getApp(), if (ret) "设置最大音量比例为: ${PlayerObserver.maxVolumeRatio}" else "设置最大比例失败，请重试", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .weight(1f, true)
                    .padding(horizontal = 10.dp)
            )

            Text(
                text = "1.0",
                fontFamily = FontFamily.Monospace
            )
        }

        Button(
            onClick = {
                activity.startActivity(Intent(activity, MusicTherapyActivity::class.java))
            }
        ) {
            Text(text = "前往疗愈播放器")
        }

        Divider(
            modifier = Modifier
                .padding(top = 9.dp)
                .fillMaxWidth()
                .height(3.dp)
        )

        Row {
            Button(
                onClick = {
                    activity.startActivity(Intent(activity, DownloadActivity::class.java))
                }
            ) {
                Text(text = "前往下载管理页面")
            }

            Button(
                onClick = {
                    activity.startActivity(Intent(activity, SongCacheDemoActivity::class.java))
                }, modifier = Modifier.padding(start = 10.dp)
            ) {
                Text(text = "前往缓存管理页面")
            }
        }

        Divider(
            modifier = Modifier
                .padding(top = 9.dp)
                .fillMaxWidth()
                .height(3.dp)
        )

        Button(
            onClick = {
                activity.startActivity(Intent(activity, AiListenTogetherActivity::class.java))
            }
        ) {
            Text(text = "前往AI伴听页面")
        }

        Divider(
            modifier = Modifier
                .padding(top = 9.dp)
                .fillMaxWidth()
                .height(3.dp)
        )

        var enableReplayGain by remember {
            mutableStateOf(OpenApiSDK.getPlayerApi().getEnableReplayGain())
        }

        Button(onClick = {
            val curValue = OpenApiSDK.getPlayerApi().getEnableReplayGain()
            OpenApiSDK.getPlayerApi().setEnableReplayGain(curValue.not())
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
                OpenApiSDK.getOpenApi().canTryPlayExcellentQuality {
                    canTryExcellentQuality = it.data ?: true
                    vipInfo = Global.getLoginModuleApi().vipInfo
                }

            }
        ) {
            Text(text = "查看臻品音质试听状态")
        }
        Button(
            onClick = {
                OpenApiSDK.getPlayerApi().tryToOpenExcellentQuality(OpenApiSDK.getPlayerApi().getCurrentSongInfo(), object : PlayCallback {
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
                OpenApiSDK.getOpenApi().canTryPlayGalaxyQuality {
                    canTryGalaxyQuality = it.data ?: true
                    vipInfo = Global.getLoginModuleApi().vipInfo
                }

            }
        ) {
            Text(text = "查看臻品全景声试听状态")
        }
        Button(
            onClick = {
                OpenApiSDK.getPlayerApi().tryToOpenGalaxyQuality(OpenApiSDK.getPlayerApi().getCurrentSongInfo(), object : PlayCallback {
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

        Text(
            text = "能否试听杜比全景声：$canTryDolbyQuality, 是否试听过：${vipInfo?.isProfitTriedByType(ProfitInfo.QUALITY_TYPE_DOLBY)}," +
                    "试听剩余时间：${vipInfo?.getProfitInfoByType(ProfitInfo.QUALITY_TYPE_DOLBY)?.remainTime}"
        )

        Button(
            onClick = {
                OpenApiSDK.getOpenApi().canTryPlayDolbyQuality() {
                    canTryDolbyQuality = it.data ?: true
                    vipInfo = Global.getLoginModuleApi().vipInfo
                }

            }
        ) {
            Text(text = "查看杜比全景声试听状态")
        }
        Button(
            onClick = {
                OpenApiSDK.getPlayerApi().tryToOpenDolbyQuality(OpenApiSDK.getPlayerApi().getCurrentSongInfo(), object : PlayCallback {
                    override fun onSuccess() {
                        Toast.makeText(UtilContext.getApp(), "试听成功！", Toast.LENGTH_SHORT).show()
                    }

                    override fun onFailure(errCode: Int, msg: String?) {
                        Toast.makeText(UtilContext.getApp(), "试听失败！$errCode, msg: $msg", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        ) {
            Text(text = "试听杜比全景声")
        }

        Button(
            onClick = {
                Global.getPlayerModuleApi().setProgressCallbackFrequency(500)
            }
        ) {
            Text(text = "设置播放进度回调频率500ms")
        }

        Divider(thickness = 3.dp, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))
        Text(
            text = "倍速：${PlayerObserver.playSpeed}x",
            fontFamily = FontFamily.Monospace
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "0.5x",
                fontFamily = FontFamily.Monospace
            )

            Slider(
                value = PlayerObserver.toOneDigits(PlayerObserver.playSpeed),
                valueRange = 0.5f..2.0f,
                onValueChange = {
                    PlayerObserver.playSpeed = PlayerObserver.toOneDigits(it)
                },
                onValueChangeFinished = {
                    thread {
                        val playSpeed = PlayerObserver.playSpeed
                        val playType = OpenApiSDK.getPlayerApi().getCurrentSongInfo()?.let {
                            if (it.isLongAudioSong()) {
                                PlaySpeedType.LONG_AUDIO
                            } else {
                                PlaySpeedType.SONG
                            }
                        }
                        playType?.let {
                            val result = OpenApiSDK.getPlayerApi().setPlaySpeed(playSpeed, playType)
                            if (result != PlayDefine.PlayError.PLAY_ERR_NONE) {
                                JobDispatcher.doOnMain {
                                    Toast.makeText(UtilContext.getApp(), "播放失败 Code is $result", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } ?: run {
                            JobDispatcher.doOnMain {
                                Toast.makeText(UtilContext.getApp(), "未知的播放类型", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f, true)
                    .padding(horizontal = 10.dp)
            )

            Text(
                text = "2.0x",
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            text = "伴唱配置: ",
            fontFamily = FontFamily.Monospace
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "进入伴唱，歌曲重新播放：",
                fontFamily = FontFamily.Monospace
            )
            Switch(
                checked = PlayerObserver.vocalAccompanyConfig.replay,
                onCheckedChange = {
                    val vocalAccompanyConfig = VocalAccompanyConfig(PlayerObserver.vocalAccompanyConfig.defaultVocalPercent, it)
                    PlayerObserver.vocalAccompanyConfig = vocalAccompanyConfig
                    OpenApiSDK.getVocalAccompanyApi().saveDefaultVocalAccompanyConfig(vocalAccompanyConfig)
                })
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = PlayerObserver.vocalAccompanyConfig.defaultVocalPercent.value.toFloat(),
                valueRange = VocalPercent.min.value.toFloat()..VocalPercent.max.value.toFloat(),
                onValueChange = {
                    val vocalAccompanyConfig = VocalAccompanyConfig(OpenApiSDK.getVocalAccompanyApi().convertToCloseVocalRadio(it.toInt()), PlayerObserver.vocalAccompanyConfig.replay)
                    PlayerObserver.vocalAccompanyConfig = vocalAccompanyConfig
                    OpenApiSDK.getVocalAccompanyApi().saveDefaultVocalAccompanyConfig(PlayerObserver.vocalAccompanyConfig)
                },
                modifier = Modifier
                    .weight(1f, true)
                    .padding(horizontal = 10.dp)
            )

            Text(
                text = "默认比例: ${PlayerObserver.vocalAccompanyConfig.defaultVocalPercent.value}",
                fontFamily = FontFamily.Monospace
            )
        }

        Divider(thickness = 3.dp, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))

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
        val activity = LocalContext.current as Activity

        Button(
            onClick = {
                if (UiUtils.isStrInt(usage.text) && UiUtils.isStrInt(contentType.text)) {
                    val ret = OpenApiSDK.getPlayerApi().setAudioUsageAndContentType(usage.text.toInt(), contentType.text.toInt())
                    Log.d(TAG, "setAudioUsageAndContentType, ret: $ret")
                } else {
                    UiUtils.showToast("该参数必须输入整数！")
                }
            }
        ) {
            Text("setUsageAndType")
        }
        Divider(thickness = 3.dp, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))

        Text(
            text = "是否支持MasterSR: ${!MasterSRManager.isDeviceNotSupportMasterSRQuality()}, SQSR: ${!SQSRManager.isDeviceNotSupportSQSRQuality()}",
            fontFamily = FontFamily.Monospace
        )
        Divider(modifier = Modifier
            .padding(top = 9.dp)
            .fillMaxWidth()
            .height(3.dp))

        Text(text = "设置音量", modifier = Modifier.padding(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            TextField(
                value = volum,
                label = {
                    Text(text = "输入音量 float")
                },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                onValueChange = {
                    volum = it
                },
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight()
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.padding(10.dp))

            TextField(
                value = volumTransient,
                label = {
                    Text(text = "输入音量 float")
                },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                onValueChange = {
                    volumTransient = it
                },
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight()
                    .fillMaxWidth()
            )
        }

        Button(
            onClick = {
                if (volum.text.toFloatOrNull() != null && volumTransient.text.toFloatOrNull() != null) {
                    val ret = OpenApiSDK.getPlayerApi().setVolume(volum.text.toFloat(), volumTransient.text.toFloat())
                    Log.d(TAG, "setVolume, ret: $ret")
                } else {
                    UiUtils.showToast("该参数必须输入float！")
                }
            }
        ) {
            Text("setVolume")
        }

        Divider(modifier = Modifier
            .padding(top = 9.dp)
            .fillMaxWidth()
            .height(3.dp))

        val fileStrBuilder = StringBuilder()
        GalaxyFileQualityManager.getExcellentFile()?.forEach {
            fileStrBuilder.appendLine(it.name)
            if (it.name.contains("csv")){
                fileStrBuilder.appendLine(it.readText())
            }
        }
        fileStrBuilder.appendLine("md5:${GalaxyFileQualityManager.getExcellentFileMD5()}")
        Text(
            text = "全景声配置: $fileStrBuilder",
            fontFamily = FontFamily.Monospace
        )

        Button(onClick = {
            OpenApiSDK.getPlayerApi().enableNotification(false)
        }, modifier = Modifier.padding(padding)) {
            Text(text = "关闭通知栏")
        }
    }

}