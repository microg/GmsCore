package org.microg.gms.droidguard.core

import android.annotation.SuppressLint
import android.os.Build.VERSION.SDK_INT
import android.util.Log

/**
 * DroidGuard may invoke Build.getSerial(), which will throw an exception, as, in contrast to original GMS, it doesn't
 * have the permission to do so.
 *
 * We found that on some systems, the behavior of Build.getSerial() is flaky, as it would only throw an exception on
 * the first attempt invoking it, but would return "unknown" afterward (which is intended for apps with
 * target SDK <= 28). DroidGuard doesn't like those flaky results, so to make them consistent, we just invoke
 * Build.getSerial() here once.
 */
object SerialUnflaky {
    @SuppressLint("MissingPermission")
    fun fetch() {
        if (SDK_INT >= 26) {
            val res1 = runCatching { android.os.Build.getSerial() }.fold({ it }, { it.javaClass.name })
            val res2 = runCatching { android.os.Build.getSerial() }.fold({ it }, { it.javaClass.name })
            if (res1 != res2) {
                Log.w("SerialUnflaky", "Build.getSerial() was flaky. res1=$res1, res2=$res2")
            }
        }
    }
}