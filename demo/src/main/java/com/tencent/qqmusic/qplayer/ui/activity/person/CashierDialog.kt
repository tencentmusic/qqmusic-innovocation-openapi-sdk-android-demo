package com.tencent.qqmusic.qplayer.ui.activity.person

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.OutlinedButton
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.vip.CashierCreateOrderItem
import com.tencent.qqmusic.openapisdk.model.vip.CashierCreateOrderParams
import com.tencent.qqmusic.openapisdk.model.vip.CashierGears
import com.tencent.qqmusic.openapisdk.model.vip.CashierOffer
import com.tencent.qqmusic.openapisdk.model.vip.CashierOfferProduct
import com.tencent.qqmusic.openapisdk.model.vip.CashierOrderInfo
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Create by tinguo on 2025/6/26
 * CopyWrite (c) 2025 TME. All rights reserved.
 */
@Suppress("IMPLICIT_CAST_TO_ANY")
class CashierDialog: BottomSheetDialogFragment() {

    private val viewModel by lazy {
        ViewModelProvider(this)[CashierViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val composeView = ComposeView(requireContext())
        composeView.setContent {
            CashierMainView(viewModel = viewModel)
        }
        return composeView
    }

    override fun onStart() {
        super.onStart()

        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            // 设置弹窗默认高度
            // behavior.peekHeight = 200.dp.value.toInt()

            // 如果想让弹窗一开始就是展开状态
            behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED

            // 设置弹窗背景
            it.background = ContextCompat.getDrawable(requireContext(), R.drawable.bottom_sheet_background)
        }
    }

    @Composable
    fun CashierMainView(modifier: Modifier = Modifier.fillMaxSize(), viewModel: CashierViewModel) {
        val cashierState = viewModel.cashierState.collectAsState()
        val msgState = viewModel.msg
        val cashierGearsState = viewModel.cashierGears.collectAsState()

        LaunchedEffect(Unit) {
            viewModel.fetchCashierGears()
        }

        androidx.compose.material.Surface(
            modifier,
            shape = RoundedCornerShape(8.dp),
            elevation = 4.dp
        ) {
            Column(verticalArrangement = Arrangement.Top)  {
                Spacer(modifier = Modifier.height(10.dp))

                if (msgState.value != null) {
                    Text(text = msgState.value ?: "", modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(10.dp))
                }

                when (cashierState.value) {
                    CashierViewModel.STATE_LOADING -> {
                        Row(modifier = Modifier.fillMaxWidth().wrapContentHeight(), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 4.dp
                            )
                        }
                    }
                    CashierViewModel.STATE_LOAD_SUCCESS -> {
                        if (cashierGearsState.value != null) {
                            CashierGearsView(cashierGearsState.value!!)
                        }
                    }
                    CashierViewModel.STATE_LOAD_FAIL -> {
                        OutlinedButton(
                            onClick = {
                                viewModel.fetchCashierGears()
                            }
                        ) {
                            Text(text = "加载失败, 点击重试", modifier = Modifier.wrapContentSize())
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    @Composable
    fun CashierGearsView(cashierGears: CashierGears) {
        val tabs = cashierGears.tabs ?: return
        val pagerState = rememberPagerState(initialPage = 0)
        val scope = rememberCoroutineScope()
        ScrollableTabRow(pagerState.currentPage, indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
            )
        }, backgroundColor = Color.Transparent) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    text = {
                        val category = when (tab.groups?.firstOrNull()?.offers?.firstOrNull()?.categoryTag) {
                            "hhlz" -> "豪华绿钻"
                            "cjhy" -> "超级会员"
                            else -> ""
                        }
                        Text(text = tab.tabInfo?.title ?: category)
                    },
                    selected = pagerState.currentPage == index,
                    onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                })
            }
        }
        HorizontalPager(tabs.size, itemSpacing = 10.dp, state = pagerState, modifier = Modifier.fillMaxSize()) { page->
            val cashierTab = tabs[page]
            val offers = cashierTab.groups?.mapNotNull { group->
                group.offers
            }?.flatten() ?: emptyList()

            LaunchedEffect(page) {
                val offer = offers.firstOrNull() ?: return@LaunchedEffect
                viewModel.fetchCashierOrder(offer.id, offer.product!!, amount = 1)
            }

            Column(verticalArrangement = Arrangement.Top, modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    items(offers) { offer->
                        CashierGearsItemView(offer, viewModel = viewModel)
                    }

                    item {
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                CashierOrderView(viewModel = viewModel)
            }
        }
    }

