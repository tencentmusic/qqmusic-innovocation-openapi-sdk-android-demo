package com.tencent.qqmusic.qplayer.ui.activity.main

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.home.HomePage
import com.tencent.qqmusic.qplayer.ui.activity.home.HomeViewModel
import com.tencent.qqmusic.qplayer.ui.activity.home.SearchViewModel
import com.tencent.qqmusic.qplayer.ui.activity.login.MinePage
import com.tencent.qqmusic.qplayer.ui.activity.search.SearchPage

@RequiresApi(Build.VERSION_CODES.N)
@Composable
fun HomeScreen(categoryViewModel: HomeViewModel) {
    HomePage(categoryViewModel)
}

@Composable
fun RankScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.colorPrimaryDark))
            .wrapContentSize(Alignment.Center)
    ) {
        Text(
            text = "Rank View",
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
            fontSize = 25.sp
        )
    }
}

@Composable
fun RadioScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.colorPrimaryDark))
            .wrapContentSize(Alignment.Center)
    ) {
        Text(
            text = "Radio View",
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
            fontSize = 25.sp
        )
    }
}

@Composable
fun SearchScreen(searchViewModel: SearchViewModel = viewModel()) {
    SearchPage(searchViewModel)
}

@Composable
fun MineScreen(homeViewModel: HomeViewModel = viewModel()) {
    MinePage(homeViewModel)
}

