package com.tencent.qqmusic.qplayer.ui.activity.mv.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
//import androidx.compose.foundation.lazy.GridCells
//import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.tencent.qqmusic.edgemv.data.MediaGroupRes
import com.tencent.qqmusic.edgemv.data.MediaSimpleRes
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.main.TopBar
import com.tencent.qqmusic.qplayer.ui.activity.mv.PlayerViewModel

class MVAreaFragment(viewModelStoreOwner: ViewModelStoreOwner, val groupRes: MediaGroupRes? = null) : Fragment() {

    var composeView: ComposeView? = null
    val playerViewModel by lazy { ViewModelProvider(viewModelStoreOwner).get(PlayerViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        playerViewModel.getAreaNext(groupRes!!, null)
        return inflater.inflate(R.layout.fragment_compose_view, container, false)

    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        composeView = view.findViewById(R.id.mv_list_compose_view)
        composeView?.setContent {
            Scaffold(topBar = { TopBar("MV")},
                modifier = Modifier.semantics{ testTagsAsResourceId=true }){
                ContentView(playerViewModel, groupRes)
            }
        }
    }


}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContentView(playerViewModel: PlayerViewModel?, mediaGroupRes: MediaGroupRes?) {
    if (mediaGroupRes == null) {
        return
    }
    val items: List<MediaSimpleRes?> = playerViewModel?.areaDetail?.value?.flatMap { it?.items?.map { it.mediaItem } ?: emptyList() } ?: emptyList()
    Column {
        Button(onClick = { playerViewModel?.getAreaNext(mediaGroupRes, items.last()) }) {
            Text(text = "新增下一页")
        }
        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Fixed(2),
        ) {
            items(items) {
                if (it?.vid.isNullOrEmpty().not()) {
                    DolbyItem(it)
                }
            }
        }
    }
}