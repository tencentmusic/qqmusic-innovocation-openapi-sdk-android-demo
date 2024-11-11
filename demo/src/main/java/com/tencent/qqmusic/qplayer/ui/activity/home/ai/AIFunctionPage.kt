package com.tencent.qqmusic.qplayer.ui.activity.home.ai

import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.ai.entity.UserInfo
import com.tencent.qqmusic.ai.function.base.IAIFunction
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.qplayer.ui.activity.home.ai.cover.AICoverSongPage
import com.tencent.qqmusic.qplayer.ui.activity.home.ai.cover.AICoverSongPersonalDataPage
import com.tencent.qqmusic.qplayer.ui.activity.home.areaIndex
import com.tencent.qqmusic.qplayer.utils.UiUtils


val functionIndex = mutableStateOf(-1)
private val functionTypes = mutableListOf(
    "AI歌曲专区",
    "灵感做歌",
    "帮你唱",
    "作曲",
    "AI个人资产",
    "AI帮唱资产"
)
private const val TAG = "AIFunctionPage"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AIFunctionPage() {

    remember {
        functionIndex.value = -1
    }

    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Log.d(TAG, "handleOnBackPressed: ")
            areaIndex.value = -1
            remove()
        }
    }
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(key1 = dispatcher) {
        onDispose {
            areaIndex.value = -1
            callback.remove() // 移除回调
        }
    }

    var userInfo by remember { mutableStateOf<UserInfo?>(null) }

    when (functionIndex.value) {
        0 -> {
            AIFolder {
                backAIFunctionHome()
            }
        }

        1 -> {
            AIComposingSongPage {
                backAIFunctionHome()
            }
        }

        2 -> {
            AICoverSongPage()
        }


        3 -> {
            AIComposePage {
                backAIFunctionHome()
            }
        }

        4 -> {
            AISongRecordPage {
                backAIFunctionHome()
            }
        }

        5 ->{
            AICoverSongPersonalDataPage{
                backAIFunctionHome()
            }
        }



        else -> {
            Column {
                Row (verticalAlignment = Alignment.CenterVertically){
                    Chip("获取用户信息") {
                        OpenApiSDK.getAIFunctionApi(IAIFunction::class.java)?.fetchUserInfo {
                            if (it.isSuccess() && it.data != null) {
                                userInfo = it.data!!.userInfo
                            } else {
                                UiUtils.showToast("获取用户信息失败:${it.errorMsg}")
                            }
                        }
                    }
                    userInfo?.let {
                        Image(
                            painter = rememberImagePainter(it.userIcon),
                            contentDescription = null,
                            modifier = Modifier
                                .size(50.dp)
                                .padding(10.dp)
                        )
                        Text(text = "用户名:${it.nickName},ID:${it.userId},手机号:${it.phoneNumber}")
                    }
                }
                LazyVerticalGrid(
                    modifier = Modifier.fillMaxSize(),
                    cells = GridCells.Fixed(2),
                ) {
                    items(functionTypes) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier
                            .height(100.dp)
                            .clickable {
                                functionTypes
                                    .indexOf(it)
                                    .let { index ->
                                        if (functionIndex.value >= 4 || OpenApiSDK
                                                .getLoginApi()
                                                .hasLogin()
                                        ) {
                                            functionIndex.value = index
                                        } else {
                                            UiUtils.showToast("请先登录")
                                        }
                                    }
                                dispatcher?.addCallback(callback)
                            }) {
                            Text(text = it)
                        }
                    }
                }
            }
        }
    }
}

private fun backAIFunctionHome() {
    functionIndex.value = -1
}

