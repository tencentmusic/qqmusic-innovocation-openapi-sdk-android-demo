package com.tencent.qqmusic.qplayer.ui.activity.home.ai.cover

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tencent.qqmusic.ai.entity.AICoverSongCreateType
import com.tencent.qqmusic.ai.entity.Voucher
import com.tencent.qqmusic.ai.entity.VoucherActivity
import com.tencent.qqmusic.ai.entity.VoucherStatus
import com.tencent.qqmusic.qplayer.ui.activity.home.ai.AIViewModel
import com.tencent.qqmusic.qplayer.utils.UiUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AIVoucherPage(backPrePage: () -> Unit) {
    val aiViewModel: AIViewModel = viewModel()

    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            backPrePage.invoke()
        }
    }

    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(key1 = dispatcher) {
        dispatcher?.addCallback(callback)
        onDispose {
            callback.remove() // 移除回调
        }
    }

    var currentTab by remember { mutableStateOf(VoucherTab.NONE) }
    val activityList by aiViewModel.activityList.collectAsState()
    val voucherList by aiViewModel.voucherList.collectAsState()
    var isLoading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp, 0.dp)
    ) {
        // 顶部按钮区域
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    currentTab = VoucherTab.ACTIVITY
                    aiViewModel.getVoucherActivityList()
                },
            ) {
                Text("获取活动列表")
            }

            Button(
                onClick = {
                    currentTab = VoucherTab.VOUCHER
                    aiViewModel.getVoucherList()
                },
            ) {
                Text("已获取券列表")
            }
        }

        // 列表区域
        when (currentTab) {
            VoucherTab.ACTIVITY -> {
                if (activityList.isEmpty()) {
                    Text("暂无活动")
                } else {
                    LazyColumn {
                        items(activityList) { activity ->
                            VoucherActivityItem(
                                activity = activity,
                                onClick = {
                                    if (activity.hasJoin == true) {
                                        UiUtils.showToast("您已参加过此活动")
                                    } else {
                                        aiViewModel.collectVoucher(activity)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            VoucherTab.VOUCHER -> {
                if (voucherList.isEmpty()) {
                    Text("暂无获取的优惠券")
                } else {
                    LaunchedEffect(listState) {
                        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                            .collect { lastVisibleItemIndex ->
                                if (lastVisibleItemIndex == voucherList.size - 1 && !isLoading && aiViewModel.voucherListHasMore.value) {
                                    // Load more items
                                    isLoading = true
                                    aiViewModel.getVoucherList()
                                }
                            }
                    }
                    LazyColumn {
                        items(voucherList) { voucher ->
                            VoucherItem(
                                voucher = voucher
                            )
                        }
                        if(isLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            else -> {}
        }
    }

}

// Tab枚举类
private enum class VoucherTab {
    ACTIVITY, VOUCHER, NONE
}

@Composable
fun VoucherActivityItem(
    activity: VoucherActivity,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 活动名称和标签
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activity.name ?: "",
                        fontWeight = FontWeight.Bold
                    )

                    activity.label?.let { label ->
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 活动描述
                activity.desc?.let { desc ->
                    Text(
                        text = desc,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 可领取券数量
                activity.vouchers?.let { vouchers ->
                    if (vouchers.isNotEmpty()) {
                        vouchers.forEachIndexed { index, ticketInfo ->
                            Text(
                                text = "优惠券${index+1},可领取: ${ticketInfo.num}, 发放总数:${ticketInfo.total_num}",
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 活动时间
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = buildString {
                            activity.beginTime?.let { begin ->
                                append(formatTime(begin))
                            }
                            append(" - ")
                            activity.endTime?.let { end ->
                                append(formatTime(end))
                            }
                        },
                    )
                }

                // 参与状态
                activity.hasJoin?.let { hasJoin ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (hasJoin) "已参与" else "未参与",
                    )
                }
            }
            if (activity.hasJoin != true) {
                Button(
                    onClick = onClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Text(text = "立即参与")
                }
            }
        }
    }
}

// 辅助函数：格式化时间戳
private fun formatTime(timestamp: Long): String {
    val date = Date(timestamp * 1000)
    val format = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
    return format.format(date)
}

@Composable
fun VoucherItem(
    voucher: Voucher,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 兑换券类型和状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 兑换券类型
                Text(
                    text = when (voucher.voucherType) {
                        AICoverSongCreateType.SEG.voucherType -> "片段制作券"
                        AICoverSongCreateType.FULL_SONG.voucherType -> "全曲制作券"
                        AICoverSongCreateType.PRO.voucherType -> "专业制作券"
                        AICoverSongCreateType.PERFECT.voucherType -> "至臻制作券"
                        else -> "未知类型"
                    },
                    fontWeight = FontWeight.Bold
                )

                // 兑换券状态
                Surface(
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        text = when (voucher.voucherStatus) {
                            VoucherStatus.Voucher_Invalid.value -> "已失效"
                            VoucherStatus.Voucher_Issued.value -> "待领取"
                            VoucherStatus.Voucher_Claimed.value -> "待使用"
                            VoucherStatus.Voucher_Redeemed.value -> "已使用"
                            else -> "未知状态"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 有效期
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = buildString {
                        append("有效期：")
                        voucher.redeemableTimeStart?.let { start ->
                            append(formatTime(start))
                        }
                        append(" - ")
                        voucher.redeemableTimeEnd?.let { end ->
                            append(formatTime(end))
                        }
                    },
                )
            }

            // 如果已使用，显示使用时间
            voucher.redeemTime?.let { redeemTime ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "使用时间：${formatTime(redeemTime)}",
                    )
                }
            }

            // 金额信息
            voucher.amount?.let { amount ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "金额：${amount.toFloat() / 100}元",
                )
            }
        }
    }
}