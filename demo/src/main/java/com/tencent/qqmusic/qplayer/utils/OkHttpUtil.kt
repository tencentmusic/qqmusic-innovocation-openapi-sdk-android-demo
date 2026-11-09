package com.tencent.qqmusic.qplayer.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

val okHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
}

suspend fun Request.sendRequest() =
    okHttpClient.newCall(this).execute()