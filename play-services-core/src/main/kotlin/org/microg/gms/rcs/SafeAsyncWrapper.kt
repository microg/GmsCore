package org.microg.gms.rcs

import android.util.Log
import kotlinx.coroutines.*

class SafeAsyncWrapper {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun <T> executeSafe(block: suspend () -> T, onError: (Throwable) -> Unit) {
        scope.launch {
            try {
                block()
            } catch (e: Exception) {
                Log.e("SafeAsyncWrapper", "RCS Execution Error caught safely", e)
                onError(e)
            } finally {
                currentCoroutineContext().cancelChildren()
            }
        }
    }

    fun release() {
        scope.cancel()
    }
}