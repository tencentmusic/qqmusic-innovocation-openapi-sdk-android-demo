package com.tencent.qqmusic.qplayer.ui.activity.mv.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.tencent.qqmusic.edgemv.impl.AreaID
import com.tencent.qqmusic.edgemv.impl.GetMVRecommendCmd
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.mv.PlayerViewModel

class MVDetailFragment(viewModelStoreOwner: ViewModelStoreOwner) : Fragment() {

    private var composeView: ComposeView? = null
    private val playerViewModel by lazy { ViewModelProvider(viewModelStoreOwner)[PlayerViewModel::class.java] }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_compose_view, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        composeView = view.findViewById(R.id.mv_list_compose_view)
        composeView?.setContent {
            playerViewModel.getRecommendMvList(GetMVRecommendCmd.LATEST, AreaID.Mainland)
            CreateView()
        }
    }

    @SuppressLint("StateFlowValueCalledInComposition")
    @Composable
    private fun CreateView() {
        val media = playerViewModel.currentData
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                horizontalArrangement = Arrangement.Center, modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Text(text = "MV VID(长按可复制):  ")
                SelectionContainer {
                    Text(text = media.value?.vid.toString())
                }
            }

            Row(
                horizontalArrangement = Arrangement.Center, modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Text(text = "上架时间:    ")
                SelectionContainer {
                    Text(text = media.value?.publicTime.toString())
                }
            }

            Row(
                horizontalArrangement = Arrangement.Center, modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Text(text = "播放量:    ")
                SelectionContainer {
                    Text(text = media.value?.playCount.toString())
                }
            }
        }
    }
}