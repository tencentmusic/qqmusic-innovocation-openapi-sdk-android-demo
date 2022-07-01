package com.tencent.qqmusic.qplayer.ui.activity.main

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.qplayer.ui.activity.MustInitConfig
import com.tencent.qqmusic.qplayer.ui.activity.OpenApiDemoActivity
import com.tencent.qqmusic.qplayer.ui.activity.SongCacheDemoActivity
import com.tencent.qqmusic.qplayer.ui.activity.TestPlayerActivity
import com.tencent.qqmusic.qplayer.ui.activity.home.HomeViewModel
import com.tencent.qqmusic.qplayer.ui.activity.home.HomePage
import com.tencent.qqmusic.qplayer.ui.activity.login.MinePage
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerActivity
import com.tencent.qqmusic.qplayer.ui.activity.player.qualityToStr

@Composable
fun HomeScreen(categoryViewModel: HomeViewModel = viewModel()) {
    HomePage(categoryViewModel.categories)
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
fun SearchScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.colorPrimaryDark))
            .wrapContentSize(Alignment.Center)
    ) {
        Text(
            text = "Search View",
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
            fontSize = 25.sp
        )
    }
}

@Composable
fun MineScreen() {
    MinePage()
}

@Composable
fun OtherScreen() {
    val activity = LocalContext.current as Activity
    var text by remember { mutableStateOf("当前为${if (OpenApiSDK.isNewProtocol) "新协议" else "旧协议"}") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
        }, modifier = Modifier.padding(16.dp)) {
            Text(text = "日志上传")
        }

        Button(onClick = {
            throw IllegalStateException("test crash")
        }, modifier = Modifier.padding(16.dp)) {
            Text(text = "立即Crash！")
        }

        Button(onClick = {
            activity.startActivity(Intent(activity, OpenApiDemoActivity::class.java))
        }, modifier = Modifier.padding(16.dp)) {
            Text(text = "OpenApi接口")
        }

        Text(text = "当前为${if (OpenApiSDK.isNewProtocol) "新协议" else "旧协议"}")

        Button(onClick = {
            activity.startActivity(Intent(activity, SongCacheDemoActivity::class.java))
        }, modifier = Modifier.padding(16.dp)) {
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
            }, modifier = Modifier.padding(16.dp)) {
                Text(text = "初始化SDK")
            }

            Button(onClick = {
                OpenApiSDK.destroy()
                Toast.makeText(activity, "销毁成功", Toast.LENGTH_SHORT).show()
            }, modifier = Modifier.padding(16.dp)) {
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
        }, modifier = Modifier.padding(16.dp)) {
            Text(text = "进入播放测试界面")
        }

    }
}