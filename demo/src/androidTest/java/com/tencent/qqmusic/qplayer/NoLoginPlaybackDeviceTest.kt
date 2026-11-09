package com.tencent.qqmusic.qplayer

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.IMediaEventListener
import com.tencent.qqmusic.openapisdk.model.SongInfo
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.lang.reflect.Proxy
import java.security.MessageDigest
import java.util.Calendar

/** 在已初始化、已有后台歌单的 Demo 真机运行；仅清理本需求的提示次数。 */
@RunWith(AndroidJUnit4::class)
class NoLoginPlaybackDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val managerClass = Class.forName("com.tencent.qqmusic.qplayer.core.voiceplay.NoLoginPlayVoiceManager")
    private val manager = managerClass.getField("INSTANCE").get(null)
    private val player get() = OpenApiSDK.getPlayerApi()
    @Volatile private var startSequence = 0
    @Volatile private var startedSongId: Long? = null
    private fun field(name: String) = managerClass.getDeclaredField(name).apply { isAccessible = true }
    private fun read(name: String): Any? = field(name).get(manager)
    private fun write(name: String, value: Any?) = field(name).set(manager, value)
    private fun note(message: String) {
        android.util.Log.i("NoLoginDeviceTest", message)
        instrumentation.sendStatus(0, Bundle().apply { putString("stream", "$message\n") })
    }
    private fun waitFor(label: String, timeout: Long = 20000, condition: () -> Boolean) {
        val until = SystemClock.elapsedRealtime() + timeout
        while (SystemClock.elapsedRealtime() < until) {
            if (condition()) return
            SystemClock.sleep(50)
        }
        error("timeout $label: song=${player.getCurrentSongInfo()?.songId}, state=${player.getPlayState()}, count=${read("playedCount")}, songs=${read("playedNoLoginSongCount")}")
    }
    private fun main(block: () -> Unit) = instrumentation.runOnMainSync(block)
    private fun stopVoice() = main { managerClass.getMethod("stopVoice", Int::class.javaPrimitiveType).invoke(manager, 3) }
    private fun reset() {
        stopVoice()
        synchronized(read("lock")!!) {
            val mmkvClass = Class.forName("com.tencent.qqmusiccommon.SimpleMMKV")
            val mmkv = mmkvClass.getMethod("getCommonMMKV").invoke(mmkvClass.getField("INSTANCE").get(null))
            mmkv.javaClass.getMethod("putInt", String::class.java, Int::class.javaPrimitiveType)
                .invoke(mmkv, "no_login_voice_play_count", 0)
            write("playedCount", 0)
            write("lastVoiceSongCount", null)
            write("playedNoLoginSongCount", 0)
            write("lastSavedDay", Calendar.getInstance().let { it.get(Calendar.YEAR) * 1000 + it.get(Calendar.DAY_OF_YEAR) })
        }
        note("RESET daily voice count=0, session songs=0")
    }
    private fun start(pos: Int) {
        val before = read("playedNoLoginSongCount") as Int
        val beforeEvent = startSequence
        val id = player.getPlayList()[pos].songId
        note("REQUEST pos=$pos id=$id begin=${player.getPlayList()[pos].tryBegin} end=${player.getPlayList()[pos].tryEnd}")
        main { assertEquals(0, player.playPos(pos)) }
        waitFor("start $id") { startSequence > beforeEvent && startedSongId == id && player.getCurrentSongInfo()?.songId == id && player.getPlayState() == 4 && (read("playedNoLoginSongCount") as Int) > before }
        note("START id=$id vip=${player.getCurrentSongInfo()?.isVip()} tryOnly=${player.getCurrentSongInfo()?.canOnlyPlayTry()} time=${player.getCurrentPlayTime()} songs=${read("playedNoLoginSongCount")}")
    }
    private fun finishAndAdvance(): Long? {
        val song = player.getCurrentSongInfo()!!
        val beforeEvent = startSequence
        val end = song.tryEnd ?: error("missing tryEnd")
        main { note("SEEK end=$end result=${player.seek(end - 1200)}") }
        waitFor("finish and next", 15000) { startSequence > beforeEvent && startedSongId != song.songId && player.getCurrentSongInfo()?.songId != song.songId && player.getPlayState() == 4 }
        note("ADVANCE ${song.songId} -> ${player.getCurrentSongInfo()?.songId}, voiceCount=${read("playedCount")}")
        return player.getCurrentSongInfo()?.songId
    }
    private fun readyVoice(expectedVip: Boolean? = null): Any {
        waitFor("voice onPlay", 9000) { (read("playedCount") as Int) > 0 && read("voicePlayer") != null }
        note("VOICE onPlay count=${read("playedCount")}, lastSong=${read("lastVoiceSongCount")}, musicState=${player.getPlayState()}")
        val voice = read("voicePlayer")!!
        if (expectedVip != null) {
            val common = commonVoice(voice)!!
            val core = common.javaClass.getDeclaredField("mAudioPlayer").apply { isAccessible = true }.get(common)
            val tracer = core.javaClass.getDeclaredField("mDataSource").apply { isAccessible = true }.get(core)
            val dataSourceType = Class.forName("com.tencent.qqmusic.mediaplayer.upstream.IDataSource")
            val fileSource = tracer.javaClass.declaredFields.first { dataSourceType.isAssignableFrom(it.type) }.apply { isAccessible = true }.get(tracer)
            val path = fileSource.javaClass.getMethod("getFilePath").invoke(fileSource) as String
            val key = (read("readyFiles") as Map<*, *>).entries.firstOrNull { (it.value as File).absolutePath == path }?.key as? String
            assertTrue("voice file must match vip=$expectedVip list", resourceKeys(expectedVip).contains(key))
            note("VOICE_SOURCE vip=$expectedVip file=${File(path).name}")
        }
        return voice
    }
    private fun commonVoice(voice: Any): Any? = voice.javaClass.getDeclaredField("mCommonPlayer").apply { isAccessible = true }.get(voice)
    private fun sourcePlayer(): Any {
        val listClass = Class.forName("com.tencent.qqmusicsdk.player.playlist.PlayListManager")
        val list = listClass.getMethod("getInstance").invoke(null)
        val audio = listClass.getMethod("getAudioPlayerManager").invoke(list)
        return audio.javaClass.getMethod("getPlayer").invoke(audio)!!
    }
    private fun policy(): Any {
        val configClass = Class.forName("com.tencent.qqmusic.openapisdk.business_common.config.CommonConfigManager")
        val config = configClass.getDeclaredField("commonConfig").apply { isAccessible = true }.get(null)!!
        return config.javaClass.getMethod("getNoLoginVoicePolicy").invoke(config)!!
    }
    private fun resourceKeys(vip: Boolean): Set<String> {
        val config = policy()
        val items = config.javaClass.getMethod(if (vip) "getVipBlockInfo" else "getNotVipBlockInfo").invoke(config) as List<*>
        return items.map { item ->
            val url = item!!.javaClass.getMethod("getVoiceUrl").invoke(item)
            val md5 = item.javaClass.getMethod("getFileMd5").invoke(item) ?: ""
            "$url|$md5"
        }.toSet()
    }

    @Test fun verifyEventListenerRegistration() {
        waitFor("manager init", 30000) { read("initialized") == true }
        main { player.stop() }
        waitFor("stop playback") { player.getPlayState() != 4 }
        SystemClock.sleep(650)
        reset()
        val configClass = Class.forName("com.tencent.qqmusic.openapisdk.business_common.config.CommonConfigManager")
        val config = configClass.getDeclaredField("commonConfig").apply { isAccessible = true }.get(null)!!
        val policyType = Class.forName("com.tencent.qqmusic.openapisdk.business_common.model.NoLoginVoicePolicy")
        val original = config.javaClass.getMethod("getNoLoginVoicePolicy").invoke(config)
        val setter = config.javaClass.getMethod("setNoLoginVoicePolicy", policyType)
        val constructor = policyType.declaredConstructors.first { it.parameterTypes.size == 5 }
        val item = Class.forName("com.tencent.qqmusic.openapisdk.business_common.model.TryBlockVoiceInfo").getConstructor().newInstance()
        val refresh = managerClass.getDeclaredMethod("refreshResources").apply { isAccessible = true }
        val helperType = Class.forName("com.tencent.qqmusic.qplayer.core.player.MusicPlayerHelper")
        val helper = helperType.getMethod("getInstance").invoke(null)
        val global = Class.forName("com.tencent.qqmusic.openapisdk.business_common.event.GlobalEventHandler")
        val loginListener = read("loginListener")
        fun registrationCounts(): Pair<Int, Int> {
            val music = helperType.getDeclaredField("mMusicEventHandleInterface").apply { isAccessible = true }.get(helper) as Collection<*>
            val business = global.getDeclaredField("callbackList").apply { isAccessible = true }.get(null) as Collection<*>
            return music.count { it === manager } to business.count { it === loginListener }
        }
        val playEvent = Class.forName("com.tencent.qqmusic.openapisdk.core.player.PlayEvent")
        val startEvent = playEvent.getField("EVENT_PLAY_START").getInt(null)
        val stateEvent = playEvent.getField("EVENT_PLAY_STATE_CHANGE").getInt(null)
        val callback = managerClass.getMethod("updateMusicPlayEvent", Int::class.javaPrimitiveType, Any::class.java)
        val names = listOf("generation", "playVoiceJob", "playedForCurrentSong", "playedNoLoginSongCount", "voicePlayer")
        val disabled = listOf(null,
            constructor.newInstance(3, 3, 3, null, null),
            constructor.newInstance(3, 3, 3, emptyList<Any>(), emptyList<Any>()),
            constructor.newInstance(3, 3, 3, listOf(item), emptyList<Any>()),
            constructor.newInstance(3, 3, 3, emptyList<Any>(), listOf(item)))
        val enabled = constructor.newInstance(3, 3, 3, listOf(item), listOf(item))
        try {
            disabled.forEachIndexed { index, value ->
                setter.invoke(config, value)
                refresh.invoke(manager)
                assertEquals(false, read("eventListening"))
                assertEquals(0 to 0, registrationCounts())
                val before = names.associateWith(::read)
                main {
                    callback.invoke(manager, startEvent, null)
                    for (state in 0..9) callback.invoke(manager, stateEvent, state)
                }
                assertEquals(before, names.associateWith(::read))
                note("PASS disabled case=$index: music/login registrations=0/0, all playback events skipped")
            }
            repeat(3) {
                setter.invoke(config, enabled)
                refresh.invoke(manager)
                assertEquals(true, read("eventListening"))
                assertEquals(1 to 1, registrationCounts())
            }
            note("PASS both lists configured: music/login registrations=1/1 after repeated refreshes")
            setter.invoke(config, disabled[3])
            refresh.invoke(manager)
            assertEquals(false, read("eventListening"))
            assertEquals(0 to 0, registrationCounts())
            assertNull(read("playVoiceJob"))
            assertNull(read("voicePlayer"))
            note("PASS config disabled: both listeners removed and hint state cleared")
            setter.invoke(config, enabled)
            refresh.invoke(manager)
            assertEquals(1 to 1, registrationCounts())
            note("PASS config re-enabled: both listeners registered once")
        } finally {
            setter.invoke(config, original)
            refresh.invoke(manager)
            reset()
        }
    }

    @Test fun verifyRequirementOnDevice() {
        val context = instrumentation.targetContext
        main { context.startActivity(Intent().setClassName(context.packageName, "com.tencent.qqmusic.qplayer.ui.activity.main.DemoActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        waitFor("manager init", 30000) { read("initialized") == true && player.getPlayList().isNotEmpty() }
        // Wait for the startup network config replacement before taking/restoring its snapshot.
        SystemClock.sleep(3500)
        waitFor("configured nonblank assets", 30000) {
            val keys = (resourceKeys(false) + resourceKeys(true)).filter { it.substringBeforeLast('|').isNotBlank() }
            keys.isNotEmpty() && (read("readyFiles") as Map<*, *>).keys.containsAll(keys)
        }
        val listener = object : IMediaEventListener {
            override fun onEvent(event: String, arg: Bundle) {
                if (event == "API_EVENT_PLAY_START") {
                    startedSongId = player.getCurrentSongInfo()?.songId
                    startSequence++
                }
                if (event.contains("ERROR") || event.contains("STATE") || event.contains("PLAY_START"))
                    note("EVENT $event $arg song=${player.getCurrentSongInfo()?.songId}")
            }
        }
        player.registerEventListener(listener)
        val originalMode = player.getPlayMode()
        val originalSong = player.getCurrentSongInfo()?.songId
        val originalPosition = player.getCurrentPlayTime().toInt()
        val wasPlaying = player.getPlayState() == 4
        val failures = mutableListOf<String>()
        val configClass = Class.forName("com.tencent.qqmusic.openapisdk.business_common.config.CommonConfigManager")
        val commonConfig = configClass.getDeclaredField("commonConfig").apply { isAccessible = true }.get(null)!!
        val policyType = Class.forName("com.tencent.qqmusic.openapisdk.business_common.model.NoLoginVoicePolicy")
        val policySetter = commonConfig.javaClass.getMethod("setNoLoginVoicePolicy", policyType)
        val originalPolicy = policy()
        var injectedAssets = false
        var restoreControlledAssets: (() -> Unit)? = null
        val songs = player.getPlayList()
        note("DEVICE playlist=${songs.size}, originalMode=$originalMode, initialized=${read("initialized")}, dailyBefore=${read("playedCount")}")
        reset()
        if (InstrumentationRegistry.getArguments().getString("resetOnly") == "true") { player.unregisterEventListener(listener); return }
        val preferred = listOf(200883727L, 246189563L, 450646L, 253043865L, 215483310L)
        val normal = songs.indices.filter { songs[it].isNoLoginSong() && !songs[it].isVip() && !songs[it].canOnlyPlayTry() }
            .sortedBy { preferred.indexOf(songs[it].songId).let { index -> if (index < 0) 10000 else index } }
        val trial = songs.indices.filter { songs[it].isNoLoginSong() && songs[it].canOnlyPlayTry() }
        note("CANDIDATES fullNonVip=${normal.size}, flaggedTryOnly=${trial.size}")
        fun scenario(name: String, block: () -> Unit) {
            if (InstrumentationRegistry.getArguments().getString("scenarios") == "assets" &&
                !name.startsWith("non-VIP") && !name.startsWith("flagged trial")) return
            try {
                main { player.stop() }
                waitFor("stop previous playback") { player.getPlayState() != 4 }
                SystemClock.sleep(650)
                reset()
                restoreControlledAssets?.invoke()
                main { player.setPlayMode(103) }
                block()
                note("PASS $name")
            } catch (t: Throwable) {
                failures += "$name: ${t.message}"
                note("FAIL $name: $t")
            } finally { stopVoice() }
        }
        fun prime(last: Int) {
            assertTrue("need two non-vip songs", normal.size >= 2)
            start(normal[0]); start(normal[1]); start(last)
            assertEquals(3, read("playedNoLoginSongCount"))
        }
        try {
            if (InstrumentationRegistry.getArguments().getString("controlledAssets") == "true") {
                @Suppress("UNCHECKED_CAST") val ready = read("readyFiles") as MutableMap<String, File>
                val itemType = Class.forName("com.tencent.qqmusic.openapisdk.business_common.model.TryBlockVoiceInfo")
                fun items(vip: Boolean): List<*> {
                    val originals = originalPolicy.javaClass.getMethod(if (vip) "getVipBlockInfo" else "getNotVipBlockInfo").invoke(originalPolicy) as List<*>
                    if (originals.all { !it!!.javaClass.getMethod("getVoiceUrl").invoke(it).toString().isBlank() }) return originals
                    val used = ready.values.map { it.absolutePath }.toSet()
                    val file = File(context.cacheDir, "no_login_voice").listFiles().orEmpty().first { it.extension == "mp3" && it.length() > 0 && it.absolutePath !in used }
                    val md5 = MessageDigest.getInstance("MD5").digest(file.readBytes()).joinToString("") { "%02x".format(it.toInt() and 255) }
                    val url = "https://device-test.invalid/${if (vip) "vip" else "nonvip"}.mp3"
                    val item = itemType.getConstructor(String::class.java, String::class.java, String::class.java).newInstance("device test fixture", url, md5)
                    synchronized(read("lock")!!) { ready["$url|$md5"] = file }
                    note("CONTROLLED_ASSET vip=$vip missing URL replaced with cached real MP3=${file.name}, md5=$md5; no server config change")
                    return listOf(item)
                }
                val notVipItems = items(false)
                val vipItems = items(true)
                val ctor = policyType.declaredConstructors.first { it.parameterTypes.size == 5 }
                policySetter.invoke(commonConfig, ctor.newInstance(3, 3, 3, notVipItems, vipItems))
                injectedAssets = true
            }
            val actualPolicy = policy()
            if (injectedAssets) {
                val readySnapshot = synchronized(read("lock")!!) { HashMap(read("readyFiles") as Map<String, File>) }
                restoreControlledAssets = {
                    val currentConfig = configClass.getDeclaredField("commonConfig").apply { isAccessible = true }.get(null)!!
                    policySetter.invoke(currentConfig, actualPolicy)
                    synchronized(read("lock")!!) {
                        @Suppress("UNCHECKED_CAST") val ready = read("readyFiles") as MutableMap<String, File>
                        ready.putAll(readySnapshot)
                    }
                }
            }
            note("POLICY " + listOf("getPlayTime", "getIntervalSongs", "getPlayNum", "getNotVipBlockInfo", "getVipBlockInfo").joinToString { name ->
                val value = actualPolicy.javaClass.getMethod(name).invoke(actualPolicy)
                "$name=${if (value is List<*>) value.size else value}"
            })
            note("CACHE ready=${(read("readyFiles") as Map<*, *>).size} files=" + File(context.cacheDir, "no_login_voice").listFiles().orEmpty().joinToString { "${it.name}:${it.length()}" })
            val requiredKeys = resourceKeys(false) + resourceKeys(true)
            for (vip in listOf(false, true)) {
                val key = resourceKeys(vip).single()
                val address = key.substringBeforeLast('|')
                note("RESOURCE_KIND vip=$vip urlPresent=${address.isNotBlank() && address != "null"} urlLength=${address.length}")
                if (address.isNotBlank() && address != "null" && !address.contains("device-test.invalid")) {
                    try {
                        val connection = java.net.URL(address).openConnection() as java.net.HttpURLConnection
                        connection.connectTimeout = 8000
                        connection.readTimeout = 8000
                        connection.requestMethod = "GET"
                        note("RESOURCE_HTTP vip=$vip host=${java.net.URI(address).host} status=${connection.responseCode} length=${connection.contentLength}")
                        connection.disconnect()
                    } catch (t: Throwable) { note("RESOURCE_HTTP vip=$vip error=${t.javaClass.simpleName}") }
                }
            }
            for (key in requiredKeys) {
                val hash = MessageDigest.getInstance("SHA-256").digest(key.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) }
                val cached = File(context.cacheDir, "no_login_voice").listFiles().orEmpty().firstOrNull { it.name.startsWith(hash) }
                val actualMd5 = cached?.let { MessageDigest.getInstance("MD5").digest(it.readBytes()).joinToString("") { byte -> "%02x".format(byte.toInt() and 255) } }
                note("RESOURCE hash=$hash expectedMd5=${key.substringAfterLast('|')} actualMd5=$actualMd5 size=${cached?.length()}")
            }
            note("RESOURCE distinctConfigured=${requiredKeys.size}")
            waitFor("downloaded voice", 60000) { (read("readyFiles") as Map<*, *>).keys.containsAll(requiredKeys) }
            var checkedMd5 = 0
            (read("readyFiles") as Map<*, *>).forEach { (key, value) ->
                val expected = (key as String).substringAfterLast('|')
                if (expected.isNotBlank()) {
                    val actual = MessageDigest.getInstance("MD5").digest((value as File).readBytes()).joinToString("") { "%02x".format(it.toInt() and 255) }
                    assertEquals(expected.lowercase(), actual)
                    checkedMd5++
                }
            }
            note("ASSETS downloaded files verified against configured MD5: count=$checkedMd5")
            scenario("non-VIP range, start delay, pause and resume") {
                prime(normal[0])
                val song = player.getCurrentSongInfo()!!
                assertTrue(player.getCurrentPlayTime() >= (song.tryBegin ?: 0))
                SystemClock.sleep(1000)
                assertEquals(0, read("playedCount"))
                assertNull(read("voicePlayer"))
                main { player.pause(false) }
                waitFor("paused before voice") { player.getPlayState() == 5 && read("playVoiceJob") == null }
                SystemClock.sleep(4300)
                assertEquals(0, read("playedCount"))
                assertNull(read("voicePlayer"))
                val resumedAt = SystemClock.elapsedRealtime()
                main { player.play(false) }
                val voice = readyVoice(false)
                val delay = SystemClock.elapsedRealtime() - resumedAt
                assertTrue("start delay=$delay", delay >= 3800 && delay < 9000)
                assertEquals(song.songId, player.getCurrentSongInfo()?.songId)
                note("TIMING resume to onPlay=${delay}ms, same song=${song.songId}")
                main { player.pause(false) }
                waitFor("pause stops active voice") { player.getPlayState() == 5 && commonVoice(voice) == null && read("voicePlayer") == null }
                main { player.play(false) }
                waitFor("music resumed") { player.getPlayState() == 4 }
                SystemClock.sleep(4500)
                assertEquals(1, read("playedCount"))
                assertNull(read("voicePlayer"))
            }
            scenario("voice interrupted by next song") {
                prime(normal[0])
                val voice = readyVoice(false)
                val songId = player.getCurrentSongInfo()?.songId
                main { player.next() }
                waitFor("manual next stops voice") { player.getCurrentSongInfo()?.songId != songId && player.getPlayState() == 4 && commonVoice(voice) == null }
            }
            scenario("switch before delay cancels old hint") {
                prime(normal[0])
                SystemClock.sleep(1000)
                start(normal[1])
                SystemClock.sleep(3300)
                assertEquals(0, read("playedCount"))
                readyVoice(false)
                assertEquals(4, read("lastVoiceSongCount"))
            }
            scenario("flagged trial counts and uses VIP hint") {
                assertTrue("need flagged trial song", trial.isNotEmpty())
                prime(trial[0])
                note("TRIAL source=${sourcePlayer().javaClass.simpleName}")
                assertTrue(sourcePlayer().javaClass.simpleName.startsWith("Try2Play"))
                val oldClass = Class.forName("com.tencent.qqmusic.qplayer.core.voiceplay.TryBlockVoiceManager")
                val oldManager = oldClass.getField("INSTANCE").get(null)
                val countField = oldClass.getDeclaredField("playedCount").apply { isAccessible = true }
                val before = countField.get(oldManager)
                readyVoice(player.getCurrentSongInfo()!!.isVip())
                assertEquals(before, countField.get(oldManager))
                assertEquals(1, read("playedCount"))
            }
            scenario("resources not ready: playback continues without late hint") {
                prime(normal[0])
                @Suppress("UNCHECKED_CAST") val ready = read("readyFiles") as MutableMap<String, File>
                val saved = synchronized(read("lock")!!) { HashMap(ready).also { ready.clear() } }
                try {
                    SystemClock.sleep(4600)
                    assertEquals(4, player.getPlayState())
                    assertEquals(0, read("playedCount")); assertNull(read("voicePlayer"))
                    synchronized(read("lock")!!) { ready.putAll(saved) }
                    SystemClock.sleep(1500)
                    assertEquals(0, read("playedCount")); assertNull(read("voicePlayer"))
                    finishAndAdvance()
                } finally { synchronized(read("lock")!!) { ready.putAll(saved) } }
            }
            scenario("single repeat preserves song") {
                prime(normal[0]); main { player.setPlayMode(101) }
                val song = player.getCurrentSongInfo()!!
                main { player.seek(song.tryEnd!! - 1200) }
                waitFor("single restart", 15000) { (read("playedNoLoginSongCount") as Int) > 3 && player.getPlayState() == 4 }
                assertEquals(song.songId, player.getCurrentSongInfo()?.songId)
                readyVoice()
            }
            scenario("single once stops without advancing") {
                prime(normal[0]); main { player.setPlayMode(100) }
                val song = player.getCurrentSongInfo()!!
                main { player.seek(song.tryEnd!! - 1200) }
                waitFor("single completed", 15000) { player.getPlayState() in listOf(6, 7, 8) }
                SystemClock.sleep(700)
                assertEquals(song.songId, player.getCurrentSongInfo()?.songId)
                assertEquals(0, read("playedCount"))
                assertNull(read("playVoiceJob"))
            }
            scenario("seek end and outside range rejected, auto next retained") {
                start(normal[0])
                val song = player.getCurrentSongInfo()!!
                val begin = song.tryBegin!!
                val end = song.tryEnd!!
                main {
                    val atEnd = player.seek(end)
                    val outside = player.seek(end + 1000)
                    note("SEEK_BOUNDARY begin=$begin end=$end atEnd=$atEnd outside=$outside source=${sourcePlayer().javaClass.simpleName}")
                    assertTrue(atEnd < 0); assertTrue(outside < 0)
                    assertTrue(player.seek(begin + 1000) >= 0)
                }
                finishAndAdvance()
            }
            scenario("frequency: first 3 songs, interval 3 songs, daily cap 3") {
                val config = policy()
                for (name in listOf("getPlayTime", "getIntervalSongs", "getPlayNum"))
                    assertEquals("device fixture $name", 3, config.javaClass.getMethod(name).invoke(config))
                for (number in 1..11) {
                    start(normal[(number - 1) % 2])
                    assertEquals(number, read("playedNoLoginSongCount"))
                    val expected = minOf(number / 3, 3)
                    if (number == 3 || number == 6 || number == 9) {
                        waitFor("voice $expected", 9000) { read("playedCount") == expected }
                        assertEquals(number, read("lastVoiceSongCount"))
                    } else SystemClock.sleep(4300)
                    assertEquals(expected, read("playedCount"))
                    note("FREQUENCY songs=$number count=${read("playedCount")}")
                }
            }
            scenario("cross day resets yesterday cap before eligibility check") {
                synchronized(read("lock")!!) {
                    write("lastSavedDay", -1)
                    write("playedCount", 3)
                }
                start(normal[0]); start(normal[1]); start(normal[0])
                readyVoice(false)
                assertEquals(1, read("playedCount"))
                note("CROSS_DAY yesterday count=3 -> today count=${read("playedCount")}")
            }
            scenario("logged-in injection: range retained, voice suppressed") {
                val global = Class.forName("com.tencent.qqmusic.openapisdk.business_common.Global")
                @Suppress("UNCHECKED_CAST") val store = global.getDeclaredField("apiImplStore").apply { isAccessible = true }.get(null) as MutableMap<Class<*>, Any>
                val loginType = Class.forName("com.tencent.qqmusic.openapisdk.business_common.login.LoginModuleApi")
                val original = store[loginType]!!
                val publicLogin = Class.forName("com.tencent.qqmusic.openapisdk.core.login.LoginApi")
                val innerLogin = Class.forName("com.tencent.qqmusic.openapisdk.business_common.login.LoginApiInner")
                val proxy = Proxy.newProxyInstance(publicLogin.classLoader, arrayOf(publicLogin)) { _, method, args ->
                    if (method.name == "hasLogin") true else method.invoke(original, *(args ?: emptyArray()))
                }
                val inner = loginType.getDeclaredField("apiInner").apply { isAccessible = true }.get(original)
                val replacement = loginType.getConstructor(publicLogin, innerLogin).newInstance(proxy, inner)
                try {
                    store[loginType] = replacement
                    prime(normal[0])
                    SystemClock.sleep(4600)
                    val song = player.getCurrentSongInfo()!!
                    assertTrue(player.getCurrentPlayTime() >= song.tryBegin!!)
                    finishAndAdvance()
                    assertEquals(0, read("playedCount")); assertNull(read("voicePlayer"))
                } finally { store[loginType] = original }
            }

        } finally {
            reset()
            if (injectedAssets) {
                val currentConfig = configClass.getDeclaredField("commonConfig").apply { isAccessible = true }.get(null)!!
                policySetter.invoke(currentConfig, originalPolicy)
                managerClass.getDeclaredMethod("refreshResources").apply { isAccessible = true }.invoke(manager)
                note("RESTORE original backend policy")
            }
            main {
                player.setPlayMode(originalMode)
                val restore = player.getPlayList().indexOfFirst { it.songId == originalSong }
                if (restore >= 0) player.playPos(restore)
            }
            if (originalSong != null) {
                try { waitFor("restore original song") { player.getCurrentSongInfo()?.songId == originalSong && player.getPlayState() == 4 } } catch (_: Throwable) { }
                main { player.seek(originalPosition); if (!wasPlaying) player.pause(false) }
            }
            reset()
            note("CLEANUP daily voice count=${read("playedCount")}, mode=${player.getPlayMode()}")
            player.unregisterEventListener(listener)
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }
}
