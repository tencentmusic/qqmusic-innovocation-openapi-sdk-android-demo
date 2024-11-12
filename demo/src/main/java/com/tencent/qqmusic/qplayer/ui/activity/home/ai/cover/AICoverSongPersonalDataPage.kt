package com.tencent.qqmusic.qplayer.ui.activity.home.ai.cover

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tencent.qqmusic.ai.entity.AICoverDataInfo
import com.tencent.qqmusic.ai.function.base.AICoverSongOperaType
import com.tencent.qqmusic.qplayer.ui.activity.home.ai.AITimbreCreatePage
import com.tencent.qqmusic.qplayer.ui.activity.home.ai.AIViewModel
import com.tencent.qqmusic.qplayer.utils.UiUtils


@Composable
fun AICoverSongPersonalDataPage(backPrePage: () -> Unit) {

    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            backPrePage.invoke()
            remove()
        }
    }

    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(key1 = dispatcher) {
        dispatcher?.addCallback(callback)
        onDispose {
            callback.remove() // 移除回调
        }
    }
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { PersonalBasePage(navController = navController) }
        composable(AITimbreTAG) {
            AITimbreCreatePage {
                navController.popBackStack()
            }
        }

        composable(AIPersonalCreatePageTAG) {
            AIPersonalCreatePage {
                navController.popBackStack()
            }
        }

        composable(AIBuyTAG) {
            AiCoverBuyPage(buyAccDataInfo) {
                navController.popBackStack()
            }
        }


    }

}

@Composable
fun PersonalBasePage(aiViewModel: AIViewModel = viewModel(), navController: NavHostController) {
    val typeValue = remember {
        mutableStateOf(AICoverSongOperaType.MY_WORK)
    }
    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(modifier = Modifier.fillMaxWidth()) {
            AICoverSongOperaType.values().forEach { type ->
                when (type) {
                    AICoverSongOperaType.MY_WORK -> {
                        item {
                            Button(onClick = {
                                typeValue.value = AICoverSongOperaType.MY_WORK
                                aiViewModel.fetchAiCoverSongPersonalList(AICoverSongOperaType.MY_WORK)
                            }, modifier = Modifier.padding(10.dp)) {
                                Text(text = "我的作品")
                            }
                        }
                    }

                    AICoverSongOperaType.MY_COLLECT_WORK -> {
                        item {
                            Button(onClick = {
                                typeValue.value = AICoverSongOperaType.MY_COLLECT_WORK
                                aiViewModel.fetchAiCoverSongPersonalList(AICoverSongOperaType.MY_COLLECT_WORK)

                            }, modifier = Modifier.padding(10.dp)) {
                                Text(text = "我收藏的作品")
                            }
                        }
                    }

                    AICoverSongOperaType.MY_COLLECT_SONG -> {
                        item {
                            Button(onClick = {
                                typeValue.value = AICoverSongOperaType.MY_COLLECT_SONG
                                aiViewModel.fetchAiCoverSongPersonalList(AICoverSongOperaType.MY_COLLECT_SONG)

                            }, modifier = Modifier.padding(10.dp)) {
                                Text(text = "我收藏的歌曲")
                            }
                        }
                    }

                    AICoverSongOperaType.MY_BUY -> {
                        item {
                            Button(onClick = {
                                typeValue.value = AICoverSongOperaType.MY_BUY
                                aiViewModel.fetchAiCoverSongPersonalList(AICoverSongOperaType.MY_BUY)
                            }, modifier = Modifier.padding(10.dp)) {
                                Text(text = "我购买的作品")
                            }
                        }
                    }

                    AICoverSongOperaType.RECOMMEND_WORK -> {
                        item {
                            Button(onClick = {
                                typeValue.value = AICoverSongOperaType.RECOMMEND_WORK
                                aiViewModel.fetchAiCoverSongPersonalList(AICoverSongOperaType.RECOMMEND_WORK)
                            }, modifier = Modifier.padding(10.dp)) {
                                Text(text = "用户作品试听")
                            }
                        }
                    }
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            aiViewModel.aiPersonalCoverSongDataList.forEach {
                item {
                    val ispersonal = when (typeValue.value) {
                        AICoverSongOperaType.MY_WORK -> true
                        AICoverSongOperaType.MY_COLLECT_WORK -> true
                        AICoverSongOperaType.MY_COLLECT_SONG -> false
                        AICoverSongOperaType.MY_BUY -> false
                        AICoverSongOperaType.RECOMMEND_WORK -> true
                    }
                    Column {
                        AiSongItem(it, isPersonal = ispersonal, navController = navController)
                        Button({
                            aiViewModel.deleteAIComposeTask(
                                "3",
                                listOf(it.ugcId ?: "")
                            ) { ret ->
                                if (ret.isSuccess()) {
                                    UiUtils.showToast("删除成功")
                                    aiViewModel.fetchAiCoverSongPersonalList(typeValue.value)
                                } else {
                                    UiUtils.showToast("删除失败:${ret.errorMsg}")
                                }
                            }
                        }) {
                            Text(text = "删除")
                        }
                    }
                }
            }

            // 添加翻页item
            item {
                Box(modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp)
                    .clickable {
                        val page = aiViewModel.passBackIndex["fetchPersonalData"]?:""
                        aiViewModel.fetchAiCoverSongPersonalList(typeValue.value, page)
                    }) {
                    if(aiViewModel.passBackIndex["fetchPersonalData"].isNullOrEmpty()) {
                        Text(text = "已经是最后一页")
                    }else {
                        Text(text = "点击翻页->", fontSize = 18.sp, color = Color.Blue)
                    }
                }
            }

        }
    }
}