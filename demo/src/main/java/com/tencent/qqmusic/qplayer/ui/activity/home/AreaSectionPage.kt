package com.tencent.qqmusic.qplayer.ui.activity.main

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import com.tencent.qqmusic.qplayer.ui.activity.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "AreaSectionPage"

@Composable
fun AreaSectionPage(homeViewModel: HomeViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        areaSectionTabs(homeViewModel)
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun areaSectionTabs(homeViewModel: HomeViewModel){
    val pages = mutableListOf(
        "Hires专区",
        "Dolby专区"
    )
    val pagerState = rememberPagerState()
    val composableScope = rememberCoroutineScope()

    ScrollableTabRow(
        selectedTabIndex = pagerState.currentPage,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
            )
        }
    ) {
        pages.forEachIndexed { index, title ->
            Tab(
                text = { Text(text = title) },
                selected = pagerState.currentPage == index,
                onClick = {
                    composableScope.launch(Dispatchers.Main) {
                        pagerState.scrollToPage(index)
                    }
                },
                selectedContentColor = Color.White,
                unselectedContentColor = Color.Gray
            )
        }
    }
    HorizontalPager(
        count = pages.size,
        state = pagerState
    ) {
        val index = pagerState.currentPage
        Log.i(TAG, "AreaSectionPage: current index $index")
        when (index) {
            0 -> {
                HiresSectionPage(homeViewModel)
            }
            1 -> {
                DolbySectionPage(homeViewModel)
            }
        }
    }
}

