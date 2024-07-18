package com.tencent.qqmusic.qplayer.ui.activity.mv.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.tencent.qqmusic.edgemv.impl.AreaID
import com.tencent.qqmusic.edgemv.impl.GetMVRecommendCmd
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.mv.MVResDetailPage
import com.tencent.qqmusic.qplayer.ui.activity.mv.PlayerViewModel

class MVRecommendFragment : Fragment() {

    private val mViewModelStoreOwner by lazy {  activity as AppCompatActivity }

    private var composeView: ComposeView? = null
    private val playerViewModel by lazy { ViewModelProvider(mViewModelStoreOwner)[PlayerViewModel::class.java] }

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

    @Composable
    private fun CreateView() {
        val list = playerViewModel.recommendList
        val type = remember {
            mutableStateOf(GetMVRecommendCmd.LATEST)
        }
        val areaID = remember {
            mutableStateOf(AreaID.Mainland)
        }

        val showTypeSelect = remember {
            mutableStateOf(false)
        }

        val showAreaSelect = remember {
            mutableStateOf(false)
        }

        fun update() {
            playerViewModel.getRecommendMvList(type.value, areaID.value)
        }
        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 10.dp)) {
                        Text(text = "类型")
                        Button(onClick = {
                            showTypeSelect.value = true
                        }, modifier = Modifier.padding(start = 20.dp)) {
                            Text(text = type.value.name)
                        }
                    }

                    DropdownMenu(expanded = showTypeSelect.value, onDismissRequest = { }) {
                        GetMVRecommendCmd.values().forEach {
                            DropdownMenuItem(
                                onClick = {
                                    showTypeSelect.value = showTypeSelect.value.not()
                                    type.value = it
                                    update()
                                },
                                content = {
                                    Text(text = it.name)
                                }
                            )
                        }
                    }
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 40.dp)) {
                        Text(text = "地区")
                        Button(onClick = {
                            showAreaSelect.value = true
                        }, modifier = Modifier.padding(start = 20.dp)) {
                            Text(text = areaID.value.name)
                        }
                    }

                    DropdownMenu(expanded = showAreaSelect.value, onDismissRequest = { }) {
                        AreaID.values().forEach {
                            DropdownMenuItem(
                                onClick = {
                                    showAreaSelect.value = showAreaSelect.value.not()
                                    areaID.value = it
                                    update()
                                },
                                content = {
                                    Text(text = it.name)
                                }
                            )
                        }
                    }
                }
            }
            MVResDetailPage(list = list.value)
        }
    }
}