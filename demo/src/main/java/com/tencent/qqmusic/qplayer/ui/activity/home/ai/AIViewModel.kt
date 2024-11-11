package com.tencent.qqmusic.qplayer.ui.activity.home.ai

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tencent.qqmusic.ai.entity.AICoverCreatePayType
import com.tencent.qqmusic.ai.entity.AICoverDataInfo
import com.tencent.qqmusic.ai.entity.AICoverSongCreateType
import com.tencent.qqmusic.ai.entity.AICoverVoucherListResponse
import com.tencent.qqmusic.ai.entity.AITagData
import com.tencent.qqmusic.ai.entity.AITryLinkResponse
import com.tencent.qqmusic.ai.entity.AIWorkLinkResponse
import com.tencent.qqmusic.ai.entity.AccInfo
import com.tencent.qqmusic.ai.entity.CollectInfo
import com.tencent.qqmusic.ai.entity.HotCreateWorkInfo
import com.tencent.qqmusic.ai.entity.LikeInfo
import com.tencent.qqmusic.ai.entity.SongStyle
import com.tencent.qqmusic.ai.entity.TimbreRecordData
import com.tencent.qqmusic.ai.entity.VocalItem
import com.tencent.qqmusic.ai.entity.VoucherGetStatus
import com.tencent.qqmusic.ai.function.base.AICoverSongOperaType
import com.tencent.qqmusic.ai.function.base.IAIFunction
import com.tencent.qqmusic.ai.function.base.IAudioRecord
import com.tencent.qqmusic.innovation.common.util.ToastUtils
import com.tencent.qqmusic.openapisdk.business_common.utils.Utils
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.openapi.OpenApiCallback
import com.tencent.qqmusic.openapisdk.core.openapi.OpenApiResponse
import com.tencent.qqmusic.qplayer.App
import com.tencent.qqmusic.qplayer.core.voiceplay.AICoverLinkPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class AIViewModel : ViewModel() {
    private val TAG = "AIViewModel"

    val fileDir = App.context.cacheDir.absolutePath + "/ai"

    val timbreList = mutableStateOf(mutableListOf<VocalItem>())
    var songStyleList: List<SongStyle> by mutableStateOf(emptyList())
    var hotAiCreateSongList: List<HotCreateWorkInfo> by mutableStateOf(emptyList())
    val aiFunction = OpenApiSDK.getAIFunctionApi(IAIFunction::class.java)
    val createTimbreSongList = mutableStateListOf<AccInfo?>()
    val aiCoverTagListData = mutableStateListOf<AITagData>()
    val aiCoverSongList = mutableStateListOf<AICoverDataInfo>()
    val aiSearchCoverSongList = mutableStateListOf<AICoverDataInfo>()
    val aiPersonalCoverSongList = mutableStateListOf<AICoverDataInfo>()
    val passBackIndex = mutableMapOf<String,String?>()

    val aiPersonalCoverSongDataList = mutableStateListOf<AICoverDataInfo>()

    val payStatues = mutableStateOf<String?>("")

    val createUgcID = mutableStateListOf<String?>(null)

    val vocher = mutableStateOf<AICoverVoucherListResponse?>(null)


    var payUrl = mutableStateOf<String?>("")

    private var aiCoverLinkPlayer: AICoverLinkPlayer? = null

    fun refreshTimbreList() {
        aiFunction?.getTimbreList("0", 50) { re ->
            timbreList.value = (re.vocalist ?: emptyList()).toMutableList()
        }
    }

    fun getSongStyleList() {
        if (Utils.isFastDoubleClick(TAG + "getSongStyleList", 500)) {
            return
        }
        aiFunction?.getSongStyleList() {
            songStyleList = it.data?.styleList ?: mutableListOf()
        }
    }

    fun getHotAICreateSongs() {
        aiFunction?.getHotAICreateSongList("", 20) {
            hotAiCreateSongList = it.data?.workList ?: mutableListOf()
        }
    }


    fun getAICoverTagList() {
        aiFunction?.getAICoverHotSongTag { resp ->
            aiCoverTagListData.clear()
            aiCoverTagListData.addAll(resp.tabList)
        }
    }


    fun getAICoverSongByTag(tagId: Int, startIndex: Int) {
        aiFunction?.getAICoverSongByTag(tagId, 10, startIndex.toString()) {
            if (startIndex==0){
                aiCoverSongList.clear()
            }
            aiCoverSongList.addAll(it.aiWorkSongInfo)
            if (it.hasMore == true && !it.nextPassBack.isNullOrEmpty()){
                passBackIndex["getAICoverSongByTag"] = it.nextPassBack
            }else{
                passBackIndex.remove("getAICoverSongByTag")
            }
        }
    }


    fun getTimbreSongList() {
        aiFunction?.getGenerateTimbreData() {
            createTimbreSongList.clear()
            createTimbreSongList.addAll(it.aiWorkSongInfo.mapNotNull { it.accInfo })
        }
    }


    fun getSearchResultByWord(keyWord: String, startIndex: Int) {
        aiFunction?.getSearchSongList(keyWord, 20, startIndex) {
            if(startIndex==0){
                aiSearchCoverSongList.clear()
            }
            aiSearchCoverSongList.addAll(it.aiWorkSongInfo)
            if (it.aiWorkSongInfo.isEmpty()){
                passBackIndex["getSearchSongList"] = "-1"
            }
        }
    }


    fun getPersonalCreateData(songMid: String, startIndex: String) {
        aiFunction?.getAICoverPersonalCreateData(songMid, 10, startIndex) {
            if(startIndex.isEmpty()){
                aiPersonalCoverSongList.clear()
            }
            aiPersonalCoverSongList.addAll(it.aiWorkSongInfo)
            if (it.hasMore == true && !it.nextPassBack.isNullOrEmpty()){
                passBackIndex["getAICoverPersonalCreateData"] = it.nextPassBack
            }else{
                passBackIndex.remove("getAICoverPersonalCreateData")
            }
        }
    }


    fun operaWorkCollect(songMid: String?, ugcId: String?, operaType: Boolean) {
        aiFunction?.collectAiCoverSong(songMid, ugcId, operaType) {
            if (it.isSuccess()) {
                if (ugcId != null) {
                    val index = aiPersonalCoverSongList.indexOfFirst { it.ugcId == ugcId }
                    if (index >= 0) {
                        val newData = aiPersonalCoverSongList[index].copy(
                            collectInfo = CollectInfo(if (operaType) 1 else 0)
                        )
                        aiPersonalCoverSongList[index] = newData
                    }

                    val indexPersonal = aiPersonalCoverSongDataList.indexOfFirst { it.ugcId == ugcId }
                    if (indexPersonal >= 0) {

                        if (operaType.not()) {
                            aiPersonalCoverSongDataList.removeAt(indexPersonal)
                        }
                    }

                    val iSearch = aiSearchCoverSongList.indexOfFirst { it.ugcId == ugcId }
                    if (iSearch >= 0) {
                        val newData = aiSearchCoverSongList[iSearch].copy(
                            collectInfo = CollectInfo(if (operaType) 1 else 0)
                        )
                        aiSearchCoverSongList[iSearch] = newData
                    }
                }

                if (songMid != null) {
                    val index = aiCoverSongList.indexOfFirst { it.accInfo?.songMid == songMid }
                    if (index >= 0) {
                        val newData = aiCoverSongList[index].copy(
                            collectInfo = CollectInfo(if (operaType) 1 else 0)
                        )
                        aiCoverSongList[index] = newData
                    }

                    val indexPersonal = aiPersonalCoverSongDataList.indexOfFirst { it.accInfo?.songMid == songMid }
                    if (indexPersonal >= 0) {
                        if (operaType.not()) {
                            aiPersonalCoverSongDataList.removeAt(indexPersonal)
                        }
                    }
                    val iSearch = aiSearchCoverSongList.indexOfFirst { it.accInfo?.songMid == songMid }
                    if (iSearch >= 0) {
                        val newData = aiSearchCoverSongList[iSearch].copy(
                            collectInfo = CollectInfo(if (operaType) 1 else 0)
                        )
                        aiSearchCoverSongList[iSearch] = newData
                    }
                }
            }
        }
    }

    fun likeAiCoverSong(ugcId: String?, operaType: Boolean) {
        aiFunction?.likeAiCoverSong(ugcId, operaType) { resp ->
            if (resp.isSuccess()) {
                if (ugcId != null) {
                    val index = aiPersonalCoverSongList.indexOfFirst { it.ugcId == ugcId }
                    val data = aiPersonalCoverSongList[index]
                    val newData = data.copy(
                        likeInfo = LikeInfo(
                            resp.likeCont,
                            if (operaType) 1 else 0
                        ),
                    )
                    aiPersonalCoverSongList[index] = newData
                }
            }
        }
    }

    fun getRecord(fileName: String, fileDir: String): IAudioRecord? {
        return aiFunction?.generateAudioRecorder(fileName, fileDir)
    }


    fun generateTimbre(fileName: String, fileDir: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = "$fileDir/$fileName"
            if (File(fileDir).exists().not()) {
                Log.d(TAG, "generateTimbre: fileDir not exist")
            }
            aiFunction?.uploadPersonalRecordFile(dir) { fileId ->
                Log.d(TAG, "generateTimbre: ${fileId.fileId}")
                aiFunction.generatePersonalTimbre(listOf(TimbreRecordData(fileId = fileId.fileId))) { _ ->

                }
            }
        }
    }

    fun queryPersonalTimbreGenerateStatus(call: (Int?) -> Unit) {
        aiFunction?.queryTimbreGenerateStatus { resp ->
            call(resp.taskStatus)
        }
    }

    fun fetchAiCoverSongPersonalList(operaType: AICoverSongOperaType, startIndex: String="") {
        aiFunction?.fetchPersonalData(operaType, startIndex, 5) { resp ->
            if(startIndex.isEmpty()){
                aiPersonalCoverSongDataList.clear()
            }
            aiPersonalCoverSongDataList.addAll(resp.aiWorkSongInfo)
            if (resp.hasMore == true && !resp.nextPassBack.isNullOrEmpty()){
                passBackIndex["fetchPersonalData"] = resp.nextPassBack
            }else{
                passBackIndex.remove("fetchPersonalData")
            }
        }
    }


    fun getTryLink(songMid: String, type: AICoverSongCreateType, callback: (AITryLinkResponse) -> Unit) {
        aiFunction?.getTryLink(songMid, type) { resp ->
            callback(resp)
        }
    }


    fun playLink(url: String, songMid: String?, ugcId: String?, eventListener: AICoverLinkPlayer.PlayEventListener? = null) {
        if (Utils.isFastDoubleClick(TAG + "playLink", 500L)) {
            return
        }
        aiCoverLinkPlayer?.stop()
        aiCoverLinkPlayer = aiFunction?.getCoverSongPlayer(url, songMid, ugcId)
        aiCoverLinkPlayer?.play(eventListener)
    }

    fun pause() {
        aiCoverLinkPlayer?.pause()
    }

    fun resume() {
        aiCoverLinkPlayer?.resume()
    }

    fun stopPlayCoverLink() {
        aiCoverLinkPlayer?.stop()
    }


    fun getWorkLink(songMid: String?, ugcId: String?, call: (AIWorkLinkResponse?) -> Unit) {
        val songMidL = if (ugcId.isNullOrEmpty()) songMid else null
        aiFunction?.getWorkPlayLink(songMidL, ugcId) {
            call(it)
        }
    }


    fun fetchAICoverWorkResp(
        songMid: String,
        makeType: AICoverSongCreateType,
        userAdapterTone: Int = 0,
        payType: AICoverCreatePayType,
        couponId: String? = null,
        call: (String?, String?) -> Unit
    ) {
        aiFunction?.fetchAICoverWorkResp(songMid, makeType, userAdapterTone, payType, couponId) { resp ->
            if (resp.isSuccess()) {
                payUrl.value = resp.payUrl
                call(resp.orderId, resp.taskId)
            } else {
                ToastUtils.showShort(resp.errorMsg)
            }
        }
    }


    fun fetchAiCoverBuyStatue(orderID: String?) {
        if (orderID.isNullOrEmpty()) {
            return
        }
        aiFunction?.fetchAiCoverPayStatus(orderID) { resp ->
            payStatues.value = resp.stateMsg
        }
    }

    fun getVoucherInfo(orderID: VoucherGetStatus?) {
        if (orderID == null) {
            return
        }
        aiFunction?.getVoucherList(orderID, "", 100) { resp ->
            vocher.value = resp
        }
    }


    fun deleteAIComposeTask(scene: String, taskIdList: List<String>, callback: OpenApiCallback<OpenApiResponse<Unit>>) {
        aiFunction?.deleteAIComposeTask(scene, taskIdList, callback)
    }


    fun getCoverTaskStatus(id: String, type: AICoverSongCreateType, callback: (Int?) -> Unit) {
        aiFunction?.queryAICoverSongTaskInfo(listOf(id)) { resp ->
            resp.data?.forEach {
                if (it.key == id) {
                    when (type) {
                        AICoverSongCreateType.SEG -> {
                            callback(it.value.segMakeStatus)
                        }

                        AICoverSongCreateType.FULL_SONG -> {
                            callback(it.value.fullMakeStatus)
                        }

                        AICoverSongCreateType.PRO -> {
                            callback(it.value.proMakeStatus)
                        }

                        AICoverSongCreateType.PERFECT -> {
                            callback(it.value.prefectMakeStatus)
                        }
                    }
                }
            }

        }
    }
}