    @Composable
    fun CashierOrderView(viewModel: CashierViewModel) {
        val createOrderState = viewModel.cashierCreateOrderState.collectAsState()
        val orderState = viewModel.cashierOrderState.collectAsState()
        val orderPayState = viewModel.cashierOrderPayState.collectAsState()
        val scope = rememberCoroutineScope()
        val bitmapState = remember { mutableStateOf<Bitmap?>(null) }

        when (createOrderState.value) {
            CashierViewModel.STATE_LOADING -> {
                Row(modifier = Modifier.fillMaxWidth().wrapContentHeight(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 4.dp
                    )
                }
            }
            CashierViewModel.STATE_LOAD_SUCCESS -> {
                val orderInfo = orderState.value?.second
                if (orderInfo != null) {
                    LaunchedEffect(orderInfo.orderId) {
                        scope.launch(Dispatchers.IO) {
                            val bitmap = UiUtils.generateQRCode(orderInfo.payUrl)
                            bitmapState.value = bitmap
                        }
                    }
                    val bitmap = bitmapState.value
                    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    if (bitmap != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(modifier = Modifier.fillMaxWidth().wrapContentHeight(), horizontalArrangement = Arrangement.Center) {
                                Image(bitmap = bitmap.asImageBitmap(), contentDescription = "二维码", modifier = Modifier.size(200.dp))
                            }
                            Column(horizontalAlignment = Alignment.Start) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(text = "订单号    ${orderInfo.orderId}")
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "过期时间  ${formatter.format(Date(orderInfo.expireAt * 1000L))}")
                                Spacer(modifier = Modifier.height(4.dp))
                                val payStatus = when (orderPayState.value) {
                                    -1 -> "加载失败"
                                    0 -> "待支付"
                                    1 -> "已支付"
                                    2 -> "已过期"
                                    else -> "未知状态"
                                }
                                Text(text = "支付状态  $payStatus")
                            }
                        }
                        DisposableEffect(orderInfo.orderId) {
                            val job = scope.launch {
                                viewModel.pollCashierOrderStatus(orderInfo.orderId)
                                while (orderPayState.value == 0 || orderPayState.value == -1) {
                                    delay(1000)
                                    viewModel.pollCashierOrderStatus(orderInfo.orderId)
                                }
                            }
                            onDispose {
                                job.cancel()
                            }
                        }
                    }
                }
            }
            CashierViewModel.STATE_LOAD_FAIL -> {
                OutlinedButton(
                    onClick = {
                        val offers = viewModel.cashierGears.value?.tabs?.firstOrNull()?.groups?.mapNotNull {
                            it.offers
                        }?.flatten()
                        val productId = viewModel.cashierOrderState.value?.first ?: return@OutlinedButton
                        val offer = offers?.firstOrNull { offer->
                            offer.product?.productId == productId
                        } ?: return@OutlinedButton
                        viewModel.fetchCashierOrder(offer.id, offer.product!!)
                    }
                ) {
                    Text(text = "下单失败", modifier = Modifier.wrapContentSize())
                }
            }
        }
    }

    @OptIn(ExperimentalCoilApi::class, ExperimentalMaterialApi::class)
    @Composable
    fun CashierGearsItemView(offer: CashierOffer, viewModel: CashierViewModel) {
        val orderState = viewModel.cashierOrderState.collectAsState()
        val isSelected = when(orderState.value?.first) {
            offer.product?.productId -> true
            else -> false
        }
        Card(
            modifier = Modifier.wrapContentSize(),
            border = if (isSelected) BorderStroke(1.dp, Color.Green) else BorderStroke(1.dp, Color.Gray),
            elevation = 4.dp,
            onClick = {
                viewModel.fetchCashierOrder(offer.id, offer.product!!, amount = 1)
            }
        ) {
            val type = when (offer.type) {
                0 -> "单品"
                1 -> "套餐"
                else -> ""
            }
            val category = when (offer.categoryTag) {
                "hhlz" -> "豪华绿钻"
                "cjhy" -> "超级会员"
                else -> offer.categoryTag ?: ""
            }

            val periodUnit = when (offer.product?.periodUnit) {
                1 -> "天"
                2 -> "周"
                3 -> "月"
                4 -> "季"
                5 -> "半年"
                6 -> "年"
                else -> {}
            }
            val period = offer.product?.let { "${it.periodNumber}${periodUnit}卡" }
            val payType = when (offer.product?.payType) {
                1 -> "自动续费"
                2 -> "常规包月"
                else -> ""
            }

            val price = offer.product?.let { "￥${it.price / 100f}" } ?: ""
            val marketPrice = offer.product?.let { "￥${it.marketPrice / 100f}" } ?: ""

            Column(Modifier.wrapContentSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "$category $type ${offer.id}")
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = price, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = marketPrice, fontSize = 8.sp, fontWeight = FontWeight.Normal, fontStyle = FontStyle.Italic)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = offer.pack?.name ?: "", )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "$payType $period".trim())
                val url = offer.pack?.image ?: ""
                if (url.isNotEmpty()) {
                    Image(painter = rememberImagePainter(offer.pack?.image ?: ""), contentDescription = null, modifier = Modifier.size(60.dp))
                }
            }
        }
    }
}

