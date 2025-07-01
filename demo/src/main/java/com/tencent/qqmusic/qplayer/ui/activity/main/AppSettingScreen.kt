package com.tencent.qqmusic.qplayer.ui.activity.main

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.tencent.qqmusic.qplayer.BaseFunctionManager
import com.tencent.qqmusic.qplayer.BuildConfig
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.utils.UiUtils
import com.tencent.qqmusic.qplayer.utils.PrivacyManager
import com.tencent.qqmusic.qplayer.utils.PrivacyManager.isGrant


@Composable
@Preview
fun AppSetting() {
    val activity = LocalContext.current as Activity
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.verticalScroll(scrollState)) {
        SingleItem(title = "其他设置页面", item = "") {
            activity.startActivity(Intent(activity, OtherActivity::class.java).apply {
                flags = flags xor Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }

        SingleItem(title = "Debug调试页面", item = "") {
            if (!BuildConfig.IS_DEBUG) {
                UiUtils.showToast("请使用debug包")
                return@SingleItem
            }
            BaseFunctionManager.proxy.gotoDebugActivity(activity = activity)
        }
        val privacyTips = if (!isGrant()) "-协议有更新！" else ""
        SingleItem(title = "关于${privacyTips}", item = "") {
            activity.startActivity(Intent(activity, AboutActivity::class.java))
            PrivacyManager.updateGrantTime()
        }

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

        Image(painter = painterResource(com.tencent.qqmusic.qplayer.base.R.drawable.ic_right_arrow), contentDescription = "", modifier = Modifier.constrainAs(image) {
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
            end.linkTo(parent.end, 10.dp)
        })
    }
}


@Composable
fun MyMultiSelectDialog(
    options: List<String>,
    onOptionsSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        title = { Text(text = "选择应用ID模式") },
        onDismissRequest = onDismissRequest,
        buttons = {},
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable(onClick = onDismissRequest)
            ) {
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




