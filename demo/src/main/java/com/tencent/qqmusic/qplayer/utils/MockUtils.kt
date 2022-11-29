package com.tencent.qqmusic.qplayer.utils

import android.content.Context
import android.util.Log
import com.tencent.qqmusic.qplayer.App


/**
 * Created by tannyli on 2021/9/1.
 * Copyright (c) 2021 TME. All rights reserved.
 */
object MockUtils {

    private const val TAG = "MockUtils"

    val SongListOwn = "8146976003"
    val SongListIdOther = "7107159327"

    val songList = listOf(314818717L, 317968884L, 316868744L, 291130348L)
    val songId = 314818717L

    val radioId = 199

    val topListId = 26

    val albumId = 55085L
    val matchId = 0

    fun testFocus(context: Context) {
        AudioFocusChangeHelper(context).apply {
            requestFocus()
            audioFocusChangeListener = object : AudioFocusChangeHelper.AudioFocusChangeListener{
                override fun audioFocusLoss() {
                    Log.i(TAG, "audioFocusLoss")
                }

                override fun audioFocusLossTransient() {
                    Log.i(TAG, "audioFocusLossTransient")
                }

                override fun audioFocusLossTransientCanDuck() {
                    Log.i(TAG, "audioFocusLossTransientCanDuck")
                }

                override fun audioFocusGain(reason: Int) {
                    Log.i(TAG, "audioFocusGain reason:$reason")
                }

            }
        }
    }


}