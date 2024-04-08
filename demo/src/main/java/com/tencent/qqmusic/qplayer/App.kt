package com.tencent.qqmusic.qplayer

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.StrictMode
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.tencent.qqmusic.innovation.common.util.DeviceUtils
import com.tencent.qqmusic.innovation.common.util.ProcessUtils
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.business_common.event.event.LogEvent
import com.tencent.qqmusic.openapisdk.core.InitConfig
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.network.NetworkTimeoutConfig
import com.tencent.qqmusic.qplayer.baselib.util.GsonHelper
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.qplayer.ui.activity.MustInitConfig
import com.tencent.qqmusic.qplayer.utils.PrivacyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * Created by tannyli on 2021/8/31.
 * Copyright (c) 2021 TME. All rights reserved.
 */
class App : Application() {


    override fun onCreate() {
        super.onCreate()
        if (!ProcessUtils.isMainProcess()) {
//            Debug.waitForDebugger()
        }
        if (MustInitConfig.openStrictMode()) {
            openStrictMode()
        }

        if (PrivacyManager.isGrant()) {
            init(this)
        } else {
            PrivacyManager.delayPrivacyEnd {
                init(this)
            }
        }
    }


    private fun openStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
    }

    companion object {
        private const val TAG = "App"

        fun init(context: Context) {
            Log.i(TAG, "init Application")


            OpenApiSDK.registerBusinessEventHandler {
                when (it.code) {
                    LogEvent.LogFileCanNotWrite -> {
                        GlobalScope.launch(Dispatchers.Main){
                            delay(2000)
                            Toast.makeText(context, "日志路径不可读写，使用默认路径", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            Global.isDebug = true
            val sharedPreferences: SharedPreferences? = try {
                context.getSharedPreferences("OpenApiSDKEnv", Context.MODE_PRIVATE)
            } catch (e: Exception) {
                QLog.e("OtherScreen", "getSharedPreferences error e = ${e.message}")
                null
            }
            val isUseForegroundService = sharedPreferences?.getBoolean("isUseForegroundService", true) ?: true
            val logFileDir = sharedPreferences?.getString("logFileDir", "")
            val savedTimeoutConfig = sharedPreferences?.getString("NetworkTimeoutConfig", "")
            val timeoutConfig = GsonHelper.safeFromJson(savedTimeoutConfig, NetworkTimeoutConfig::class.java) ?: NetworkTimeoutConfig.DEFAULT()
            val initConfig = InitConfig(
                context.applicationContext,
                MustInitConfig.APP_ID,
                MustInitConfig.APP_KEY,
                DeviceUtils.getAndroidID(),

            ).apply {
                this.isUseForegroundService = isUseForegroundService
                this.crashConfig = InitConfig.CrashConfig(enableNativeCrashReport = true, enableAnrReport = true)
                this.deviceConfigInfo.apply {
                    hardwareInfo = ""
                }
                this.logFileDir = logFileDir
                this.networkTimeoutConfig = timeoutConfig
            }
            val start = System.currentTimeMillis()
            OpenApiSDK.init(initConfig)
            GlobalScope.launch(Dispatchers.Default) {
                try {
                    val enableLog = sharedPreferences?.getBoolean("enableLog", true) ?: true
                    OpenApiSDK.getLogApi().setLogEnable(enableLog)
                } catch (ignore: Exception) {
                }
            }
            Log.i(TAG, "init cost:${System.currentTimeMillis() - start}")
        }
    }
}