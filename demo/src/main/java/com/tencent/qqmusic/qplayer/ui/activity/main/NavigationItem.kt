package com.tencent.qqmusic.qplayer.ui.activity.main

import com.tencent.qqmusic.qplayer.R

sealed class NavigationItem(var route: String, var icon: Int, var title: String) {
    object Home : NavigationItem("main", R.drawable.ic_home, "首页")
    object Music : NavigationItem("rank", R.drawable.ic_music, "排行")
    object Movies : NavigationItem("radio", R.drawable.ic_movie, "电台")
    object Books : NavigationItem("search", R.drawable.ic_book, "搜索")
    object Profile : NavigationItem("mine", R.drawable.ic_profile, "我的")
    object Other : NavigationItem("other", R.drawable.ic_other, "其他")
}