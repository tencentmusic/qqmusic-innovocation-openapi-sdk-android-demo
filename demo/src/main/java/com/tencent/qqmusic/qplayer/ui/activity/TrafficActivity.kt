package com.tencent.qqmusic.qplayer.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TrafficActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TrafficPage()
        }
    }

    @Composable
    fun TrafficPage() {
        val scope = rememberCoroutineScope()
        var sdkTrafficMbToday by remember { mutableStateOf(0f) }
        var sdkTrafficMb3Days by remember { mutableStateOf(0f) }
        var sdkTrafficMbMonth by remember { mutableStateOf(0f) }

        var appTrafficMbToday by remember { mutableStateOf(0f) }
        var appTrafficMb3Days by remember { mutableStateOf(0f) }
        var appTrafficMbMonth by remember { mutableStateOf(0f) }

        Column {
            Text(text = "SDK今天已消耗流量：${sdkTrafficMbToday}MB")
            Text(text = "SDK近3天已消耗流量：${sdkTrafficMb3Days}MB")
            Text(text = "SDK本月已消耗流量：${sdkTrafficMbMonth}MB")

            Text(text = "APP今天已消耗流量：${appTrafficMbToday}MB", modifier = Modifier.padding(top = 20.dp))
            Text(text = "APP近3天已消耗流量：${appTrafficMb3Days}MB")
            Text(text = "APP本月已消耗流量：${appTrafficMbMonth}MB")

            Button(onClick = {
                scope.launch(Dispatchers.IO) {
                    val todayTs = UiUtils.getTimestampsForDaysAgo(0)
                    sdkTrafficMbToday = OpenApiSDK.getTrafficApi().getSDKTrafficMB(todayTs.first, todayTs.second)
                    appTrafficMbToday = OpenApiSDK.getTrafficApi().getAppTrafficMB(todayTs.first, todayTs.second)

                    val threeDaysTs = UiUtils.getTimestampsForDaysAgo(3)
                    sdkTrafficMb3Days = OpenApiSDK.getTrafficApi().getSDKTrafficMB(threeDaysTs.first, threeDaysTs.second)
                    appTrafficMb3Days = OpenApiSDK.getTrafficApi().getAppTrafficMB(threeDaysTs.first, threeDaysTs.second)

                    val monthTs = UiUtils.getTimestampsForDaysAgo(30)
                    sdkTrafficMbMonth = OpenApiSDK.getTrafficApi().getSDKTrafficMB(monthTs.first, monthTs.second)
                    appTrafficMbMonth = OpenApiSDK.getTrafficApi().getAppTrafficMB(monthTs.first, monthTs.second)
                }
            }) {
                Text(text = "刷新")
            }
        }
    }
}