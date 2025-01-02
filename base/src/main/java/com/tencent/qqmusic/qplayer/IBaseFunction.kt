package com.tencent.qqmusic.qplayer

import android.app.Activity

interface IBaseFunction {
    fun getAccount(): Account
    fun getAppCheckMode(): Boolean
    fun setAppCheckMode(strick: Boolean)
    fun getWxAPPID(): String
    fun getMatchID(): String
    fun getQQAPPID(): String
    fun gotoDebugActivity(activity: Activity)
    fun initDebug()
}