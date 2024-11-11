package com.tencent.qqmusic.qplayer.ui.activity.home.ai.cover

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.ai.entity.AICoverCreatePayType
import com.tencent.qqmusic.ai.entity.AICoverDataInfo
import com.tencent.qqmusic.ai.entity.AICoverSongCreateType
import com.tencent.qqmusic.ai.entity.MidProduce
import com.tencent.qqmusic.ai.entity.ReferenceAudioType
import com.tencent.qqmusic.ai.entity.VoucherGetStatus
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine.PlayState
import com.tencent.qqmusic.qplayer.core.voiceplay.AICoverLinkPlayer
import com.tencent.qqmusic.qplayer.ui.activity.home.ai.AIViewModel
import com.tencent.qqmusic.qplayer.utils.UiUtils
import com.tencent.qqmusic.qzdownloader.downloader.DownloadResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalCoilApi::class)
@Composable
fun AiCoverBuyPage(dataInfo: AICoverDataInfo?, backPrePage: () -> Unit) {
    val aiViewModel: AIViewModel = viewModel()
    val scope = rememberCoroutineScope()

    val orderId = remember { mutableStateOf("") }

    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            backPrePage.invoke()
            remove()
        }
    }

    val midProduce = remember {
        mutableStateOf<AICoverSongCreateType>(AICoverSongCreateType.SEG)
    }

    val paytype = remember {
        mutableStateOf(AICoverCreatePayType.CASH)
    }

    val voucherId = remember {
        mutableStateOf<String?>("")
    }

    val taskIds = remember {
        mutableStateOf("")
    }

    val makeStatus = remember {
        mutableStateOf(0)
    }

    val queryNum = remember { mutableStateOf(0) }

    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(key1 = dispatcher) {
        dispatcher?.addCallback(callback)
        onDispose {
            callback.remove() // 移除回调
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            aiViewModel.stopPlayCoverLink()
        }
    }


    LaunchedEffect(midProduce.value) {
        aiViewModel.getVoucherInfo(VoucherGetStatus.NOT_USE)
    }



    DisposableEffect(taskIds.value) {
        val job = scope.launch(Dispatchers.IO) {
            val type = midProduce.value
            var i = 0
            while (i < 300) {
                aiViewModel.getCoverTaskStatus(taskIds.value, type) {
                    makeStatus.value = it ?: 0
                }
                delay(1000)
                i++
            }
        }
        onDispose {
            job.cancel()
        }
    }

    DisposableEffect(orderId.value) {
        val job = scope.launch(Dispatchers.IO) {
            var i = 0
            while (i < 300) {
                queryNum.value = i
                aiViewModel.fetchAiCoverBuyStatue(orderId.value)
                delay(1000)
                i++
            }
        }
        onDispose {
            job.cancel()
        }
    }


    val image = remember {
        mutableStateOf(ImageBitmap(1, 1))
    }

    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (img, songName, singer, buyCover, payStatus, segs) = createRefs()
        Image(
            painter = rememberImagePainter(dataInfo?.accInfo?.cover),
            "",
            modifier = Modifier
                .width(60.dp)
                .height(60.dp)
                .zIndex(1f)
                .clip(CircleShape)
                .constrainAs(img) {
                    start.linkTo(parent.start, margin = 10.dp)
                    top.linkTo(parent.top)
                }
        )
        Text(dataInfo?.accInfo?.songName ?: "", modifier = Modifier.constrainAs(songName) {
            top.linkTo(img.top)
            start.linkTo(img.end, margin = 10.dp)
            bottom.linkTo(singer.top)
        })
        Text(dataInfo?.accInfo?.singer ?: "", modifier = Modifier.constrainAs(singer) {
            top.linkTo(songName.bottom)
            start.linkTo(img.end, margin = 10.dp)
            bottom.linkTo(img.bottom)
        })
        Column(modifier = Modifier.constrainAs(segs) {
            top.linkTo(img.bottom)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
        }) {
            Row {
                if (ReferenceAudioType.isTypeAvailable(dataInfo?.supportTypeMask ?: 0, ReferenceAudioType.ReferenceAudioTypeSegment)) {
                    SegPage(dataInfo, dataInfo?.accInfo?.segMidProduct, AICoverSongCreateType.SEG, midProduce.value.createType) {
                        midProduce.value = it
                    }
                }
                if (ReferenceAudioType.isTypeAvailable(dataInfo?.supportTypeMask ?: 0, ReferenceAudioType.ReferenceAudioTypeFull)) {
                    SegPage(dataInfo, dataInfo?.accInfo?.noSegMidProduct, AICoverSongCreateType.FULL_SONG, midProduce.value.createType) {
                        midProduce.value = it
                    }
                }
            }

            Row {
                if (ReferenceAudioType.isTypeAvailable(dataInfo?.supportTypeMask ?: 0, ReferenceAudioType.ReferenceAudioTypePro)) {
                    SegPage(dataInfo, dataInfo?.accInfo?.proMidProduct, AICoverSongCreateType.PRO, midProduce.value.createType) {
                        midProduce.value = it
                    }
                }
                if (ReferenceAudioType.isTypeAvailable(dataInfo?.supportTypeMask ?: 0, ReferenceAudioType.ReferenceAudioTypePerfect)) {

                    SegPage(dataInfo, dataInfo?.accInfo?.perfectMidProduct, AICoverSongCreateType.PERFECT, midProduce.value.createType) {
                        midProduce.value = it
                    }
                }
            }
            if ((aiViewModel.vocher.value?.makeRights?.segRemainCnt ?: 0) > 0) {
                Button(onClick = {
                    if (paytype.value == AICoverCreatePayType.RIGHT) {
                        paytype.value = AICoverCreatePayType.CASH
                    } else {
                        paytype.value = AICoverCreatePayType.RIGHT
                    }
                }) {
                    Text("片段权益数量 ${aiViewModel.vocher.value?.makeRights?.segRemainCnt ?: 0}")
                }
            }

            if ((aiViewModel.vocher.value?.makeRights?.fullRemainCnt ?: 0) > 0) {
                Button(onClick = {
                    if (paytype.value == AICoverCreatePayType.RIGHT) {
                        paytype.value = AICoverCreatePayType.CASH
                    } else {
                        paytype.value = AICoverCreatePayType.RIGHT
                    }
                }) {
                    Text("全曲权益数量 ${aiViewModel.vocher.value?.makeRights?.fullRemainCnt ?: 0}")
                }
            }

            if ((aiViewModel.vocher.value?.makeRights?.proRemainCnt ?: 0) > 0) {
                Button(onClick = {
                    if (paytype.value == AICoverCreatePayType.RIGHT) {
                        paytype.value = AICoverCreatePayType.CASH
                    } else {
                        paytype.value = AICoverCreatePayType.RIGHT
                    }
                }) {
                    Text("专业权益数量 ${aiViewModel.vocher.value?.makeRights?.proRemainCnt ?: 0}")
                }
            }

            if ((aiViewModel.vocher.value?.makeRights?.perfectNum ?: 0) > 0) {
                Button(onClick = {
                    if (paytype.value == AICoverCreatePayType.RIGHT) {
                        paytype.value = AICoverCreatePayType.CASH
                    } else {
                        paytype.value = AICoverCreatePayType.RIGHT
                    }
                }) {
                    Text("至臻权益数量 ${aiViewModel.vocher.value?.makeRights?.perfectNum ?: 0}")
                }
            }

            LazyRow {
                aiViewModel.vocher.value?.vouchers?.forEach { voucher ->
                    item {
                        val color = if (voucherId.value == voucher.voucherId && paytype.value == AICoverCreatePayType.COUPON) {
                            Color(0xFF1AAD19)
                        } else {
                            Color(0xFF999999)
                        }

                        Box(modifier = Modifier
                            .background(color)
                            .clickable {
                                paytype.value = AICoverCreatePayType.COUPON
                                voucherId.value = voucher.voucherId
                            }
                            .padding(10.dp)) {
                            Column {
                                when (voucher.voucherType) {
                                    AICoverSongCreateType.SEG.voucherType -> {
                                        Text("片段权益")
                                    }

                                    AICoverSongCreateType.FULL_SONG.voucherType -> {
                                        Text("全曲权益")
                                    }

                                    AICoverSongCreateType.PRO.voucherType -> {
                                        Text("专业权益")
                                    }

                                    AICoverSongCreateType.PERFECT.voucherType -> {
                                        Text("至臻权益")
                                    }
                                }
                                Text("单张价格 : ${(voucher.amount ?: 0) / 1000} 元")
                            }
                        }
                    }
                }
            }
            if (paytype.value == AICoverCreatePayType.RIGHT || paytype.value == AICoverCreatePayType.COUPON) {
                Button({
                    paytype.value = AICoverCreatePayType.CASH
                }) {
                    Text("正在使用权益/券，点击改为现金支付")
                }
            } else {
                val num = when (midProduce.value) {
                    AICoverSongCreateType.SEG -> {
                        Pair(dataInfo?.accInfo?.segMidProduct?.oriPrice, dataInfo?.accInfo?.segMidProduct?.discountPrice)
                    }

                    AICoverSongCreateType.FULL_SONG -> {
                        Pair(dataInfo?.accInfo?.noSegMidProduct?.oriPrice, dataInfo?.accInfo?.noSegMidProduct?.discountPrice)
                    }

                    AICoverSongCreateType.PRO -> {
                        Pair(dataInfo?.accInfo?.proMidProduct?.oriPrice, dataInfo?.accInfo?.proMidProduct?.discountPrice)
                    }

                    AICoverSongCreateType.PERFECT -> {
                        Pair(dataInfo?.accInfo?.perfectMidProduct?.oriPrice, dataInfo?.accInfo?.perfectMidProduct?.discountPrice)
                    }

                    else -> {
                        Pair(0, 0)
                    }
                }

                Text(" 原价 ${(num.first ?: 0) / 1000}  需支付 ${(num.second ?: 0) / 1000} 元")
            }
        }
        val isRebuild = when (midProduce.value) {
            AICoverSongCreateType.SEG -> {
                dataInfo?.makeInfo?.userSegUgcId?.isNotEmpty() ?: false
            }

            AICoverSongCreateType.FULL_SONG -> {
                dataInfo?.makeInfo?.userUgcId?.isNotEmpty() ?: false
            }

            AICoverSongCreateType.PRO -> {
                dataInfo?.makeInfo?.userProUgcId?.isNotEmpty() ?: false
            }

            AICoverSongCreateType.PERFECT -> {
                dataInfo?.makeInfo?.userPerfectUgcId?.isNotEmpty() ?: false
            }

            else -> {
                false
            }
        }
        if (isRebuild) {
            paytype.value = AICoverCreatePayType.REBUILD
        }
        LaunchedEffect(midProduce.value) {
            if (paytype.value == AICoverCreatePayType.CASH) {
                aiViewModel.fetchAICoverWorkResp(
                    dataInfo?.accInfo?.songMid ?: "",
                    midProduce.value,
                    0,
                    paytype.value,
                    voucherId.value
                ) { oId, tId ->
                    orderId.value = oId ?: ""
                    taskIds.value = tId ?: ""
                    image.value = UiUtils.generateQRCode(aiViewModel.payUrl.value ?: "")?.asImageBitmap() ?: ImageBitmap(1, 1)
                }
            }
        }
        if (isRebuild) {
            Button(modifier = Modifier.constrainAs(buyCover) {
                top.linkTo(segs.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(parent.bottom)
            }, onClick = {
                aiViewModel.fetchAICoverWorkResp(
                    dataInfo?.accInfo?.songMid ?: "",
                    midProduce.value,
                    0,
                    AICoverCreatePayType.REBUILD,
                    voucherId.value
                ) { oId, tId ->
                    orderId.value = oId ?: ""
                    taskIds.value = tId ?: ""
                }

            }) {
                Text("重新生成")
            }
        } else {
            if (paytype.value == AICoverCreatePayType.COUPON || paytype.value == AICoverCreatePayType.RIGHT) {
                Button(modifier = Modifier.constrainAs(buyCover) {
                    top.linkTo(segs.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                }, onClick = {
                    aiViewModel.fetchAICoverWorkResp(
                        dataInfo?.accInfo?.songMid ?: "",
                        midProduce.value,
                        0,
                        paytype.value,
                        voucherId.value
                    ) { oId, tId ->
                        orderId.value = oId ?: ""
                        taskIds.value = tId ?: ""
                    }

                }) {
                    Text("使用券生成")
                }
            } else {
                Image(
                    bitmap = image.value,
                    null,
                    modifier = Modifier.constrainAs(buyCover) {
                        top.linkTo(segs.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                    }
                )
            }
        }
        Column(modifier = Modifier.constrainAs(payStatus) {
            top.linkTo(buyCover.bottom)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
            bottom.linkTo(parent.bottom)
        }) {
            Text("${aiViewModel.payStatues.value ?: ""} 查询次数 ${queryNum.value}")
            val status = when (makeStatus.value) {
                0 -> "未生成"
                1 -> "生成中"
                2 -> "生成成功"
                3 -> "生成失败"
                else -> "未知"
            }
            Text("作品生成状态 $status")

        }
    }

}

@Composable
private fun SegPage(
    aiCoverDataInfo: AICoverDataInfo?,
    data: MidProduce?,
    type: AICoverSongCreateType,
    selectType: Int?,
    callback: (AICoverSongCreateType) -> Unit
) {
    val aiViewModel: AIViewModel = viewModel()
    val scope = rememberCoroutineScope()
    data ?: return
    val text = remember { mutableStateOf("试听") }

    val color = if (selectType == type.createType) {
        Color(0xFF1AAD19)
    } else {
        Color(0xFF999999)
    }

    Column(modifier = Modifier
        .background(color)
        .clickable {
            callback(type)
        }) {
        Row {
            Text(data.name ?: "")
            Text(data.tag ?: "")
        }
        Row {
            Text(data.subText ?: "")
            Button({
                scope.launch(Dispatchers.IO) {
                    var audioStatus = -1
                    while (audioStatus == -1 || audioStatus == 2) {
                        aiViewModel.getTryLink(aiCoverDataInfo?.accInfo?.songMid ?: "", type) {
                            audioStatus = it.audioStatus ?: -1
                            if (audioStatus == 3) {
                                aiViewModel.playLink(it.audioLink ?: "", null, null, object : AICoverLinkPlayer.PlayEventListener {
                                    override fun onDownloadFailed(url: String?, result: DownloadResult?) {

                                    }

                                    override fun onDownloadSucceed(url: String?, result: DownloadResult?) {

                                    }

                                    override fun onDownloadProgress(url: String?, totalSize: Long, downSize: Long, writeSize: Long) {

                                    }

                                    override fun onPlayStateChange(state: Int) {
                                        if (state == PlayState.MEDIAPLAYER_STATE_STARTED) {
                                            text.value = "试听中"
                                        } else if (state == PlayState.MEDIAPLAYER_STATE_STOPPED || state == PlayState.MEDIAPLAYER_STATE_PAUSED || state == PlayState.MEDIAPLAYER_STATE_PLAYBACKCOMPLETED) {
                                            text.value = "试听"
                                        }
                                    }

                                })
                            } else {
                                text.value = "生成中"
                            }
                        }
                        delay(1000)
                    }
                }
            }) {
                Text(text.value ?: "")
            }
        }
    }

}

