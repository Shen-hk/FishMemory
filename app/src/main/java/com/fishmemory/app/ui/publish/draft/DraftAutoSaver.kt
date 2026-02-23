package com.fishmemory.app.ui.publish.draft

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DraftAutoSaver(
    private val scope: CoroutineScope,
    private val intervalMs: Long = 30_000L,
    private val onAutoSave: suspend () -> Unit,
    private val onStateChanged: (State) -> Unit
) {
    sealed class State {
        data object Dirty : State()
        data object Saving : State()
        data object Saved : State()
        data class Error(val message: String?) : State()
    }

    private val mutex = Mutex()
    @Volatile private var dirty: Boolean = false
    private var loopJob: Job? = null

    fun start() {
        if (loopJob != null) return
        loopJob = scope.launch(Dispatchers.Main.immediate + SupervisorJob()) {
            while (true) {
                delay(intervalMs)
                if (!dirty) continue
                saveNowInternal(isAuto = true)
            }
        }
    }

    fun stop() {
        loopJob?.cancel()
        loopJob = null
    }

    fun markDirty() {
        dirty = true
        onStateChanged(State.Dirty)
    }

    suspend fun saveNow() {
        saveNowInternal(isAuto = false)
    }

    private suspend fun saveNowInternal(isAuto: Boolean) {
        mutex.withLock {
            if (!dirty && isAuto) return
            onStateChanged(State.Saving)
            try {
                onAutoSave()
                dirty = false
                onStateChanged(State.Saved)
            } catch (t: Throwable) {
                dirty = true
                onStateChanged(State.Error(t.message))
            }
        }
    }
}

