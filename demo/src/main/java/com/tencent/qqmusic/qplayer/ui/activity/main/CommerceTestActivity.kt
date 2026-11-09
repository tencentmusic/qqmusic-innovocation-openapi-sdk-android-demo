package com.tencent.qqmusic.qplayer.ui.activity.main

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums
import com.tencent.qqmusic.openapisdk.core.traffic.ShowWebViewParam
import com.tencent.qqmusic.openapisdk.core.webview.WebHandle
import com.tencent.qqmusic.openapisdk.model.UserInfoNew
import com.tencent.qqmusic.openapisdk.model.quality.QualityInfoItem
import com.tencent.qqmusic.openapisdk.model.vip.DialogInfoWhenBlock
import com.tencent.qqmusic.openapisdk.model.vip.GetCheckOutInfoParam
import com.tencent.qqmusic.openapisdk.model.vip.GetShowInfoWhenBlockParam
import com.tencent.qqmusic.qplayer.ui.activity.login.WebViewActivity
import com.tencent.qqmusic.qplayer.ui.activity.mv.MvBuyQRDialog
import com.tencent.qqmusic.qplayer.utils.UiUtils

/**
 * 商业化SDK（CommerceSDK）新增接口的测试页。
 *
 * 覆盖 `CommerceSDK.get*` 封装出的公开接口：
 * - [getBlockInfoForShow] / [getCheckoutInfo]（阻断展示、收银台链接）
 * - [getUserInfoNew]（用户信息）
 * - [getQualityInfo] / [getQualityDisplayInfo]（品质展示信息）
 * - [showWebView]（Commerce 内嵌 WebView 容器）
 */
class CommerceTestActivity : ComponentActivity() {

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Scaffold(topBar = { TopBar("商业化SDK接口测试") }) {
                CommerceTestScreen()
            }
        }
    }
}

private enum class BlockCheckoutMode(val title: String) {
    BLOCK_INFO("阻断展示"),
    CHECKOUT("收银台链接")
}

/** 预设场景：一键填充 playErrCode/bizType/bizID/scene/cOrder，来源与 commercesdk demo 对齐 */
private data class ScenePreset(
    val name: String,
    val playErrCode: Int,
    val bizType: Int,
    val bizID: Long,
    val scene: String,
    val cOrder: String
)

private val SCENE_PRESETS = listOf(
    ScenePreset("绿钻默认", 200, 0, 0, "", "demoTest"),
    ScenePreset("超会默认", 208, 0, 0, "", ""),
    ScenePreset("绿钻特权", 200, 1, 97773, "vip_privilege", "demoTest"),
    ScenePreset("超会特权", 0, 1, 97773, "svip_privilege", "demoTest"),
    ScenePreset("升级超会", 0, 0, 0, "upgrade_svip", "demoTest"),
    ScenePreset("Hires专区", 0, 0, 0, "hires_zone", ""),
    ScenePreset("杜比专区", 0, 0, 0, "dolby_zone", ""),
    ScenePreset("音乐壁纸", 0, 0, 0, "music_wallpaper_svip", ""),
    ScenePreset("登录后跳转收银台(WX)", 0, 0, 0, "login_forward_wx", ""),
    ScenePreset("登录后跳转收银台(QQ)", 0, 0, 0, "login_forward_qq", ""),
    ScenePreset("登录后跳转收银台(主站扫码)", 0, 0, 0, "login_forward_music", "")
)

private data class BlockIdPreset(val name: String, val blockId: Int)

private val BLOCK_ID_PRESETS = listOf(
    BlockIdPreset("会员歌曲下载受阻", 2),
    BlockIdPreset("SQ音质受阻", 3),
    BlockIdPreset("Hires音质受阻", 4),
    BlockIdPreset("臻品2.0受阻", 5),
    BlockIdPreset("臻品全景声受阻", 6),
    BlockIdPreset("臻品母带受阻", 7),
    BlockIdPreset("杜比全景声受阻", 8),
    BlockIdPreset("黑胶受阻", 9),
    BlockIdPreset("伴唱受阻", 10),
    BlockIdPreset("疗愈受阻", 11)
)

/** 预设 unplayableCode，每个预设包含完整参数组合（来源：后台阻断规则表） */
private data class UnplayableCodePreset(
    val name: String,
    val code: Int,
    val playErrCode: Int = 0,
    val blockId: Int = 1,
    val bizType: Int = 0,
    val bizId: Long = 0,
    val access: Int = 0,
    val scene: String = "",
    val cOrder: String = "",
)

