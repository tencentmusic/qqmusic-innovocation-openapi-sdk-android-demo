package com.tencent.qqmusic.qplayer.ui.activity.login

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.*
import android.widget.Toast
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.qplayer.R

// 
// Created by clydeazhang on 2022/1/4 11:30 上午.
// Copyright (c) 2022 Tencent. All rights reserved.
// 
class WebViewActivity : Activity() {
    companion object {
        private const val TAG = "@@@WebViewActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)
        val web = findViewById<WebView>(R.id.web)
        web.settings.apply {
            this.javaScriptEnabled = true
        }
        web.webChromeClient = object : WebChromeClient() {

        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                Log.d(TAG, "shouldInterceptRequest, url=${request?.url?.toString()}")
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                Log.d(TAG, "shouldOverrideUrlLoading 1, url=$url")
                return false
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                Log.d(TAG, "shouldOverrideUrlLoading, url=${request?.url?.toString()}")
                val isQQ = request?.url?.toString()?.startsWith("wtloginmqq://") ?: false
                if (isQQ) {
                    Log.d(TAG, "jump qq")
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(request?.url?.toString() ?: "")))
                    return true
                }
                return false
            }
        }

        OpenApiSDK.getLoginApi().getQQLoginWebUrl(this, "") { url, error ->
            if (!url.isNullOrEmpty()) {
                web.loadUrl(url)
            } else {
                Toast.makeText(this, "获取url失败", Toast.LENGTH_SHORT).show()
            }
        }

    }

}