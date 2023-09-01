package com.tencent.qqmusic.qplayer.ui.activity.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.tencent.qqmusic.innovation.common.util.UtilContext
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.qplayer.ui.activity.MustInitConfig
import com.tencent.qqmusic.sharedfileaccessor.SPBridge


@Composable
@Preview
fun AppSetting() {

    val strict = "严格模式"
    val no_strict = "无检查模式"

    val sharedPreferences: SharedPreferences? = remember {
        try {
            UtilContext.getApp().getSharedPreferences("OpenApiSDKEnv", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            QLog.e("OtherScreen", "getSharedPreferences error e = ${e.message}")
            null
        }
    }
    val activity = LocalContext.current as Activity
    Column {
        SingleItem(title = "测试设置页面", item = "") {
            activity.startActivity(Intent(activity, OtherActivity::class.java).apply {
                flags = flags xor Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
        var showDialog by remember { mutableStateOf(false) }
        
    }
}

@Composable
fun SingleItem(title: String, item: String?, block: () -> Unit) {
    ConstraintLayout(modifier = Modifier
        .clickable {
            block()
        }
        .height(60.dp)
        .fillMaxWidth()) {
        val (host, itemText, image) = createRefs()
        Text(text = title, modifier = Modifier.constrainAs(host) {
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
            start.linkTo(parent.start, 20.dp)
        })

        Text(text = item ?: "", modifier = Modifier.constrainAs(itemText) {
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
            end.linkTo(image.start, 5.dp)
        })

        Image(painter = painterResource(R.drawable.ic_right_arrow), contentDescription = "", modifier = Modifier.constrainAs(image) {
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
            end.linkTo(parent.end, 10.dp)
        })
    }
}


@Composable
fun MyMultiSelectDialog(
    options: List<String>,
    onOptionsSelected: (String) -> Unit
) {
    AlertDialog(
        title = { Text(text = "选择应用ID模式") },
        onDismissRequest = {},
        buttons = {},
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable {
                                onOptionsSelected(option)
                            }
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.body1.merge(),
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                    Divider()
                }
            }
        }
    )
}




