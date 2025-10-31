package com.tencent.qqmusic.qplayer.ui.activity.person

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.business_common.event.event.LoginEvent
import com.tencent.qqmusic.openapisdk.business_common.login.OpenIdInfo
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.login.AuthType
import com.tencent.qqmusic.openapisdk.core.openapi.OpenApiResponse
import com.tencent.qqmusic.openapisdk.model.OperationsInfo
import com.tencent.qqmusic.openapisdk.model.UserInfo
import com.tencent.qqmusic.openapisdk.model.VipInfo
import com.tencent.qqmusic.openapisdk.model.vip.CheckNeedRenewalInfo
import com.tencent.qqmusic.qplayer.App
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.lang.StringBuilder

class MineViewModel : ViewModel() {
    private val TAG = "MineViewModel"
    private val _userInfo = MutableStateFlow<UserInfo?>(null)
    val userInfo: StateFlow<UserInfo?> = _userInfo

    private val _userVipInfo = MutableStateFlow<VipInfo?>(null)
    val userVipInfo: StateFlow<VipInfo?> = _userVipInfo

    private val _loginInfo = MutableStateFlow<OpenIdInfo?>(null)
    val loginInfo: StateFlow<OpenIdInfo?> = _loginInfo

    private val _partnerAccountInfo = MutableStateFlow<String?>(null)
    val partnerAccountInfo: StateFlow<String?> = _partnerAccountInfo
    private var lastUserUpdateTime = 0L

    private val _limitFree = MutableStateFlow<OpenApiResponse<OperationsInfo>?>(null)
    val limitFree: StateFlow<OpenApiResponse<OperationsInfo>?> = _limitFree

    val remindRenewalInfo = MutableStateFlow<CheckNeedRenewalInfo?>(null)

    init {
        OpenApiSDK.registerBusinessEventHandler { event ->
            when (event.code) {
                LoginEvent.MusicUserLogIn -> {
                    updateData()
                }
                LoginEvent.MusicUserLogOut -> {
                    _userInfo.value = null
                    _loginInfo.value = null
                    _userVipInfo.value = null
                    _partnerAccountInfo.value = null
                    _limitFree.value = null
                }
                LoginEvent.UserVipInfoUpdate -> {
                    updateData()
                }
            }
        }
    }

    fun updateData() {
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastUserUpdateTime < 100) {
            Log.d(TAG, "updateData too more ")
            return
        }
        lastUserUpdateTime = currentTime

