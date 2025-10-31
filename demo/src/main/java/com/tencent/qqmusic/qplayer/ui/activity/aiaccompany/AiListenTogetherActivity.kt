package com.tencent.qqmusic.qplayer.ui.activity.aiaccompany

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.isDigitsOnly
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.innovation.common.util.ToastUtils
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.ai.AIListenError
import com.tencent.qqmusic.openapisdk.core.player.ai.OnAIListenTogetherListener
import com.tencent.qqmusic.openapisdk.core.player.ai.OnVoicePlayListener
import com.tencent.qqmusic.openapisdk.model.aiaccompany.AIAccompanyRole
import com.tencent.qqmusic.openapisdk.model.aiaccompany.AIVolumeData
import com.tencent.qqmusic.openapisdk.model.aiaccompany.VoicePrompts
import com.tencent.qqmusic.qplayer.baselib.util.QLogEx
import com.tencent.qqmusic.qplayer.ui.activity.main.TopBar
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerObserver
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class AiListenTogetherActivity: AppCompatActivity() {
    companion object {
        private const val TAG = "AiAccompanyActivity"
    }
    private val viewModel: AiAccompanyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.init()
        setContent {
                AiListenTogetherScreen(viewModel)
            }
    }

    @Composable
    fun AiListenTogetherScreen(viewModel:AiAccompanyViewModel) { // 假设 MyViewModel 已经定义
        Scaffold(
            topBar = {
                TopBar("AI伴听")
            }
        ) {
            Column {
                OpenAIListenTogether(viewModel)
                TestPlayVoice()
                FetchRecSong(this@AiListenTogetherActivity)
                UpdateListenTime()
                Divider(
                    Modifier
                        .height(13.dp)
                        .padding(top = 5.dp, bottom = 5.dp))
                AiAccompanyScreen(viewModel)
            }
        }
    }

    @Composable
    fun OpenAIListenTogether(viewModel: AiAccompanyViewModel) {
        val roleId = viewModel.curRole?.roleId
        Row(modifier = Modifier.padding(top = 5.dp, bottom = 5.dp, start = 5.dp)) {

            TextButton(onClick = {
                if (roleId == null) {
                    ToastUtils.showShort("请先选择角色再进入一起听!!!")
                } else {
                    OpenApiSDK.getAIListenTogetherApi().openAIListenTogether(object : OnAIListenTogetherListener {
                        override fun onSuccess(useTimeBySec: Long) {
                            ToastUtils.showShort("一起听进入成功，一起听时长：${useTimeBySec / 60} 分钟")
                            AiAccompanyHelper.isListenTogetherOpen = true
                            viewModel.updateListenTogetherOpen(true)
                        }

                        override fun onFail(error: AIListenError) {
                            ToastUtils.showShort("一起听进入失败")
                        }
                    })
                }
            }, Modifier.background(Color.Green)) {
                Text(text = "打开一起听", color = Color.White, fontSize = 18.sp)
            }

            TextButton(onClick = {
                val role = OpenApiSDK.getAIListenTogetherApi().currentAIAccompanyRole()
                role?.let {
                    OpenApiSDK.getAIListenTogetherApi().queryAiListenTogetherTime(it, object : OnAIListenTogetherListener {
                        override fun onSuccess(useTimeBySec: Long) {
                            ToastUtils.showShort("一起听关闭成功,当前角色${role.name}, 陪伴时长${useTimeBySec / 60} 分钟")
                        }

                        override fun onFail(error: AIListenError) {
                            ToastUtils.showShort("一起听关闭成功")
                        }
                    })
                } ?: run {
                    ToastUtils.showShort("一起听关闭成功")
                }
                OpenApiSDK.getAIListenTogetherApi().closeAIListenTogether()
                AiAccompanyHelper.isListenTogetherOpen = false
                AiAccompanyHelper.curScene = null
                viewModel.updateListenTogetherOpen(false)
            }, modifier = Modifier
                .padding(start = 10.dp)
                .background(Color.Red)) {
                Text(text = "关闭一起听", color = Color.White, fontSize = 18.sp)
            }
        }
        val tips = if (viewModel.isListenTogetherOpen) {
            "打开"
        } else {
            "关闭"
        }
        Text(text = "一起听状态: $tips", color = Color.Black, fontSize = 18.sp, modifier = Modifier.padding(10.dp))
    }

    @Composable
    fun TestPlayVoice() {
        Row(modifier = Modifier.padding(top = 5.dp, bottom = 5.dp, start = 5.dp)) {
            TextButton(onClick = {
                val status = OpenApiSDK.getAIListenTogetherApi().playVoice(VoicePrompts(0, "", "https://qmlisten.y.qq.com/3045/854051b880c2e0b4c7741724dfe2df77.mp3", 0L), AIVolumeData(1.0f, 0.3f), object : OnVoicePlayListener {
                    override fun onPlay() {
                        QLogEx.AI_LISTEN_TOGETHER.i(TAG, "onPlay")
                    }

                    override fun onStop() {
                        QLogEx.AI_LISTEN_TOGETHER.i(TAG, "onStop")
                    }

                    override fun onError() {
                        QLogEx.AI_LISTEN_TOGETHER.i(TAG, "onError")
                    }
                }, needFadeIn = true, needFadeOut = true)
                ToastUtils.showShort(status.msg)
            }, Modifier.background(Color.Green)) {
                Text(text = "播报语音", color = Color.White, fontSize = 15.sp)
            }

            TextButton(onClick = {
                OpenApiSDK.getAIListenTogetherApi().stopVoice()
            }, modifier = Modifier
                .padding(start = 5.dp)
                .background(Color.Red)) {
                Text(text = "停止语音", color = Color.White, fontSize = 15.sp)
            }
        }

    }

    @Composable
    fun FetchRecSong(activity: AppCompatActivity) {
        var scene by remember { mutableStateOf(TextFieldValue("")) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            TextField(
                value = scene,
                label = {
                    Text(text = "输入场景，每组用空格分隔，同组内用-分隔，如：情绪-开心 环境-小雨")
                },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                onValueChange = {
                    scene = it
                },
                modifier = Modifier
                    .wrapContentSize()
                    .wrapContentHeight()
            )
        }
        TextButton(onClick = {
            scene = TextFieldValue("")
            val categories = mapOf(
                "情绪" to listOf("欢快", "愉快", "开心", "愉悦", "高兴", "快乐", "难过", "悲伤", "孤独", "忧伤", "治愈", "思念", "想念", "励志", "鸡血", "轻松", "舒缓", "舒适"),
                "语种" to listOf("国语", "粤语", "闽南语", "日语", "韩语", "英语", "法语", "其他", "拉丁语", "纯音乐"),
                "状态" to listOf("放空", "小憩", "休息"),
                "环境" to listOf("晴天（白天）", "晴天（夜间）", "多云（白天）", "多云（夜间）", "阴", "雾霾", "小雨", "中雨", "大雨", "暴雨", "小雪", "中雪", "大雪", "暴雪"),
                "目的地" to listOf("住宅", "家", "公司", "学校", "餐饮", "景区"),
                "驾乘信息" to listOf("儿童")
            )
            // 从 Map 中随机选取 n 个元素
            val randomEntries = categories.entries
                .toList()  // 将 entries 转换为 List
                .shuffled(Random)  // 打乱顺序
                .take(Random.nextInt(1,categories.size + 1))  // 取前 n 个元素

            val newList = mutableListOf<String>()
            randomEntries.forEach { entry->
                newList.add("${entry.key}-${entry.value.shuffled().first()}")
            }
            scene = TextFieldValue(newList.joinToString(" "))

        }) {
            Text(text="随机填入场景")
        }

        val sceneMap = hashMapOf<String, String>()
        scene.text.split(" ").forEach {
            val keyValue = it.split("-")
            sceneMap[keyValue.firstOrNull() ?: ""] = keyValue.getOrNull(1) ?: ""
        }


        TextButton(onClick = {
            viewModel.curRole?.roleId?.let { roleId ->
                val ret = OpenApiSDK.getAIListenTogetherApi().getAiRecommendSongs(roleId, sceneMap, true) { resp ->
                    if (resp.isSuccess()) {
                        resp.data?.let {
                            AiAccompanyHelper.curScene=sceneMap
                            GlobalScope.launch {
                                if (it.transitionIntro!=null){
                                    AiAccompanyHelper.play(it.transitionIntro)
                                    it.transitionIntro!!.voiceDuration?.let { durationSec -> delay((1+durationSec)*1000) }
                                }else{
                                    Toast.makeText(activity, "没有串场信息", Toast.LENGTH_SHORT).show()
                                }
                                it.songList?.let { songList -> OpenApiSDK.getPlayerApi().playSongs(songList) }
                            }
                            Toast.makeText(activity, "获取成功，已添加到播放列表", Toast.LENGTH_SHORT).show()
                        }

                    } else {
                        Toast.makeText(activity, "获取失败，错误信息：${resp.ret}-${resp.errorMsg}", Toast.LENGTH_SHORT).show()
                    }
                }
                if (ret != AIListenError.SUCCESS) {
                    Toast.makeText(activity, "获取失败，错误信息：${ret.msg}", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(activity, "请先选择角色", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text(text = "拉取推荐歌曲并播放")
        }
    }

    @Composable
    fun UpdateListenTime(){
        var updateTime by remember { mutableStateOf(TextFieldValue("")) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            TextField(
                value = updateTime,
                label = {
                    Text(text = "输入一起听总时长,单位秒")
                },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                onValueChange = {
                    if(it.text.isDigitsOnly()){
                        updateTime=it
                    }
                },
                modifier = Modifier
                    .wrapContentSize()
                    .wrapContentHeight()
            )
            Button(onClick = {
                updateTime.text.toLongOrNull()?.let {time ->
                    OpenApiSDK.getAIListenTogetherApi().operationAiListenTogetherTime(
                        "3",time,object : OnAIListenTogetherListener{
                            override fun onSuccess(useTimeBySec: Long) {
                                viewModel.updateUseTime()
                            }

                            override fun onFail(error: AIListenError) {
                                ToastUtils.showShort("上报时长失败：${error.code},${error.msg}")
                            }

                        })
                }

            }) {
                Text(text = "点击上报")
            }
        }
    }

    @Composable
    fun AiAccompanyScreen(viewModel: AiAccompanyViewModel) {
        val roleList = viewModel.roleList
        if (roleList.isNullOrEmpty()) {
            Text(text = "暂无AI角色")
        } else {
            LazyColumn(
                modifier = Modifier.padding(5.dp),
                contentPadding = PaddingValues(5.dp)
            ) {
                item {
                    Text(text = "角色列表")
                }

                items(roleList.size) { index ->
                    val role = roleList[index]
                    RoleItem(role, role.roleId == viewModel.curRole?.roleId)
                }
            }
        }
    }

    @Composable
    fun RoleItem(role: AIAccompanyRole, isUsing: Boolean) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly, // 平分横向空间
            verticalAlignment = Alignment.CenterVertically
        ) {
            image(url = role.headIcon ?: "")
            image(url = role.staticPic ?: "")
            image(url = role.dynamicPic ?: "")
        }
        Column {
            Text(text = role.name ?: "")

            Text(text = "已陪伴时长：${PlayerObserver.convertTime(role.useTimeBySecond)}")

            role.musicPreferences?.joinToString(" ")?.let {
                Text(text = "音乐偏好：$it")
            }
            role.personality?.personTags?.joinToString(" ")?.let {
                Text(text = "个人标签：$it")
            }

            role.personality?.musicTags?.joinToString(" ")?.let {
                Text(text = "音乐标签：$it")
            }

            Text(text = role.personality?.mbti ?: "")

            role.personality?.introduction?.let {
                Text(text = "介绍：$it")
            }

            role.personality?.openingRemark?.let {
                Text(text = "开场白：${it.rawText}")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly, // 平分横向空间
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    AiAccompanyHelper.play(role.personality?.openingRemark)
                }) {
                    Text(text = "播放口白")
                }
                TextButton(onClick = {
                    viewModel.selectRole(role)
                }, enabled = !isUsing) {
                    val text = if (isUsing) {
                        "已选择"
                    } else {
                        "选择角色"
                    }
                    Text(text)
                }
            }
        }
    }

    @Composable
    private fun image(url: String) {
        Image(painter = rememberImagePainter(url), "",
            modifier = Modifier
                .width(100.dp)
                .height(100.dp))
    }

    @Preview(showBackground = true)
    @Composable
    fun PreviewAiListenTogetherScreen() {
        val fakeViewModel = AiAccompanyViewModel()
        AiListenTogetherScreen(fakeViewModel)
    }
}