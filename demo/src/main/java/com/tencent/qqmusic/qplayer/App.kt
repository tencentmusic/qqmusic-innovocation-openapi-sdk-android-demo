package com.tencent.qqmusic.qplayer

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.tencent.qqmusic.innovation.common.util.UtilContext
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.ISDKSpecialNeedInterface
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.qplayer.ui.activity.MainActivity
import com.tencent.qqmusic.qplayer.ui.activity.MustInitConfig
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerObserver
import com.tencent.qqmusic.qplayer.utils.AudioFocusChangeHelper
import com.tencent.qqmusic.qplayer.utils.MockUtils
import com.tencent.qqmusic.sharedfileaccessor.SPBridge
import com.tencent.qqmusicplayerprocess.service.NotificationParams
import java.lang.Exception

/**
 * Created by tannyli on 2021/8/31.
 * Copyright (c) 2021 TME. All rights reserved.
 */
class App : Application() {

    companion object {
        private const val TAG = "App"

        fun init(context: Context) {
            Log.i(TAG, "init Application")
            OpenApiSDK.init(
                context.applicationContext,
                MustInitConfig.APP_ID,
                MustInitConfig.APP_KEY
            )

            OpenApiSDK.getPlayerApi().setEnableCallStateListener(false)
            OpenApiSDK.getPlayerApi().setSDKSpecialNeedInterface(object : ISDKSpecialNeedInterface {
                override fun getNotification(playSong: SongInfo): Notification? {
                    var notification: Notification? = null
                    var builder: Notification.Builder? = null
                    when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val channelId = "10000"
                                val channelName = "新聊天消息"
                                val importance = NotificationManager.IMPORTANCE_HIGH
                                val notificationManager =
                                    context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                                val channel =
                                    NotificationChannel(channelId, channelName, importance)
                                notificationManager.createNotificationChannel(channel)
                            }

                            builder = Notification.Builder(UtilContext.getApp(), "10000")
                            builder.setContentTitle(NotificationParams.SNotificationTitle)
                                .setSmallIcon(R.drawable.icon_notification)
                                .setOngoing(false)
                                .setCategory(Notification.CATEGORY_SERVICE)
                            notification = builder.build()
                        }
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN -> {
                            builder = Notification.Builder(UtilContext.getApp())
                            builder.setContentTitle(NotificationParams.SNotificationTitle)
                            builder.setSmallIcon(R.drawable.icon_notification)
                            builder.setOngoing(false)
                            notification = builder.build()
                        }
                        else -> {
                            notification = Notification()
                        }
                    }
                    return notification
                }

                override fun needRequestFocus(): Boolean {
                    return true
                }

                override fun isAutoPlayNext(): Boolean {
                    return true
                }
            })

//            MockUtils.testFocus(context)

            //OpenApiSDK.getPlayerApi().setEnableMediaButton(false)
            OpenApiSDK.getPlayerApi().setEnableBluetoothListener(false)

            PlayerObserver.registerSongEvent()
        }
    }
}