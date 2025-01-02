package com.tencent.qqmusic.qplayer.ui.activity.login

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.login.AuthType
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

// 
// Created by clydeazhang on 2022/1/6 2:30 下午.
// Copyright (c) 2022 Tencent. All rights reserved.
// 
class OpiQRCodeActivity : Activity() {

    private var isFinished = false

    companion object {
        private const val TAG = "OpiQRCodeActivity"
        const val KEY_DEV_NAME = "KEY_DEV_NAME"
        private const val POLL_INTERVAL = 1000L
        private const val ERROR_EXPIRE = -10
        private const val ERROR_NO_SCAN = -11
        private const val ERROR_CANCEL = -14
    }

    private val ivQrCode by lazy {
        findViewById<ImageView>(R.id.iv_qrcode)
    }
    private val tvTips by lazy {
        findViewById<TextView>(R.id.tv_tips)
    }
    private val btnRefresh by lazy {
        findViewById<TextView>(R.id.btn_refresh)
    }

    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private val devName by lazy {
        intent.extras?.getString(KEY_DEV_NAME, "Unknown") ?: "Unknown"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_openapisdk_qrcode)

        btnRefresh.setOnClickListener {
            startGetQrCode()
        }

        startGetQrCode()
    }

    private fun startGetQrCode() {
        showTips("二维码加载中...", false)
        OpenApiSDK.getOpenApi().getLoginQrCode() {
            if (it.isSuccess()) {
                GlobalScope.launch {
                    val bitmap = UiUtils.generateQRCode(it.data!!.qrCode)
                    runOnUiThread {
                        if (bitmap != null) {
                            showQrCode(bitmap)
                            if (!isDestroyed) {
                                handler.postDelayed(Runnable {
                                    requestQrCodeAuthResult(it.data!!.authCode)
                                }, POLL_INTERVAL)
                            }
                        } else {
                            showTips("二维码生成失败", true)
                        }
                    }
                }
            } else {
                showTips(it.errorMsg ?: "二维码加载失败", true)
            }
        }
    }

    private fun requestQrCodeAuthResult(pollAuthCode: String) {
        OpenApiSDK.getOpenApi().pollQrCodeLoginResult(pollAuthCode) {
            if (isFinished) {
                return@pollQrCodeLoginResult
            }
            if (it.isSuccess()) {
                // 登录成功
                Log.i(TAG, "扫码登录成功, authCode=${it.data}")
                OpenApiSDK.getLoginApi().onGetAuthCode(AuthType.QRCode, it.data ?: "")
                finish()
            } else {
                if (it.ret == ERROR_EXPIRE) {
                    showTips("二维码已过期", true)
                } else if (it.ret == ERROR_CANCEL) {
                    // 取消以后需要刷新二维码
                    startGetQrCode()
                } else if (it.ret == ERROR_NO_SCAN) {
                    if (!isDestroyed) {
                        handler.postDelayed(Runnable {
                            requestQrCodeAuthResult(pollAuthCode)
                        }, POLL_INTERVAL)
                    }
                } else {
                    showTips("轮询结果失败", true)
                }
            }
        }
    }

    private fun showTips(tips: String, needBtn: Boolean) {
        ivQrCode.visibility = View.GONE
        tvTips.visibility = View.VISIBLE
        if (needBtn) {
            btnRefresh.visibility = View.VISIBLE
        } else {
            btnRefresh.visibility = View.GONE
        }
        tvTips.text = tips
    }

    private fun showQrCode(bitmap: Bitmap) {
        ivQrCode.visibility = View.VISIBLE
        tvTips.visibility = View.GONE
        btnRefresh.visibility = View.GONE
        ivQrCode.setImageBitmap(bitmap)
    }

    override fun finish() {
        super.finish()
        handler.removeCallbacksAndMessages(null)
        isFinished = true
    }
}