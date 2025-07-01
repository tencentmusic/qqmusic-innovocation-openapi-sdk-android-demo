package com.tencent.qqmusic.qplayer.ui.activity.mv

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.tencent.qqmusic.edgemv.data.MediaResDetail
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.mv.fragment.MVAreaFragment
import com.tencent.qqmusic.qplayer.ui.activity.mv.fragment.MVDetailFragment
import com.tencent.qqmusic.qplayer.ui.activity.mv.fragment.MVDolbyFragment
import com.tencent.qqmusic.qplayer.ui.activity.mv.fragment.MVExcellentFragment
import com.tencent.qqmusic.qplayer.ui.activity.mv.fragment.MVPlayerFragment
import com.tencent.qqmusic.qplayer.ui.activity.mv.fragment.MVRecommendFragment


class MVPlayerActivity : AppCompatActivity() {
    companion object {
        const val MV_ID = "mvid"
        const val MV_RES = "mv_res_content"
        const val Content_Type = "content_type"
        const val Recommend = "Recommend"
        const val Dolby_Content = "Dolby_content"
        const val Content_Detail = "Content_Detail"
        const val Content_Area = "Content_Area"
        const val Content_EXCELLENT = "Content_EXCELLENT"

    }

    private var adapter: MvFragmentViewAdapter? = null

    private val playerViewModel by lazy { ViewModelProvider(this).get(PlayerViewModel::class.java) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mvplayer)
        val viewPager = findViewById<ViewPager2>(R.id.view_pager)
        adapter = MvFragmentViewAdapter(supportFragmentManager, this)
        viewPager.adapter = adapter
        handleIntent(intent)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun handleIntent(intent: Intent?) {
        intent?.let {
            val type = it.getStringExtra(Content_Type)
            val fragments = when (type) {
                Recommend -> {
                    listOf(MVRecommendFragment())
                }

                Dolby_Content -> {
                    listOf(MVDolbyFragment())
                }

                Content_EXCELLENT -> {
                    listOf(MVExcellentFragment())
                }

                Content_Detail -> {
                    listOf(MVAreaFragment(this@MVPlayerActivity, intent.getParcelableExtra(Content_Area)))
                }

                else -> {
                    val index = intent.getStringExtra(MV_ID) ?: ""
                    if (index.isNotEmpty()) {
                        playerViewModel.updateMedia(index)
                    } else {
                        val detail = intent.getParcelableExtra<MediaResDetail>(MV_RES)
                        playerViewModel.updateMedia(detail)
                    }
                    listOf(
                        MVPlayerFragment(this@MVPlayerActivity),
                        MVDetailFragment(this@MVPlayerActivity)
                    )
                }
            }
            adapter?.list = fragments
            adapter?.notifyDataSetChanged()
        }
    }


    private class MvFragmentViewAdapter(fragmentManager: FragmentManager, activity: AppCompatActivity) : FragmentStateAdapter(fragmentManager, activity.lifecycle) {

        var list: List<Fragment>? = emptyList()

        override fun getItemCount(): Int {
            return list?.size ?: 0
        }

        override fun createFragment(p0: Int): Fragment {
            return list?.get(p0)!!
        }

    }
}