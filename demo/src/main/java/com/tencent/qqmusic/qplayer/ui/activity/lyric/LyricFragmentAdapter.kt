package com.tencent.qqmusic.qplayer.ui.activity.lyric

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.tencent.qqmusic.qplayer.R

/**
 * Created by tannyli on 2023/11/30.
 * Copyright (c) 2023 TME. All rights reserved.
 */
class LyricFragmentAdapter(activity: FragmentActivity): FragmentStateAdapter(activity) {
    override fun getItemCount(): Int {
        return 5
    }

    override fun createFragment(position: Int): Fragment {
        if (position == 1) return BaseLyricFragment(R.layout.fragment_lyric_new2)
        if (position == 2) return BaseLyricFragment(R.layout.fragment_lyric_new3)
        if (position == 3) return BaseLyricFragment(R.layout.fragment_lyric_new4)
        if (position == 4) return BaseLyricFragment(R.layout.fragment_lyric_new5)
        return BaseLyricFragment()
    }

}