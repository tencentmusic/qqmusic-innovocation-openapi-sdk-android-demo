package com.tencent.qqmusic.qplayer.ui.activity.home.ai

import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.ai.entity.EmTimbreModStatus
import com.tencent.qqmusic.ai.entity.PersonalTimbreItem
import com.tencent.qqmusic.ai.entity.TimbreModType

private const val TAG = "AIPersonalTimbrePage"

@Composable
fun AIPersonalTimbrePage(
    aiViewModel: AIViewModel = viewModel(),
    backPrePage: () -> Unit
) {
    val timbreList = aiViewModel.timbrePersonalList.value
    Log.d(TAG, "(${aiViewModel.hashCode()})AIPersonalTimbrePage: timbreList = $timbreList")
    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Log.d(TAG, "handleOnBackPressed: ")
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        if (timbreList.isEmpty()) {
            // 空列表状态
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "暂无个人音色",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        } else {
            // 音色列表
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(timbreList) { timbre ->
                    PersonalTimbreItemCard(
                        timbre = timbre,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable(onClick = {
                                aiViewModel.selectTimbre(timbre)
                            }),
                        editClick = { name, type ->
                            // 修改音色类型和名称的操作
                            aiViewModel.editTimbre(timbre.timbreID, name, type)
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun testPersonalTimbrePage() {
    PersonalTimbreItemCard(
        timbre = PersonalTimbreItem(
            0,
            "测试音色",
            "",
            true,
            "",
            EmTimbreModStatus.emTimbreModSucc.value,
            0,
            true,
            false,
            false,
            0,
            0,
            0
        ),
        modifier = Modifier
    ) { _, _ -> Unit }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun PersonalTimbreItemCard(
    timbre: PersonalTimbreItem,
    modifier: Modifier = Modifier,
    editClick: (String, Int) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    // 编辑对话框
    if (showEditDialog) {
        PersonalTimbreEditDialog(timbre.name, timbre.type, editClick) {
            showEditDialog = false
        }
    }

    Card(
        modifier = modifier,
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 封面图片
            Box {
                Image(
                    painter = rememberImagePainter(data = timbre.pic),
                    contentDescription = "音色封面",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                // 当前使用标识
                if (timbre.isSelect) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.Blue)
                    ) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.star_on),
                            contentDescription = "当前使用",
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.Center),
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 音色信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 音色名称
                Text(
                    text = timbre.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 模型状态
                Text(
                    text = getTimbreStatusText(timbre.status) + " " + getTimbreTypeText(timbre.type),
                    fontSize = 14.sp,
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Blue)
                    .clickable(onClick = {
                        showEditDialog = true
                        // 这里可以设置初始的音色类型值
                    })
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_edit),
                    contentDescription = "编辑",
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.Center),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun PersonalTimbreEditDialog(
    editedName: String,
    editedType: Int,
    editClick: (String, Int) -> Unit,
    dismiss: () -> Unit
) {
    var editedName1 by remember { mutableStateOf(editedName) }
    var editedType1 by remember { mutableStateOf(editedType) }
    var expanded by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = { dismiss.invoke() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(16.dp),
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "编辑音色信息",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 音色名称编辑
                Text(
                    text = "音色名称",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                BasicTextField(
                    value = editedName1,
                    onValueChange = { editedName1 = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.LightGray.copy(alpha = 0.2f))
                        .padding(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 音色类型编辑 - 改为下拉选择框
                Text(
                    text = "音色类型",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize(Alignment.TopStart)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.LightGray.copy(alpha = 0.2f))
                            .clickable { expanded = true }
                            .padding(8.dp)
                    ) {
                        Text(
                            text = getTimbreTypeText(editedType1),
                            textAlign = TextAlign.Start
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(onClick = {
                            editedType1 = TimbreModType.EmTimbreModTypeMale.value
                            expanded = false
                        }) {
                            Text("男")
                        }
                        DropdownMenuItem(onClick = {
                            editedType1 = TimbreModType.EmTimbreModTypeFemale.value
                            expanded = false
                        }) {
                            Text("女")
                        }
                        DropdownMenuItem(onClick = {
                            editedType1 = TimbreModType.EmTimbreModTypeChildren.value
                            expanded = false
                        }) {
                            Text("儿童")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Gray)
                            .clickable { dismiss.invoke() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "取消",
                            color = Color.White
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Blue)
                            .clickable {
                                editClick(editedName1, editedType1)
                                dismiss.invoke()
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "保存",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun getTimbreStatusText(status: Int): String {
    return when (status) {
        EmTimbreModStatus.emTimbreModRunning.value -> "生成中..."
        EmTimbreModStatus.emTimbreModSucc.value -> "模型就绪"
        EmTimbreModStatus.emTimbreModFail.value -> "生成失败"
        EmTimbreModStatus.emTimbreModWaiting.value -> "等待中"
        EmTimbreModStatus.emTimbreModDel.value -> "已删除"
        else -> "未生成模型"
    }
}

// 添加音色类型文本转换函数
@Composable
fun getTimbreTypeText(type: Int): String {
    return when (type) {
        TimbreModType.EmTimbreModTypeMale.value -> "男"
        TimbreModType.EmTimbreModTypeFemale.value -> "女"
        TimbreModType.EmTimbreModTypeChildren.value -> "儿童"
        else -> "未知类型"
    }
}