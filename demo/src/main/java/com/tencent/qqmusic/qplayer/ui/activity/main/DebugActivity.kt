package com.tencent.qqmusic.qplayer.ui.activity.main

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tencent.qqmusic.innovation.common.util.DeviceUtils
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.qplayer.ui.activity.MustInitConfig

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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {




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