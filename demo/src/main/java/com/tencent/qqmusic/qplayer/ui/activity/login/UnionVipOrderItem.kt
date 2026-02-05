package com.tencent.qqmusic.qplayer.ui.activity.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tencent.qqmusic.openapisdk.model.vip.UnionVipOrderInfo
import com.tencent.qqmusic.openapisdk.model.vip.UnionVipOrderItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 订单项组件
 * @param orderInfo 订单信息
 * @param onOrderClick 订单点击回调
 * @param modifier 修饰符
 */
@Composable
fun UnionVipOrderItem(
    orderInfo: UnionVipOrderInfo,
    onOrderClick: (UnionVipOrderInfo) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOrderClick(orderInfo) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 订单头部信息
            OrderHeader(orderInfo)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 订单商品列表
            OrderItemsList(orderInfo.items)
        }
    }
}

/**
 * 订单头部信息
 */
@Composable
private fun OrderHeader(orderInfo: UnionVipOrderInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "订单号: ${orderInfo.id ?: "未知"}",
                color = Color.Gray,
                fontSize = 12.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "创建时间 " + (orderInfo.createTime?.let { formatTimestamp(it) } ?: "未知时间"),
                color = Color.LightGray,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "支付时间 " + (orderInfo.payTime?.let { formatTimestamp(it) } ?: "未知时间"),
                color = Color.LightGray,
                fontSize = 11.sp
            )
        }
        
        // 订单状态标签
        OrderStatusBadge(orderInfo.state, orderInfo.stateDesc)
    }
}

/**
 * 订单状态标签
 */
@Composable
private fun OrderStatusBadge(state: String?, stateDesc: String?) {
    val (backgroundColor, textColor) = when (state) {
        "created" -> Color(0xFFFFF2E8) to Color(0xFFFF6B35) // 已下单 - 橙色
        "cancel" -> Color(0xFFF5F5F5) to Color(0xFF666666) // 取消订单 - 灰色
        "pre_paid", "paid" -> Color(0xFFE8F4FF) to Color(0xFF1890FF) // 中间状态 - 蓝色
        "shipped" -> Color(0xFFE8F7F0) to Color(0xFF00A854) // 已发货 - 绿色
        "finish" -> Color(0xFFF0F5FF) to Color(0xFF722ED1) // 已完成 - 紫色
        "refund" -> Color(0xFFFFF0F0) to Color(0xFFFF4D4F) // 已退款 - 红色
        else -> Color(0xFFF5F5F5) to Color(0xFF666666) // 默认 - 灰色
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .border(1.dp, textColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = stateDesc ?: getStatusText(state) ?: "未知状态",
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 订单商品列表
 */
@Composable
private fun OrderItemsList(items: List<UnionVipOrderItem>?) {
    items?.takeIf { it.isNotEmpty() }?.let { itemList ->
        Column {
            Text(
                text = "商品清单",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            itemList.forEachIndexed { index, item ->
                OrderItemRow(item)
                if (index < itemList.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * 单个商品项
 */
@Composable
private fun OrderItemRow(item: UnionVipOrderItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F9FA), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 商品图标
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFE8F4FF), RoundedCornerShape(6.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // 这里可以使用实际的商品图标
            Text(
                text = "VIP",
                color = Color(0xFF1890FF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 商品信息
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.name ?: "未知商品",
                color = Color.Black,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "数量: ${item.quantity ?: 1}",
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
        
        // 商品金额
        Text(
            text = "¥${formatAmount(item.subtotalAmount)}",
            color = Color.Black,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 格式化时间戳
 */
private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp * 1000L)
    val formatter = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.getDefault())
    return formatter.format(date)
}

/**
 * 格式化金额（分转元）
 */
private fun formatAmount(amount: Int?): String {
    return if (amount != null) {
        String.format("%.2f", amount / 100.0)
    } else {
        "0.00"
    }
}

/**
 * 获取状态文本
 */
private fun getStatusText(state: String?): String? {
    return when (state) {
        "created" -> "已下单"
        "cancel" -> "已取消"
        "pre_paid" -> "待支付"
        "paid" -> "已支付"
        "shipped" -> "已发货"
        "finish" -> "已完成"
        "refund" -> "已退款"
        else -> null
    }
}