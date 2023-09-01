package com.tencent.qqmusic.qplayer.ui.activity.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.constraintlayout.compose.ConstraintLayout
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.openapisdk.business_common.event.event.TransactionPushData
import java.text.SimpleDateFormat
import java.util.*


@Composable
fun VIPSuccessDialog(
    data: TransactionPushData?, categoryViewModel: HomeViewModel, onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        ConstraintLayout(
            modifier = Modifier
                .width(400.dp)
                .height(300.dp)
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(10.dp)
        ) {
            val (title, name, avatar, time, paytype, orderid, paytime, exit) = createRefs()
            val titleText = if ((data?.vipType ?: 1L) == 1L) "豪华绿钻" else "超级会员"
            Text(text = "$titleText 购买成功", modifier = Modifier.constrainAs(title) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            })

            Text(text = categoryViewModel.userInfo.value?.nickName ?: "", modifier = Modifier.constrainAs(name) {
                top.linkTo(title.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            })
            Image(painter = rememberImagePainter(categoryViewModel.userInfo.value?.avatarUrl),
                contentDescription = "",
                modifier = Modifier
                    .size(100.dp)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(50))
                    .constrainAs(avatar) {
                        top.linkTo(name.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    })

            val typeText = when (data?.sellType) {
                1L -> "天"
                2L -> "月"
                3L -> "年"
                else -> ""
            }
            Text(text = "开通时长: ${data?.sellNum} ${typeText}", modifier = Modifier.constrainAs(time) {
                top.linkTo(avatar.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            })
            val autoTypeText = when (data?.autoType) {
                0L -> "非自动续费"
                1L -> "包月"
                2L -> "包季"
                3L -> "包年"
                else -> ""
            }


            Text(text = "续费类型: ${if (data?.autoFeeFlag == 1L) "首次开通" else "续费"}", modifier = Modifier.constrainAs(paytype) {
                top.linkTo(time.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            })

            Text(text = "购买单号: ${data?.innerOrderID?.trim()}", modifier = Modifier.constrainAs(orderid) {
                top.linkTo(paytype.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            })

            Text(text = "购买时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date((data?.payOrderTime ?: 0) * 1000))}",
                modifier = Modifier.constrainAs(paytime) {
                    top.linkTo(orderid.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                })

            Button(onClick = { onDismiss.invoke() }, modifier = Modifier.constrainAs(exit) {
                top.linkTo(paytime.bottom)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }) {
                Text(text = "知道了")
            }
        }
    }
}