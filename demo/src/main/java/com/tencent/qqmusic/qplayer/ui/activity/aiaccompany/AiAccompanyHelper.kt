package com.tencent.qqmusic.qplayer.ui.activity.aiaccompany

import android.widget.Toast
import com.tencent.qqmusic.innovation.common.util.UtilContext
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.ai.AIListenError
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.openapisdk.model.aiaccompany.AIAccompanyRole
import com.tencent.qqmusic.openapisdk.model.aiaccompany.AIVolumeData
import com.tencent.qqmusic.openapisdk.model.aiaccompany.VoicePrompts
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AiAccompanyHelper {

    const val TYPE_PAUSE = 1
    const val TYPE_RESUME = 2

    private const val KEY_PAUSE = "pause"
    private const val KEY_RESUME = "resume"
    private const val KEY_HI_FIRST_TIME = "greeting"
    private const val KEY_HI_NOT_FIRST_TIME = "secondGreeting"

    var selectedRole: AIAccompanyRole? = null
    var curScene: Map<String,String>? = null

    var isListenTogetherOpen = false

    private var transitionIntro: Pair<List<SongInfo>,VoicePrompts?>? = null

    fun handleSongChangeAndPlayVoice(song: SongInfo?) {
        song ?: return
        if (isListenTogetherOpen && song.extraInfo?.introduceVoiceUrl?.isNotEmpty() == true) {
            play(song.extraInfo?.toVoicePrompts())
        }
    }

    fun handlePlayActionAndPlayVoice(actionType: Int) {
        if (!isListenTogetherOpen) {
            return
        }
        selectedRole?.voicePrompts?.let {
            when(actionType) {
                TYPE_PAUSE -> {
                    play(it[KEY_PAUSE]?.randomOrNull())
                }
                TYPE_RESUME -> {
                    play(it[KEY_RESUME]?.randomOrNull())
                }
            }
        }
    }

    // 处理转场语音播放。
    fun handleSongChangeAndPlayTransitionIntro(song: SongInfo?){
        song ?: return
        if(isListenTogetherOpen){
            GlobalScope.launch {
                transitionIntro?.let {
                    if(song in it.first){
                        play(it.second)
                        it.second?.voiceDuration?.let { durationSec -> delay((1+durationSec)*1000)  }
                        transitionIntro = null
                    }
                }
            }
        }
    }

    // 拉取推荐歌曲
    fun fetchRec2AppendSongList(){
        if (isListenTogetherOpen){
            OpenApiSDK.getAIListenTogetherApi().currentAIAccompanyRole()?.let { role ->
                val ret = OpenApiSDK.getAIListenTogetherApi().getAiRecommendSongs(role.roleId, curScene, true) { resp ->
                    if (resp.isSuccess()) {
                        resp.data?.let {
                            it.songList?.let { songList ->
                                OpenApiSDK.getPlayerApi().appendSongToPlaylist(songList)
                                it.transitionIntro?.let { voicePrompts ->
                                    transitionIntro = Pair(songList,voicePrompts)
                                }
                            }
                        }
                        Toast.makeText(UtilContext.getApp(), "拉歌成功，已添加到播放列表", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(UtilContext.getApp(), "拉歌失败，错误信息：${resp.ret}-${resp.errorMsg}", Toast.LENGTH_SHORT).show()
                    }
                }
                if (ret != AIListenError.SUCCESS) {
                    Toast.makeText(UtilContext.getApp(), "拉歌失败，错误信息：${ret.msg}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun play(voicePrompts: VoicePrompts?) {
        voicePrompts ?: return
        OpenApiSDK.getAIListenTogetherApi().playVoice(voicePrompts, AIVolumeData())
    }

    fun onRoleSelected(role: AIAccompanyRole) {
        val greeting = if (role.isFirstUse) {
            role.voicePrompts?.get(KEY_HI_FIRST_TIME)?.randomOrNull()
        } else {
            role.voicePrompts?.get(KEY_HI_NOT_FIRST_TIME)?.randomOrNull()
        }
        play(greeting)
    }
}