private val UNPLAYABLE_CODE_PRESETS = listOf(
    UnplayableCodePreset("可正常播放", 0),
    UnplayableCodePreset("无版权或受地区版权限制(1)", 1),
    UnplayableCodePreset("需要绿钻权益", 2, bizId = 652284775),
    UnplayableCodePreset("未购买数字专辑", 3),
    UnplayableCodePreset("无版权或受地区版权限制(4)", 4),
    UnplayableCodePreset("暂无可用音频资源", 5),
    UnplayableCodePreset("当前厂商或AppID没有播放权限", 8),
    UnplayableCodePreset("歌曲需要购买", 11),
    UnplayableCodePreset("应版权方要求不能在线播放", 12),
    UnplayableCodePreset("该歌曲无法在设备上播放", 100),
    UnplayableCodePreset("需要超会", 110, bizType = 3, bizId = 652284775),
    UnplayableCodePreset("预售数专(未预购,引导购买)", 112),
    UnplayableCodePreset("预售数专(已预购,正常播放)", 113)
)

/** 用于 [getQualityDisplayInfo] 的品质常量列表（与 QualityAlert 一致） */
private val QUALITY_IDS = listOf(
    PlayerEnums.Quality.STANDARD,
    PlayerEnums.Quality.NAC,
    PlayerEnums.Quality.HQ,
    PlayerEnums.Quality.SQ,
    PlayerEnums.Quality.SQ_SR,
    PlayerEnums.Quality.DOLBY,
    PlayerEnums.Quality.HIRES,
    PlayerEnums.Quality.EXCELLENT,
    PlayerEnums.Quality.GALAXY,
    PlayerEnums.Quality.MASTER_TAPE,
    PlayerEnums.Quality.MASTER_SR,
    PlayerEnums.Quality.DTSX
)

@Composable
private fun CommerceTestScreen() {
    val activity = LocalContext.current as Activity
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionTitle("阻断展示 & 收银台链接")
        BlockCheckoutSection(activity)

        SectionTitle("用户信息")
        UserInfoSection()

        SectionTitle("品质展示")
        QualitySection()

        SectionTitle("WebView")
        WebViewSection(activity)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.subtitle1,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp)
    )
    Divider()
}

/**
 * 阻断展示 + 收银台链接测试。两者共享同一组请求参数，仅请求接口与结果处理不同。
 */
