package com.tencent.qqmusic.qplayer.ui.activity.person

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter


@Composable
fun MinePageNew(model: MineViewModel) {
    model.updateData()
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .padding(start = 0.dp, bottom = 0.dp, top = 20.dp, end = 0.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20))
                .background(Color(0xFFF0FFF0))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 0.dp, vertical = 10.dp)) {
                Image(
                    painter = rememberImagePainter(model.userInfo.collectAsState().value?.avatarUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(50))
                )
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 0.dp), verticalArrangement = Arrangement.Center) {
                    val vip = model.userVipInfo.collectAsState().value
                    val vipText = model.getVipText(vip)
                    val vipLevelText = "会员 Level : ${vip?.vipLevel}级"
                    Text(text = "用户昵称：${model.userInfo.collectAsState().value?.nickName ?: " "}")
                    Text(text = vipLevelText)
                    Text(text = vipText)
                    if (vip?.isVip() == true || vip?.isLongAudioVip() == true) {
                        Text(text = "开始时间：${model.getVipTimeStartText(vip)}")
                        Text(text = "结束时间：${model.getVipEndTimeText(vip)}")
                    }
                    Text(text = "登录方式：${model.getLoginType(model.loginInfo.collectAsState().value)}")
                    val partnerIdInfo = model.partnerAccountInfo.collectAsState().value ?: ""
                    if (partnerIdInfo.isNullOrEmpty().not()) {
                        Text(text = "已登录第三方帐号 id：$partnerIdInfo")
                    }
//                    Text(text = "是否高潜用户:${model.limitFree.collectAsState().value?.data?.payHighDive?:"null"}")
//                    Text(text = "限免:${model.getLimitFreeInfo(model.limitFree.collectAsState().value)}")
                }
            }
        }
    }
}