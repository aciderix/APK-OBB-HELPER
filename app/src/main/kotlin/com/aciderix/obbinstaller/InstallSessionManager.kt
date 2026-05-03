package com.aciderix.obbinstaller

import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap

sealed class InstallResult {
    data object Success : InstallResult()
    data class Failure(val message: String) : InstallResult()
}

object InstallSessionManager {
    private val pending = ConcurrentHashMap<Int, CompletableDeferred<InstallResult>>()

    fun register(sessionId: Int): CompletableDeferred<InstallResult> {
        val d = CompletableDeferred<InstallResult>()
        pending[sessionId] = d
        return d
    }

    fun deliver(sessionId: Int, result: InstallResult) {
        pending.remove(sessionId)?.complete(result)
    }
}
