package com.tencent.qqmusic.qplayer.ui.activity.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.business_common.login.OpenIdInfo
import com.tencent.qqmusic.openapisdk.business_common.login.PartnerIdInfo
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.login.AuthType
import com.tencent.qqmusic.openapisdk.model.UserInfo
import com.tencent.qqmusic.openapisdk.model.VipInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MineViewModel : ViewModel() {

    private val _userInfo = MutableStateFlow<UserInfo?>(null)
    val userInfo: StateFlow<UserInfo?> = _userInfo

    private val _userVipInfo = MutableStateFlow<VipInfo?>(null)
    val userVipInfo: StateFlow<VipInfo?> = _userVipInfo

    private val _loginInfo = MutableStateFlow<OpenIdInfo?>(null)
    val loginInfo: StateFlow<OpenIdInfo?> = _loginInfo

    private val _partnerAccountInfo = MutableStateFlow<PartnerIdInfo?>(null)
    val partnerAccountInfo: StateFlow<PartnerIdInfo?> = _partnerAccountInfo

    fun updateData() {
        viewModelScope.launch {
            OpenApiSDK.getOpenApi().fetchUserInfo(callback = {
                _userInfo.value = it.data
                _loginInfo.value = Global.getLoginModuleApi().openIdInfo
                _partnerAccountInfo.value = Global.getLoginModuleApi().partnerIdInfo
            })

            OpenApiSDK.getOpenApi().fetchGreenMemberInformation {
                _userVipInfo.value = it.data
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
                else -> {
                    "未知"
                }
            }
        } ?: "未知"
    }

    fun isVip(vipInfo: VipInfo?): Boolean {
        return vipInfo?.greenVipFlag == 1   // 普通绿钻
                || vipInfo?.eightFlag == 1       // 8元付费包
                || vipInfo?.twelveFlag == 1      // 12元付费包
                || vipInfo?.yearGreenVipFlag == 1   // 年费绿钻
                || vipInfo?.superGreenVipFlag == 1  // 豪华绿钻
                || vipInfo?.hugeVipFlag == 1
    }


    fun getVipText(vipInfo: VipInfo?): String {
        return if (!isVip(vipInfo)) {
            "非vip用户"
        } else if (vipInfo?.hugeVipFlag == 1) {
            "超级会员"
        }  else if (vipInfo?.superGreenVipFlag == 1) {
            "豪华绿钻"
        } else if (vipInfo?.greenVipFlag == 1) {
            "绿钻"
        }  else {
            "信息获取不明"
        }
    }

    fun getVipTimeStartText(vipInfo: VipInfo?): String {
        return if (!isVip(vipInfo)) {
            ""
        } else if (vipInfo?.greenVipFlag == 1) {
            vipInfo.greenVipStartTime
        } else if (vipInfo?.superGreenVipFlag == 1) {
            vipInfo.superGreenVipStartTime
        } else if (vipInfo?.hugeVipFlag == 1) {
            vipInfo.hugeVipStartTime
        } else {
            "信息获取不明"
        }
    }

    fun getVipEndTimeText(vipInfo: VipInfo?): String {
        return if (!isVip(vipInfo)) {
            ""
        } else if (vipInfo?.greenVipFlag == 1) {
            vipInfo.greenVipEndTime
        } else if (vipInfo?.superGreenVipFlag == 1) {
            vipInfo.superGreenVipEndTime
        } else if (vipInfo?.hugeVipFlag == 1) {
            vipInfo.hugeVipEndTime
        } else {
            "信息获取不明"
        }
    }
}