package com.tencent.qqmusic.qplayer.ui.activity.person

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.qplayer.ui.activity.mv.MvBuyQRDialog.showTextDialog
import com.tencent.qqmusic.qplayer.utils.UiUtils.getSuperQualityTypeName
import com.tencent.qqmusic.qplayer.utils.getAllSuperQualityList


@OptIn(ExperimentalCoilApi::class)
@Composable
fun MinePageNew(model: MineViewModel) {
    model.updateData()
    val sheetState = remember { mutableStateOf(false) }
    val context: AppCompatActivity = LocalContext.current as AppCompatActivity
    val vip = model.userVipInfo.collectAsState().value
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
                        .clickable{
                            var msg = ""
                            getAllSuperQualityList().forEach { type ->
                                if (type>0){
                                    msg += "${getSuperQualityTypeName(type)}:\n" +
                                            "可试听=${vip?.getProfitInfoByType(type)?.canTry()}," +
                                            "试听过=${vip?.isProfitTriedByType(type)}," +
                                            "剩余时间=${vip?.getProfitInfoByType(type)?.remainTime}s\n"
                                }
                            }
                            showTextDialog(context = context, title = "用户权益", message = msg)
                        }
                )
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 0.dp), verticalArrangement = Arrangement.Center) {
                    val isVip = vip?.isVip() ?: false
                    val vipText = model.getVipText(vip).joinToString("\n")
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

                    if (vip != null) {
                        OutlinedButton(
                            onClick = {
                                sheetState.value = true
                                showCashier(activity = context, model)
                            }
                        ) {
                            val text = if (isVip) {
                                "立即续费"
                            } else {
                                "开通会员"
                            }
                            Text(text = text)
                        }
                    }
                }
            }
        }
    }
}

fun showCashier(activity: AppCompatActivity, model: MineViewModel) {
    val fgr = activity.supportFragmentManager
    CashierDialog().show(fgr, "CashierDialog")
}