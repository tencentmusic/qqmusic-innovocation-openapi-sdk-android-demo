package com.tencent.qqmusic.qplayer.ui.activity.mv

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.tencent.qqmusic.edgemv.IEdgeMediaPlayer
import com.tencent.qqmusic.edgemv.data.MediaArea
import com.tencent.qqmusic.edgemv.data.MediaGroupRes
import com.tencent.qqmusic.edgemv.data.MediaResDetail
import com.tencent.qqmusic.edgemv.data.MediaSimpleRes
import com.tencent.qqmusic.edgemv.impl.AreaID
import com.tencent.qqmusic.edgemv.impl.CategoryType
import com.tencent.qqmusic.edgemv.impl.GetMVRecommendCmd
import com.tencent.qqmusic.edgemv.impl.SpecialArea
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.hologram.EdgeMvProvider
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PlayerViewModel : ViewModel() {
    val provider: EdgeMvProvider? = OpenApiSDK.getProviderByClass(EdgeMvProvider::class.java)
    val mPlayer: IEdgeMediaPlayer? = provider?.getMediaPlayer()
    private val _currentData = MutableStateFlow<MediaResDetail?>(null)
    val currentData: StateFlow<MediaResDetail?> = _currentData
    private val _recommendList: MutableState<List<MediaResDetail>> = mutableStateOf(emptyList())
    var recommendList: State<List<MediaResDetail>> = _recommendList

    private val _areaDetail: MutableState<List<MediaGroupRes?>> = mutableStateOf(emptyList())
    var areaDetail: State<List<MediaGroupRes?>> = _areaDetail


    private val _mvContent: MutableState<MediaArea?> = mutableStateOf(null)
    var mvContent: State<MediaArea?> = _mvContent

    fun updateMedia(mediaResDetail: MediaResDetail?) {
        AppScope.launchIO {
            _currentData.emit(mediaResDetail)
        }
    }

    fun updateMedia(id: String) {
        provider?.getMediaNetWorkImpl()?.getMediaInfo(listOf(id)) { updateMedia(it.data?.firstOrNull()) }
    }


    fun getRecommendMvList(type: GetMVRecommendCmd, areaID: AreaID, type2: CategoryType = CategoryType.ALL) {
        provider?.getMediaNetWorkImpl()?.getRecommendMVList(type, areaID, type2) {
            if (it.isSuccess()) {
                AppScope.launchIO {
                    it.data?.let {
                        val ids = it.mapNotNull { it.vid }.take(50)
                        provider.getMediaNetWorkImpl().getMediaInfo(ids) {
                            _recommendList.value = it.data ?: emptyList()
                        }
                    }
                }
            }else{
                UiUtils.showToast("接口报错：${it.errorMsg}")
            }
        }
    }


    fun getDolbyContent() {
        provider?.getMediaNetWorkImpl()?.getContentArea(SpecialArea.Dolby) {
            if (it.isSuccess()) {
                _mvContent.value = it.data
            }else{
                UiUtils.showToast("接口报错：${it.errorMsg}")
            }
        }
    }


    fun getExcellentContent() {
        provider?.getMediaNetWorkImpl()?.getContentArea(SpecialArea.Excellent) {
            if (it.isSuccess()) {
                _mvContent.value = it.data
            }else{
                UiUtils.showToast("接口报错：${it.errorMsg}")
            }
        }
    }

    fun getAreaNext(mediaGroupRes: MediaGroupRes, las: MediaSimpleRes? = null) {
        provider?.getMediaNetWorkImpl()?.getAreaDetail(SpecialArea.Dolby, mediaGroupRes, las, 4) {
            if (it.isSuccess()) {
                if (las == null) {
                    _areaDetail.value = emptyList()
                }
                val list = _areaDetail.value.toMutableList().apply { add(it.data) }
                _areaDetail.value = list
            }else{
                UiUtils.showToast("接口报错：${it.errorMsg}")
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        mPlayer?.destroy()
        _currentData.value = null
        _recommendList.value = emptyList()
        _mvContent.value = null
    }

}