package com.tencent.qqmusic.qplayer.ui.activity.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.constraintlayout.compose.ConstraintLayout
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.openapisdk.business_common.event.event.TransactionPushData
import com.tencent.qqmusic.openapisdk.model.vip.CheckNeedRenewalInfo
import com.tencent.qqmusic.qplayer.ui.activity.main.CopyableText
import com.tencent.qqmusic.qplayer.ui.activity.person.MineViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun VIPSuccessDialog(
    data: TransactionPushData?, categoryViewModel: MineViewModel, onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        ConstraintLayout(
            modifier = Modifier
                .width(400.dp)
                .height(400.dp) // 增加高度以避免挤压
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(16.dp) // 增加内边距
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally ) {
                val titleText = if ((data?.vipType ?: 1L) == 1L) "豪华绿钻" else "超级会员"
                CopyableText("购买成功",titleText)
                CopyableText("昵称",categoryViewModel.userInfo.collectAsState().value?.nickName)
                Image(
                    painter = rememberImagePainter(
                        categoryViewModel.userInfo.collectAsState().value?.avatarUrl),
                    contentDescription = "",
                    modifier = Modifier
                        .size(80.dp)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(50))
                )
                val typeText = when (data?.sellType) {
                    1L -> "天"
                    2L -> "月"
                    3L -> "年"
                    else -> ""
                }
                CopyableText("开通时长","${data?.sellNum} $typeText")
                val autoTypeText = when (data?.autoType) {
                    0L -> "非自动续费"
                    1L -> "包月"
                    2L -> "包季"
                    3L -> "包年"
                    else -> data?.autoType.toString()
                }
                CopyableText(title = "续费周期", content = autoTypeText)
                CopyableText(title = "续费类型", content = if (data?.autoFeeFlag == 1L) "首次开通" else "续费")
                CopyableText(title = "支付金额", content = "%.2f ¥".format((data?.price?.toDouble() ?: 0.0) / 100))
                CopyableText(title = "购买单号", content = data?.innerOrderID?.trim())
                CopyableText(
                    title = "购买时间",
                    content = SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()).format(Date((data?.payOrderTime ?: 0) * 1000)))

                Button(
                    onClick = { onDismiss.invoke() },
                    modifier = Modifier.wrapContentSize()
                ) {
                    Text(text = "知道了")
                }
            }
        }
    }
}


@Composable
fun RemindRenewalDialog(renewalInfo: CheckNeedRenewalInfo, setShowDialog: () -> Unit) {
    val renewalType = when (renewalInfo.renewalNoticeType) {
        1 -> {
            "临期"
        }
        2 -> {
            "过期"
        }
        else -> {
            ""
        }
    }
    val textToShow = "续费提醒类型：$renewalType \n" +
            "绿钻需要续费：${renewalInfo.isGreenVipNeedNotice == 1} \n" +
            "超会需要续费：${renewalInfo.isSuperVipNeedNotice == 1}"
    Dialog(onDismissRequest = {
        setShowDialog()
    }) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(15.dp)
        ) {
            Text(text = "该续费VIP咯\n")
            Text(text = textToShow)
            TextButton(onClick = {
                setShowDialog()
            }) {
                Text(text = "知道了")
            }
        }
    }
}

@Preview
@Composable
fun PreviewVIPSuccessDialog(){
    VIPSuccessDialog(
        data = TransactionPushData(
            state = 1, vipType = 1, sellNum = 7, sellType = 1, autoType = 1,
            channelID = "12345779", orderID = "11111111", musicID = "263049787",
            openapiID = "", channelData = "", autoFeeFlag = 1, innerOrderID = "",
            price = "1", payOrderTime = 1638130641),
        categoryViewModel = MineViewModel(), {  })
}