package com.tencent.qqmusic.qplayer.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author haodongyuan on 2018/5/23.
 * @since 0.1
 */
object AppLifeCycleManager : LifecycleEventObserver {
    const val STATE_INITIATING = 1
    const val STATE_STARTED = 2
    const val STATE_STOPPED = 3

    private val appState: AtomicInteger = AtomicInteger(STATE_INITIATING)
    private val observerList = mutableListOf<MusicLifecycleEventObserver>()
    private val eventFlow = MutableSharedFlow<Pair<WeakReference<LifecycleOwner>, Lifecycle.Event>>()
    private var job: Job? = null

    fun initManager() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        job?.cancel()
        job = AppScope.launchIO {
            eventFlow.collect { pair ->
                when (pair.second) {
                    Lifecycle.Event.ON_START -> {
                        appState.getAndSet(STATE_STARTED)
                    }

                    Lifecycle.Event.ON_STOP -> {
                        appState.getAndSet(STATE_STOPPED)
                    }

                    else -> {}
                }
                observerList.map { it.onStateChanged(pair.first.get(), pair.second) }
            }
        }
    }

    fun destroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        observerList.clear()
        job?.cancel()
    }


    fun registerListener(observer: MusicLifecycleEventObserver) {
        observerList.add(observer)
    }


    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) = runBlocking {
        eventFlow.emit(Pair(WeakReference(source), event))
        return@runBlocking
    }
}

fun interface MusicLifecycleEventObserver : LifecycleObserver {
    fun onStateChanged(source: LifecycleOwner?, event: Lifecycle.Event)
}