        viewModelScope.launch {
            OpenApiSDK.getOpenApi().fetchUserInfo(callback = {
                _userInfo.value = it.data
                // 仅用于测试
                _loginInfo.value = Global.getLoginModuleApi().openIdInfo
                _partnerAccountInfo.value = OpenApiSDK.getPartnerApi().queryThirdPartyAccountID()
            })
            val sharedPreferences: SharedPreferences? = try {
                App.context.getSharedPreferences("OpenApiSDKEnv", Context.MODE_PRIVATE)
            } catch (e: Exception) {
                QLog.e("OtherScreen", "getSharedPreferences error e = ${e.message}")
                null
            }
            val isPartnerAccountIndependent = sharedPreferences?.getBoolean("accountModePartner", false) ?: false
            // 第三方独立登录。获取会员的接口使用独立接口
            if (isPartnerAccountIndependent) {
                OpenApiSDK.getOpenApi().fetchPartnerMemberInformation {
                    _userVipInfo.value = it.data
                }
            } else {
                OpenApiSDK.getOpenApi().fetchGreenMemberInformation {
                    _userVipInfo.value = it.data
                }
            }

            Global.getOpenApi().getOperationsInfo(callback = {
                _limitFree.value = it
            })
        }
    }

    fun refreshVipRenewalInfo() {
        viewModelScope.launch {
            OpenApiSDK.getOpenApi().getVipRenewalInfo {
                remindRenewalInfo.value = it.data
            }
        }
    }

    fun getLoginType(openIdInfo: OpenIdInfo?): String {
        return openIdInfo?.let {
            when (it.type) {
                AuthType.QQ -> "QQ"
                AuthType.QQMusic -> "QQMusic"
                AuthType.QRCode -> "扫码登录"
                AuthType.WX -> "微信"
                AuthType.PHONE -> "手机"
                AuthType.PARTNER -> "三方帐号登录"
                AuthType.OPEN_ID -> "OpenID登录"
                AuthType.MUSIC_ID -> "MusicID登录"
                AuthType.LOGIN_TOKEN -> "token登录"
                else -> {
                    "未知"
                }
            }
        } ?: "未知"
    }


    fun getVipText(vipInfo: VipInfo?): List<String> {
        val ret = mutableListOf<String>()
        val audioVip = if (vipInfo?.isVip() == false && !vipInfo.isLongAudioVip()) {
            "非vip用户"
        } else if (vipInfo?.isSuperVip() == true) {
            "超级会员"
        } else if (vipInfo?.superGreenVipFlag == 1) {
            "豪华绿钻"
        } else if (vipInfo?.greenVipFlag == 1) {
            "绿钻"
        } else if (vipInfo?.partnerVipFlag == 1) {
            "三方独立会员"
        } else {
            ""
        }
        ret.add(audioVip)
        val payVip = if (vipInfo?.twelveFlag == 1) {
            "12元付费包用户"
        } else if (vipInfo?.eightFlag == 1) {
            "8元付费包用户"
        } else {
            ""
        }
        ret.add(payVip)
        if (vipInfo?.longAudioVip == 1) {
            ret.add("听书会员用户")
        }
        if (OpenApiSDK.getLoginApi().isQQMusicLoginUserSame()) {
            ret.add("Q音手机端登录相同账号")
        }
        if (ret.isEmpty()) {
            ret.add("信息获取不明")
        }
        return ret
    }

    fun getVipTimeStartText(vipInfo: VipInfo?): String {
        return if (vipInfo?.isVip() == false && !vipInfo.isLongAudioVip()) {
            ""
        } else if (vipInfo?.greenVipFlag == 1) {
            vipInfo.greenVipStartTime
        } else if (vipInfo?.superGreenVipFlag == 1) {
            vipInfo.superGreenVipStartTime
        } else if (vipInfo?.hugeVipFlag == 1) {
            vipInfo.hugeVipStartTime
        } else if (vipInfo?.twelveFlag == 1) {
            vipInfo.twelveStartTime
        } else if (vipInfo?.eightFlag == 1) {
            vipInfo.eightStartTime
        } else if (vipInfo?.longAudioVip == 1) {
            vipInfo.longAudioVipStartTime
        } else if (vipInfo?.partnerVipFlag == 1) {
            vipInfo.partnerVipStartTime
        } else {
            "信息获取不明"
        }
    }

    fun getVipEndTimeText(vipInfo: VipInfo?): String {
        return if (vipInfo?.isVip() == false && !vipInfo.isLongAudioVip()) {
            ""
        } else if (vipInfo?.greenVipFlag == 1) {
            vipInfo.greenVipEndTime
        } else if (vipInfo?.superGreenVipFlag == 1) {
            vipInfo.superGreenVipEndTime
        } else if (vipInfo?.hugeVipFlag == 1) {
            vipInfo.hugeVipEndTime
        } else if (vipInfo?.twelveFlag == 1) {
            vipInfo.twelveEndTime
        } else if (vipInfo?.eightFlag == 1) {
            vipInfo.eightEndTime
        } else if (vipInfo?.longAudioVip == 1) {
            vipInfo.longAudioVipEndTime
        } else if (vipInfo?.partnerVipFlag == 1) {
            vipInfo.partnerVipEndTime
        } else {
            "信息获取不明"
        }
    }

    fun logout() {
        QLog.i(TAG, "logout")
        viewModelScope.launch {
            OpenApiSDK.getLoginApi().logout()
        }
    }

    fun getLimitFreeInfo(operationsInfoResp: OpenApiResponse<OperationsInfo>?): String {
        // 解析限免信息
        return operationsInfoResp?.data?.let { operationsInfo ->
            when (operationsInfo.newLoginFreeListenState) {
                0 -> "未登录"
                1 -> "未领取"
                2 -> "生效中,${convertSecondsToTimeFormat(operationsInfo.newLoginFreeListenRemainSec)}"
                3 -> "已过期"
                else -> "${operationsInfo.newLoginFreeListenState}-未知状态"
            }
        } ?: "接口异常,${operationsInfoResp?.errorMsg}"
    }

    private fun convertSecondsToTimeFormat(sec: Long): String {
        val days = sec / 86400
        val hours = (sec % 86400) / 3600
        val minutes = (sec % 3600) / 60
        val seconds = sec % 60

        val timeFormat = StringBuilder()
        if (days > 0) timeFormat.append("%d天".format(days))
        if (hours > 0) timeFormat.append("%02dh".format(hours))
        if (minutes > 0) timeFormat.append("%02dm".format(minutes))
        if (seconds > 0) timeFormat.append("%02ds".format(seconds))

        return timeFormat.toString()
    }
}