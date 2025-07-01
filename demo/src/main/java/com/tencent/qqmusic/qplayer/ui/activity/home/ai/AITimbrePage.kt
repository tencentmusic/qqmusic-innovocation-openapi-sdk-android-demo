package com.tencent.qqmusic.qplayer.ui.activity.home.ai

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.ai.entity.VocalItem
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.home.ai.cover.AITimbreTAG
import com.tencent.qqmusic.qplayer.utils.UiUtils

@OptIn(ExperimentalCoilApi::class)
@Composable
fun AITimbrePage(
    navController: NavController? = null,
    aiViewModel: AIViewModel = viewModel(),
    onlyPersonal: Boolean = false,
    click: ((VocalItem?) -> Unit)? = null
) {

    val selectVocalItem = remember {
        mutableStateOf<VocalItem?>(null)
    }

    //UI代码
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        Button(onClick = {
            navController?.navigate(AITimbreTAG)
        }) {
            if (aiViewModel.timbreList.value.findLast { it.isPrivate } == null) {
                Text(text = "点击生成个人音色")
            } else {
                Text(text = "重新录制个人音色")
            }
        }

        LazyRow(modifier = Modifier.padding(start = 20.dp, end = 20.dp)) {
            aiViewModel.timbreList.value.filter { if (onlyPersonal) it.isPrivate else true }.forEachIndexed { index, i ->
                item {
                    ConstraintLayout(modifier = Modifier
                        .height(90.dp)
                        .clickable {
                            if (selectVocalItem.value != i) {
                                selectVocalItem.value = i
                            } else {
                                selectVocalItem.value = null
                            }
                            click?.invoke(selectVocalItem.value)
                        }) {
                        val (mark, image, title) = createRefs()
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(60.dp)
                                .constrainAs(image) {
                                    start.linkTo(parent.start)
                                    end.linkTo(parent.end)
                                    top.linkTo(parent.top)
                                }
                        ) {
                            // 主图片（zIndex默认为0）
                            Image(
                                painter = rememberImagePainter(i.iconUrl),
                                contentDescription = "",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )

                            if (i.paymode == 1) {
                                Image(
                                    painter = painterResource(id = R.drawable.need_pay_icon),
                                    contentDescription = "付费图标",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(color = Color.White.copy(alpha = 0.8f))
                                        .align(Alignment.BottomEnd)
                                )
                                if (!i.isUnlocked){
                                    IconButton(modifier = Modifier
                                        .alpha(0.6f)
                                        .fillMaxSize()
                                        .background(
                                            color = Color.White.copy(alpha = 0.7f), // 半透明黑色背景
                                            shape = CircleShape
                                        ), onClick = {
                                        val price = if(i.productInfo.activityId>0) {
                                            i.productInfo.discountPrice
                                        } else {
                                            i.productInfo.oriPrice
                                        }
                                        UiUtils.showToast("价格:${price}")
                                        if (selectVocalItem.value != i) {
                                            selectVocalItem.value = i
                                        } else {
                                            selectVocalItem.value = null
                                        }
                                        click?.invoke(selectVocalItem.value)
                                    }) {
                                        Icon(Icons.Filled.Lock, contentDescription = "解锁")
                                    }
                                }
                            }
                        }

                        Text(text = i.name, modifier = Modifier.constrainAs(title) {
                            top.linkTo(image.bottom, margin = 10.dp)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                        })
                        if (selectVocalItem.value == i) {
                            Box(
                                modifier = Modifier
                                    .width(50.dp)
                                    .height(50.dp)
                                    .zIndex(2f)
                                    .constrainAs(mark) {
                                        top.linkTo(parent.top)
                                        end.linkTo(parent.end)
                                        start.linkTo(parent.start)
                                        bottom.linkTo(parent.bottom)
                                    }) {
                                CheckMark()
                            }
                        }
                    }
                }
            }
        }

    }
    //功能代码
    LaunchedEffect(Unit) {
        aiViewModel.refreshTimbreList()
    }
}


@Composable
private fun CheckMark() {
    Canvas(modifier = Modifier.size(50.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.2f, size.height * 0.5f)
            lineTo(size.width * 0.4f, size.height * 0.7f)
            lineTo(size.width * 0.8f, size.height * 0.3f)
        }
        drawPath(
            path = path,
            color = androidx.compose.ui.graphics.Color.Cyan,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}