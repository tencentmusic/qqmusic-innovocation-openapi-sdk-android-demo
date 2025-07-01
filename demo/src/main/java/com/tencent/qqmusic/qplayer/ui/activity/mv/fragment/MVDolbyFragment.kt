package com.tencent.qqmusic.qplayer.ui.activity.mv.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
//import androidx.compose.foundation.lazy.GridCells
//import androidx.compose.foundation.lazy.GridItemSpan
//import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.edgemv.data.MediaGroupRes
import com.tencent.qqmusic.edgemv.data.MediaSimpleRes
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.mv.MVPlayerActivity
import com.tencent.qqmusic.qplayer.ui.activity.mv.PlayerViewModel

class MVDolbyFragment : Fragment() {

    private val mViewModelStoreOwner by lazy { activity as AppCompatActivity }


    private var composeView: ComposeView? = null
    private val playerViewModel by lazy { ViewModelProvider(mViewModelStoreOwner)[PlayerViewModel::class.java] }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        playerViewModel.getDolbyContent()
        return inflater.inflate(R.layout.fragment_compose_view, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        composeView = view.findViewById(R.id.mv_list_compose_view)
        composeView?.setContent {
            CreateView(playerViewModel)
        }
    }
}

class MVExcellentFragment : Fragment() {

    private val mViewModelStoreOwner by lazy { activity as AppCompatActivity }


    private var composeView: ComposeView? = null
    private val playerViewModel by lazy { ViewModelProvider(mViewModelStoreOwner)[PlayerViewModel::class.java] }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        playerViewModel.getExcellentContent()
        return inflater.inflate(R.layout.fragment_compose_view, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        composeView = view.findViewById(R.id.mv_list_compose_view)
        composeView?.setContent {
            CreateView(playerViewModel)
        }
    }
}

private val plachImageID: Int = R.drawable.musicopensdk_icon_light

@OptIn(ExperimentalFoundationApi::class, ExperimentalCoilApi::class)
@Preview
@Composable
private fun CreateView(playerViewModel: PlayerViewModel? = null) {
    val data = playerViewModel?.mvContent
    val activity = LocalContext.current as Activity
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = data?.value?.title ?: "", fontSize = 30.sp)
        Text(text = data?.value?.desc ?: "", fontSize = 20.sp)
        Image(
            painter = rememberImagePainter(data?.value?.cover ?: plachImageID,
                builder = {
                    crossfade(false)
                    placeholder(plachImageID)
                }),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .requiredHeightIn(min = 150.dp)
        )

        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Fixed(2),
        ) {
            data?.value?.items?.forEach { item: MediaGroupRes ->
                item(span = { GridItemSpan(2) }) {
                    Text(text = (item.title ?: "无主题")+" >" , fontSize = 30.sp, modifier = Modifier.clickable {
                        activity.startActivity(
                            Intent(activity, MVPlayerActivity::class.java).apply {
                                putExtra(MVPlayerActivity.Content_Type, MVPlayerActivity.Content_Detail)
                                putExtra(MVPlayerActivity.Content_Area, item)
                            })
                    })
                }
                val list: MutableList<MediaSimpleRes?> = item.items?.map { it.mediaItem }?.toMutableList() ?: mutableListOf()

                if (list.size % 2 != 0) {
                    list.add(MediaSimpleRes())
                }

                items(list) {
                    if (it?.vid.isNullOrEmpty().not()) {
                        DolbyItem(it)
                    } else {
                        Box(
                            modifier = Modifier
                                .width(400.dp)
                                .height(300.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun DolbyItem(mediaResDetail: MediaSimpleRes? = null) {
    val activity = LocalContext.current as? Activity
    Column {
        Box(
            modifier = Modifier
                .width(400.dp)
                .height(300.dp)
                .padding(20.dp)
                .clickable {
                    activity?.startActivity(
                        Intent(activity, MVPlayerActivity::class.java).apply {
                            putExtra(MVPlayerActivity.MV_ID, mediaResDetail?.vid)
                        })
                }

        ) {
            Column {
                ConstraintLayout {
                    val (cor, playerNum, playTime) = createRefs()
                    Image(
                        painter = rememberImagePainter(mediaResDetail?.coverImage ?: plachImageID,
                            builder = {
                                crossfade(false)
                                placeholder(plachImageID)
                            }),
                        contentDescription = null,
                        modifier = Modifier
                            .width(400.dp)
                            .wrapContentHeight()
                            .requiredHeightIn(min = 200.dp)
                            .constrainAs(cor) {
                                top.linkTo(parent.top)
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                            }
                    )

                    Text(text = formatPlayCount(mediaResDetail?.playCount), fontSize = 10.sp, modifier = Modifier.constrainAs(playerNum) {
                        start.linkTo(cor.start)
                        bottom.linkTo(cor.bottom)
                    })

                    Text(
                        text = MVPlayerFragment.convertSecondsToMinutesSeconds(mediaResDetail?.playTime ?: 0),
                        fontSize = 10.sp,
                        modifier = Modifier.constrainAs(playTime) {
                            end.linkTo(cor.end)
                            bottom.linkTo(cor.bottom)
                        })

                }
                Text(text = mediaResDetail?.name ?: "暂无标题", fontSize = 15.sp)
            }
        }
    }

}

fun formatPlayCount(playCount:Int?):String{
    if(playCount!=null){
        if (playCount>=10000){
            return "%.1f万".format(playCount/10000.0)
        }
        return playCount.toString()
    }
    return "null"
}