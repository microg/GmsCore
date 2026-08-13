package org.microg.gms.utils

import org.microg.gms.base.core.R
import java.util.Locale

object AppPatcherDetector {

    // TODO: Also need implement detection using apk signature.

    private val KNOWN_PACKAGES = listOf(
        ".morphe.android" to R.string.morphe,
        ".vanced.android" to R.string.vanced,
        ".revanced.android" to R.string.revanced,
        ".rex.android" to R.string.youtube_advanced,
        ".rvx.android" to R.string.revanced_extended,
        ".rve.android" to R.string.revanced_extended_rufusin,
        "anddea.youtube" to R.string.revanced_extended_anddea,
        "bill.youtube" to R.string.revanced_extended_anddea
    )

    private val BLACKLIST_PACKAGES = listOf(
        "app.revanced.android.gms",
        "app.morphe.android.gms", // If it's happening in future
        "app.morphe.manager",
        "app.revanced.manager",
        "app.rvx.manager"
    )

    fun getUsingPackageName(packageName: String?): Int? {
        if (packageName.isNullOrEmpty()) return null
        val pkgLower = packageName.lowercase(Locale.ROOT)

        if (BLACKLIST_PACKAGES.any { pkgLower == it }) return null
        return KNOWN_PACKAGES.firstOrNull { pkgLower.contains(it.first) }?.second
    }
}