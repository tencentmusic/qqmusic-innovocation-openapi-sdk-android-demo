package com.tencent.qqmusic.qplayer

import android.app.Activity

interface IBaseFunction {
    fun getAccount(): Account
    fun getAppCheckMode(): @AppCheckMode Int
    fun setAppCheckMode(@AppCheckMode appCheckMode: Int)
    fun setAppIdAndAppKey(appId: String, appKey: String)
    fun getWxAPPID(): String
    fun getMatchID(): String
    fun getQQAPPID(): String
    fun gotoDebugActivity(activity: Activity)
    fun initDebug(isDebug: Boolean)
    fun getChannelId(): String
}