class CashierViewModel: ViewModel() {
    companion object {
        const val STATE_IDLE = 0
        const val STATE_LOADING = 1
        const val STATE_LOAD_FAIL = 2
        const val STATE_LOAD_SUCCESS = 3
    }

    private  val _cashierState = MutableStateFlow(STATE_IDLE)
    val cashierState = _cashierState.asStateFlow()

    private val _msg: MutableState<String?> = mutableStateOf(null)
    val msg: State<String?> = _msg

    private val _cashierGears: MutableStateFlow<CashierGears?> = MutableStateFlow(null)
    val cashierGears: StateFlow<CashierGears?> = _cashierGears.asStateFlow()

    private val _cashierCreateOrderState = MutableStateFlow(STATE_IDLE)
    val cashierCreateOrderState = _cashierCreateOrderState.asStateFlow()

    private val _cashierOrderState: MutableStateFlow<Pair<String, CashierOrderInfo?>?> = MutableStateFlow(null)
    val cashierOrderState: StateFlow<Pair<String, CashierOrderInfo?>?> = _cashierOrderState.asStateFlow()

    private val _cashierOrderPayState = MutableStateFlow(-1)
    val cashierOrderPayState = _cashierOrderPayState.asStateFlow()

    fun fetchCashierGears() {
        _cashierState.update { STATE_LOADING }
        _cashierCreateOrderState.update { STATE_IDLE }
        _msg.value = null
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchCashierGears { resp->
                if (resp.isSuccess()) {
                    _cashierState.update { STATE_LOAD_SUCCESS }
                    _cashierGears.value = resp.data
                } else {
                    _cashierState.update { STATE_LOAD_FAIL }
                    _msg.value = "获取收银台失败(${resp.subRet}) ${resp.errorMsg}"
                }
            }
        }
    }

    fun fetchCashierOrder(offerId: String, product: CashierOfferProduct, amount: Int = 1) {
        _cashierCreateOrderState.update { STATE_LOADING }
        _msg.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val item = CashierCreateOrderItem(
                productId = product.productId,
                skuId = product.skuId,
                quantity = amount,
            )
            val params = CashierCreateOrderParams(
                cOrder = "123456",
                items = listOf(item),
                offerId = offerId,
                totalAmount = product.price * amount,
            )
            OpenApiSDK.getOpenApi().fetchCashierPayUrl(params) { resp->
                if (resp.isSuccess() && resp.data != null) {
                    _cashierCreateOrderState.update { STATE_LOAD_SUCCESS }
                    _cashierOrderState.update { product.productId to resp.data!! }
                } else {
                    _cashierCreateOrderState.update { STATE_LOAD_FAIL }
                    _msg.value = "获取订单失败(${resp.subRet}) ${resp.errorMsg}"
                    _cashierOrderState.update { product.productId to null }
                }
            }
        }
    }

    fun pollCashierOrderStatus(orderId: String) {
        _msg.value = null
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().pollCashierOrderStatus(orderId) { resp->
                if (resp.isSuccess()) {
                    _cashierOrderPayState.update { resp.data!! }
                } else {
                    _cashierOrderPayState.update { -1 }
                    _msg.value = "轮询订单失败(${resp.subRet}) ${resp.errorMsg}"
                }
            }
        }
    }

}