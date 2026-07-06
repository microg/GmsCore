package com.google.android.gms.rcs

import android.content.Context

object RcsBackend {

    private var instance: RcsBackend? = null

    fun getInstance(context: Context): RcsBackend {
        if (instance == null) {
            instance = RcsBackend()
        }
        return instance!!
    }

    fun register(identity: String) {
        // Stub: would connect to a local RCS server or proxy
    }

    fun unregister() {
        // Stub
    }
}