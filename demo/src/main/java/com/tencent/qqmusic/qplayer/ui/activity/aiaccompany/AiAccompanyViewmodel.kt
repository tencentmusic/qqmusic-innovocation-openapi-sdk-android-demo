package com.tencent.qqmusic.qplayer.ui.activity.aiaccompany

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.tencent.qqmusic.innovation.common.util.ToastUtils
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.ai.AIListenError
import com.tencent.qqmusic.openapisdk.core.player.ai.OnAIListenTogetherListener
import com.tencent.qqmusic.openapisdk.model.aiaccompany.AIAccompanyRole
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.qplayer.baselib.util.QLogEx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class AiAccompanyViewModel: ViewModel() {
    companion object {
        private const val TAG = "AiAccompanyViewModel"
    }
    var curRole: AIAccompanyRole? by mutableStateOf(AiAccompanyHelper.selectedRole)
    var roleList: ArrayList<AIAccompanyRole>? by mutableStateOf(arrayListOf())
    var isListenTogetherOpen: Boolean by mutableStateOf(AiAccompanyHelper.isListenTogetherOpen)
    private var mTimingJob: Job? = null

    fun init() {
        OpenApiSDK.getAIListenTogetherApi().getAiRoleList {
            roleList = it.data?.let { it1 -> ArrayList(it1) }
        }
    }

    fun selectRole(role: AIAccompanyRole) {
        val result = OpenApiSDK.getAIListenTogetherApi().selectAIAccompanyRole(role)
        if (result == AIListenError.SUCCESS) {
            updateRole(role)
            OpenApiSDK.getAIListenTogetherApi().getAiRoleDetail(role.roleId) {
                updateRole(it.data)
                it.data?.let { role ->
                    AiAccompanyHelper.onRoleSelected(role)
                }
                if (!it.isSuccess()) {
                    init()
                }
            }
        } else {
            ToastUtils.showShort("选择角色失败，原因：${result.msg}")
        }
    }

    fun updateListenTogetherOpen(open: Boolean) {
        isListenTogetherOpen = open
        if (open) {
            startTiming()
        } else {
            cancelTiming()
        }
    }

    private fun updateRole(role: AIAccompanyRole?) {
        curRole = role
        AiAccompanyHelper.selectedRole = role
        roleList?.forEachIndexed { index, aiAccompanyRole ->
            if (role?.roleId == aiAccompanyRole?.roleId) {
                roleList?.set(index, role)
                return
            }
        }
    }

    fun updateUseTime(){
        curRole?.let { role ->
            val lastTime = role.useTimeBySecond
            OpenApiSDK.getAIListenTogetherApi().getAiRoleDetail(role.roleId) {
                if(it.isSuccess()){
                    it.data?.let { newRole ->
                        updateRole(newRole)
                        ToastUtils.showShort("一起听时长更新成功:$lastTime->${newRole.useTimeBySecond}")
                    }
                }
            }
        }
    }

    private fun startTiming() {
        cancelTiming()
        mTimingJob = flow {
            while (true) {
                delay(60.toDuration(DurationUnit.SECONDS))
                emit(System.currentTimeMillis())
            }
        }.flowOn(Dispatchers.IO).onStart {
            QLogEx.AI_LISTEN_TOGETHER.i(TAG, "startTiming onStart")
        }.onCompletion {
            QLogEx.AI_LISTEN_TOGETHER.i(TAG, "startTiming onCompletion")
        }.onEach {
            QLogEx.AI_LISTEN_TOGETHER.i(TAG, "startTiming currentTimeMillis = $it")
            OpenApiSDK.getAIListenTogetherApi().operationAiListenTogetherTime("1", 60L, object : OnAIListenTogetherListener {
                override fun onSuccess(useTimeBySec: Long) {
                    ToastUtils.showShort("上报时长成功")
                    updateUseTime()
                }

                override fun onFail(error: AIListenError) {
                    ToastUtils.showShort("上报时长失败：${error.msg}")
                }
            })
        }.launchIn(AppScope.ioScope())
    }

    private fun cancelTiming() {
        mTimingJob?.cancel()
        mTimingJob = null
        QLogEx.AI_LISTEN_TOGETHER.i(TAG, "cancelHeartBeat")
    }

    override fun onCleared() {
        super.onCleared()
        cancelTiming()
    }


}