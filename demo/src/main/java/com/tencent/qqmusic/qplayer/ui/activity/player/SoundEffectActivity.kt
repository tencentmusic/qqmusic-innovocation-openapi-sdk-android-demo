package com.tencent.qqmusic.qplayer.ui.activity.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.paging.*
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.openapisdk.model.SoundEffectItem
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.main.TopBar
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Created by tannyli on 2022/11/24.
 * Copyright (c) 2022 TME. All rights reserved.
 */
class SoundEffectActivity : ComponentActivity() {

    private val vm: SoundEffectViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SoundEffectScreen(flow = vm.pagingEffectList())
        }
    }

    @Composable
    fun SoundEffectScreen(flow: Flow<PagingData<SoundEffectItem>>) {
        Scaffold(topBar = { TopBar("音效设置") }) {
            val effects = flow.collectAsLazyPagingItems()
            val curId = remember { mutableStateOf(vm.currentEffect()?.sdkId ?: -1) }
            LazyColumn(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)) {
                this.items(effects) { effect ->
                    effect ?: return@items
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable {
                            if (effect.sdkId == curId.value) {
                                OpenApiSDK
                                    .getPlayerApi()
                                    .setSoundEffectType(null)
                                UiUtils.showToast("取消音效->${effect.name}")
                                curId.value = -1
                                return@clickable
                            }
                            val ret = OpenApiSDK
                                .getPlayerApi()
                                .setSoundEffectType(effect)
                            if (ret == 0) {
                                UiUtils.showToast("设置音效->${effect.name}")
                                curId.value = effect.sdkId
                            } else if (ret == PlayDefine.PlayError.PLAY_ERR_UNSUPPORT) {
                                UiUtils.showToast("为保证您的听歌体验，杜比或臻品类歌曲播放中不建议叠加其他音效等效果")
                            } else if (ret == PlayDefine.PlayError.PLAY_ERR_NEED_VIP){
                                UiUtils.showToast("需要vip")
                            } else if (ret == PlayDefine.PlayError.PLAY_ERR_NEED_SUPER_VIP) {
                                UiUtils.showToast("需要超级会员")
                            } else {
                                UiUtils.showToast("ret:$ret")
                            }
                        }) {
                        Row(modifier = Modifier.weight(1f)) {
                            Text(text = effect.name)
                            if (effect.vipFlag == 1) {
                                Text(
                                    text = " 超级会员",
                                    modifier = Modifier.padding(5.dp, 0.dp),
                                    color = Color.Green,
                                )
                            }
                        }
                        if (curId.value == effect.sdkId) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_cheked),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(20.dp)
                            )
                        }

                    }
                }
            }
        }
    }

    class SoundEffectViewModel: ViewModel() {
        fun pagingEffectList() =  Pager(PagingConfig(50)) {
            SoundEffectPagingSource()
        }.flow

        fun currentEffect() = OpenApiSDK.getPlayerApi().getCurSoundEffect()
    }

    class SoundEffectPagingSource: PagingSource<Int, SoundEffectItem>() {
        override fun getRefreshKey(state: PagingState<Int, SoundEffectItem>): Int? {
            return state.anchorPosition
        }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SoundEffectItem> {
            return try {
                withContext(Dispatchers.IO) {
                    val dataList = OpenApiSDK.getOpenApi().blockingGet<List<SoundEffectItem>> {
                        this.fetchSoundEffectConfig(it)
                    }.data ?: emptyList()
                    val prevKey = null
                    val nextKey = null
                    LoadResult.Page<Int, SoundEffectItem>(
                        data = dataList,
                        prevKey = prevKey,
                        nextKey = nextKey
                    )
                }
            } catch (e: Exception) {
                return LoadResult.Error(e)
            }
        }

    }

}