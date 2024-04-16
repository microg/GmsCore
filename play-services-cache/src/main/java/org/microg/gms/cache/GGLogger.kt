package org.microg.gms.cache

import android.os.Build
import java.util.logging.Level
import java.util.logging.Logger

internal object GGLogger {

    var IS_ENABLED = false //TODO set it false in production mode
    fun d(param: String) {
        if (IS_ENABLED) if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Logger.getGlobal().log(Level.INFO, "GGLogger: $param")
        }
    }

    fun d(param: String, t: Throwable) {
        if (IS_ENABLED) if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Logger.getGlobal().log(Level.INFO, "GGLogger: $param $t")
        }
    }
}