@Composable
private fun BlockCheckoutSection(activity: Activity) {
    var mode by remember { mutableStateOf(BlockCheckoutMode.BLOCK_INFO) }

    var playErrCode by remember { mutableStateOf("200") }
    var blockID by remember { mutableStateOf("0") }
    var bizType by remember { mutableStateOf("1") }
    var bizID by remember { mutableStateOf("97773") }
    var scene by remember { mutableStateOf("svip_privilege") }
    var cOrder by remember { mutableStateOf("demoTest") }
    var urlStyle by remember { mutableStateOf("1") }
    var vipType by remember { mutableStateOf("0") }
    var unplayableCode by remember { mutableStateOf("0") }
    var access by remember { mutableStateOf("0") }
    var darkModeChecked by remember { mutableStateOf(false) }

    var blockDialog by remember { mutableStateOf<DialogInfoWhenBlock?>(null) }

    // 预设下拉状态
    var sceneMenuExpanded by remember { mutableStateOf(false) }
    var blockIdMenuExpanded by remember { mutableStateOf(false) }

    // 模式切换
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
        BlockCheckoutMode.entries.forEach { m ->
            if (m == mode) {
                Button(onClick = { mode = m }) { Text(m.title) }
            } else {
                OutlinedButton(onClick = { mode = m }) { Text(m.title) }
            }
        }
    }

    // 预设：场景 / 阻断ID
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 2.dp)) {
        OutlinedButton(onClick = { sceneMenuExpanded = true }) { Text("选择预设场景") }
        DropdownMenu(expanded = sceneMenuExpanded, onDismissRequest = { sceneMenuExpanded = false }) {
            SCENE_PRESETS.forEach { preset ->
                DropdownMenuItem(onClick = {
                    playErrCode = preset.playErrCode.toString()
                    bizType = preset.bizType.toString()
                    bizID = preset.bizID.toString()
                    scene = preset.scene
                    cOrder = preset.cOrder
                    sceneMenuExpanded = false
                }) { Text(preset.name) }
            }
        }
        OutlinedButton(onClick = { blockIdMenuExpanded = true }) { Text("选择预设阻断") }
        DropdownMenu(expanded = blockIdMenuExpanded, onDismissRequest = { blockIdMenuExpanded = false }) {
            BLOCK_ID_PRESETS.forEach { preset ->
                DropdownMenuItem(onClick = {
                    blockID = preset.blockId.toString()
                    blockIdMenuExpanded = false
                }) { Text(preset.name) }
            }
            Divider()
            UNPLAYABLE_CODE_PRESETS.forEach { preset ->
                DropdownMenuItem(onClick = {
                    unplayableCode = preset.code.toString()
                    playErrCode = preset.playErrCode.toString()
                    blockID = preset.blockId.toString()
                    bizType = preset.bizType.toString()
                    bizID = preset.bizId.toString()
                    access = preset.access.toString()
                    scene = preset.scene
                    cOrder = preset.cOrder
                    blockIdMenuExpanded = false
                }) { Text("unplayCode: ${preset.name}") }
            }
        }
    }

    // 参数输入
    LabeledInput("playErrCode", playErrCode) { playErrCode = it }
    LabeledInput("blockID", blockID) { blockID = it }
    LabeledInput("bizType", bizType) { bizType = it }
    LabeledInput("bizID", bizID) { bizID = it }
    LabeledInput("scene", scene) { scene = it }
    LabeledInput("cOrder", cOrder) { cOrder = it }
    LabeledInput("unplayableCode", unplayableCode) { unplayableCode = it }
    LabeledInput("access", access) { access = it }
    if (mode == BlockCheckoutMode.CHECKOUT) {
        LabeledInput("urlStyle", urlStyle) { urlStyle = it }
        LabeledInput("vipType", vipType) { vipType = it }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "darkMode", fontSize = 12.sp, modifier = Modifier.width(100.dp))
        androidx.compose.material.Checkbox(checked = darkModeChecked, onCheckedChange = { darkModeChecked = it })
    }

    Button(
        onClick = {
            val urlStyleV = urlStyle.toIntOrNull() ?: 1
            val vipTypeV = vipType.toIntOrNull() ?: 0
            val playErrCodeV = playErrCode.toIntOrNull() ?: 0
            val blockIDV = blockID.toIntOrNull() ?: 0
            val bizTypeV = bizType.toIntOrNull() ?: 0
            val bizIDV = bizID.toLongOrNull() ?: 0
            val unplayableCodeV = unplayableCode.toIntOrNull() ?: 0
            val accessV = access.toIntOrNull() ?: 0
            val darkModeV = if (darkModeChecked) 1 else 0

            when (mode) {
                BlockCheckoutMode.BLOCK_INFO -> {
                    OpenApiSDK.getOpenApi().getBlockInfoForShow(
                        GetShowInfoWhenBlockParam(
                            playErrCode = playErrCodeV,
                            blockID = blockIDV,
                            bizType = bizTypeV,
                            bizID = bizIDV,
                            scene = scene,
                            cOrder = cOrder,
                            darkMode = darkModeV,
                            unplayableCode = unplayableCodeV,
                            access = accessV
                        )
                    ) { resp ->
                        if (!resp.isSuccess()) {
                            UiUtils.showToast("请求失败：${resp.ret}-${resp.subRet}-${resp.errorMsg}")
                            return@getBlockInfoForShow
                        }
                        val info = resp.data
                        when (info?.showType) {
                            0 -> UiUtils.showToast(info.toast?.text ?: "不受阻（showType=0）")
                            1 -> UiUtils.showToast(info.toast?.text ?: "toast 字段未下发（showType=1）")
                            2 -> blockDialog = info.dialog
                            3 -> {
                                val url = info.webview?.url
                                if (url.isNullOrBlank()) {
                                    UiUtils.showToast("webview url 为空")
                                } else {
                                    val title = if (info.webview?.urlStyle == 2) "车机二维码" else "车机H5"
                                    MvBuyQRDialog.showQRCodeDialog(activity, url, title) {
                                        WebViewActivity.start(activity, url)
                                    }
                                }
                            }
                            else -> UiUtils.showToast("请求成功但无阻断信息 showType=${info?.showType}")
                        }
                    }
                }

                BlockCheckoutMode.CHECKOUT -> {
                    OpenApiSDK.getOpenApi().getCheckoutInfo(
                        GetCheckOutInfoParam(
                            urlStyle = urlStyleV,
                            vipType = vipTypeV,
                            playErrCode = playErrCodeV,
                            blockID = blockIDV,
                            bizType = bizTypeV,
                            bizID = bizIDV,
                            scene = scene,
                            cOrder = cOrder,
                            darkMode = darkModeV
                        )
                    ) { resp ->
                        if (!resp.isSuccess()) {
                            UiUtils.showToast("请求失败：${resp.ret}-${resp.subRet}-${resp.errorMsg}")
                            return@getCheckoutInfo
                        }
                        val url = resp.data?.payUrl
                        if (url.isNullOrBlank()) {
                            UiUtils.showToast("收银台链接为空")
                        } else {
                            val title = if (urlStyleV == 2) "车机二维码" else "车机H5"
                            MvBuyQRDialog.showQRCodeDialog(activity, url, title) {
                                WebViewActivity.start(activity, url)
                            }
                        }
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Text(if (mode == BlockCheckoutMode.BLOCK_INFO) "获取阻断展示信息" else "获取收银台链接")
    }

    // 阻断弹窗展示（showType=2）
    blockDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = { blockDialog = null },
            title = { Text(dialog.title ?: "阻断提示") },
            text = { Text(dialog.title ?: "") },
            confirmButton = {
                TextButton(onClick = { blockDialog = null }) { Text(dialog.buttonText ?: "确定") }
            }
        )
    }
}

@Composable
private fun LabeledInput(label: String, value: String, onValueChange: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(text = label, fontSize = 12.sp, modifier = Modifier.width(100.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontSize = 12.sp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun UserInfoSection() {
    var result by remember { mutableStateOf("") }
    Button(onClick = {
        result = "请求中..."
        OpenApiSDK.getOpenApi().getUserInfoNew { resp ->
            result = if (resp.isSuccess()) {
                resp.data?.let { formatUserInfo(it) } ?: "请求成功但 data 为空"
            } else {
                "请求失败：${resp.getDetailMsg()}"
            }
        }
    }) { Text("获取用户信息 getUserInfoNew") }
    if (result.isNotBlank()) {
        ResultText(result)
    }
}

@Composable
private fun QualitySection() {
    var refreshResult by remember { mutableStateOf("") }
    var cacheResult by remember { mutableStateOf("") }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {
            refreshResult = "请求中..."
            OpenApiSDK.getOpenApi().getQualityInfo { resp ->
                refreshResult = if (resp.isSuccess()) {
                    resp.data?.joinToString("\n") { item -> formatQualityItem(item) }
                        ?: "请求成功但 data 为空"
                } else {
                    "请求失败：${resp.getDetailMsg()}"
                }
            }
        }) { Text("刷新品质信息 getQualityInfo") }
        Button(onClick = {
            val map = OpenApiSDK.getOpenApi().getQualityDisplayInfo(QUALITY_IDS)
            cacheResult = if (map.isEmpty()) "(空)"
            else map.entries.joinToString("\n") { (quality, item) ->
                "quality=$quality → title=${item.title} subTitle=${item.subTitle}"
            }
        }) { Text("读取展示缓存 getQualityDisplayInfo") }
    }
    if (refreshResult.isNotBlank()) {
        ResultText(refreshResult)
    }
    if (cacheResult.isNotBlank()) {
        ResultText(cacheResult)
    }
}

@Composable
private fun WebViewSection(activity: Activity) {
    var url by remember { mutableStateOf("") }
    var webHandle by remember { mutableStateOf<WebHandle?>(null) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            placeholder = { Text("输入测试 url，默认 y.qq.com") },
            textStyle = TextStyle(fontSize = 12.sp),
            modifier = Modifier.weight(1f)
        )
        Button(onClick = {
            val target = url.ifBlank { "https://y.qq.com" }
            webHandle = OpenApiSDK.getWebViewApi().showWebView(
                activity,
                ShowWebViewParam(url = target, fullscreen = false)
            )
        }) { Text("展示") }
        Button(onClick = { webHandle?.finish() }) { Text("关闭") }
    }
    Text(
        text = "展示：通过 CommerceWebView 容器打开页面；关闭：调用 WebHandle.finish()",
        fontSize = 11.sp,
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
    )
}

@Composable
private fun ResultText(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

private fun formatUserInfo(info: UserInfoNew): String = buildString {
    appendLine("昵称: ${info.userInfo?.nickName ?: "-"}")
    appendLine("头像: ${info.userInfo?.headImageUrl ?: "-"}")
    appendLine("登录类型: ${info.userInfo?.accountLoginType}")
    val vipInfos = info.vipInfos.orEmpty()
    if (vipInfos.isEmpty()) {
        appendLine("会员: 无")
    } else {
        vipInfos.forEach { item ->
            appendLine(
                "会员[${item.vipType}] ${item.vipName ?: "-"}: " +
                    "isVip=${item.isVip} 等级=${item.vipLevel} 到期=${item.endTime} " +
                    "自动续费=${item.autoPay} 年费=${item.isYearVip}"
            )
        }
    }
    val titles = info.displayInfo?.titles.orEmpty()
    if (titles.isNotEmpty()) {
        appendLine("展示title: ${titles.joinToString(" / ")}")
    }
    val icons = info.displayInfo?.icons.orEmpty()
    if (icons.isNotEmpty()) {
        appendLine("展示icon: ${icons.joinToString(" / ")}")
    }
}

private fun formatQualityItem(item: QualityInfoItem): String =
    "id=${item.quality} name=${item.name} title=${item.title} " +
        "subTitle=${item.subTitle} size=${item.sizeDesc} vip=${item.enableVipTypes}"
