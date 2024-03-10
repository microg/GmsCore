/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.common

import com.google.android.gms.common.internal.CertData
import org.microg.gms.common.GooglePackagePermission.*
import org.microg.gms.utils.digest
import org.microg.gms.utils.toHexString

private const val SHA1 = "SHA1"
private const val SHA256 = "SHA-256"

val KNOWN_GOOGLE_PACKAGES = mapOf(
    // Legacy set
    // These include all previously KNOWN_GOOGLE_PACKAGES and grant them all google package permissions
    // Those should be replaced by new entries that
    // - use SHA-256 instead of SHA-1
    // - has more accurate permission set (in most cases, ACCOUNT+AUTH+OWNER is sufficient)
    Pair(
        PackageAndCertHash("com.google.android.apps.classroom", SHA1, "46f6c8987311e131f4f558d8e0ae145bebab6da3"),
        GooglePackagePermission.entries.toSet()
    ),
    Pair(
        PackageAndCertHash("com.google.android.apps.inbox", SHA1, "aa87ce1260c008d801197bb4ecea4ab8929da246"),
        GooglePackagePermission.entries.toSet()
    ),
    Pair(
        PackageAndCertHash("com.google.android.apps.playconsole", SHA1, "d6c35e55b481aefddd74152ca7254332739a81d6"),
        GooglePackagePermission.entries.toSet()
    ),
    Pair(
        PackageAndCertHash("com.google.android.apps.travel.onthego", SHA1, "0cbe08032217d45e61c0bc72f294395ee9ecb5d5"),
        GooglePackagePermission.entries.toSet()
    ),
    Pair(
        PackageAndCertHash("com.google.android.apps.tycho", SHA1, "01b844184e360686aa98b48eb16e05c76d4a72ad"),
        GooglePackagePermission.entries.toSet()
    ),
    Pair(
        PackageAndCertHash("com.google.android.contacts", SHA1, "ee3e2b5d95365c5a1ccc2d8dfe48d94eb33b3ebe"),
        GooglePackagePermission.entries.toSet()
    ),
    Pair(
        PackageAndCertHash("com.google.android.wearable.app", SHA1, "a197f9212f2fed64f0ff9c2a4edf24b9c8801c8c"),
        GooglePackagePermission.entries.toSet()
    ),
    Pair(
        PackageAndCertHash("com.google.android.apps.youtube.music", SHA1, "afb0fed5eeaebdd86f56a97742f4b6b33ef59875"),
        GooglePackagePermission.entries.toSet()
    ),
    Pair(
        PackageAndCertHash("com.google.android.vr.home", SHA1, "fc1edc68f7e3e4963c998e95fc38f3de8d1bfc96"),
        GooglePackagePermission.entries.toSet()
    ),
    Pair(
        PackageAndCertHash("com.google.vr.cyclops", SHA1, "188c5ca3863fa121216157a5baa80755ceda70ab"),
        GooglePackagePermission.entries.toSet()
    ),
    Pair(
        PackageAndCertHash("com.waze", SHA1, "35b438fe1bc69d975dc8702dc16ab69ebf65f26f"),
        GooglePackagePermission.entries.toSet()
    ),
    Pair(
        PackageAndCertHash("com.google.android.apps.wellbeing", SHA1, "4ebdd02380f1fa0b6741491f0af35625dba76e9f"),
        GooglePackagePermission.entries.toSet()
    ),
    Pair(
        PackageAndCertHash("com.google.android.apps.village.boond", SHA1, "48e7985b8f901df335b5d5223579c81618431c7b"),
        GooglePackagePermission.entries.toSet()
    ),
    Pair(
        PackageAndCertHash("com.google.android.apps.subscriptions.red", SHA1, "de8304ace744ae4c4e05887a27a790815e610ff0"),
        GooglePackagePermission.entries.toSet()
    ),
    Pair(
        PackageAndCertHash("com.google.android.apps.meetings", SHA1, "47a6936b733dbdb45d71997fbe1d610eca36b8bf"),
        GooglePackagePermission.entries.toSet()
    ),
    Pair(
        PackageAndCertHash("com.google.android.apps.nbu.paisa.user", SHA1, "80df78bb700f9172bc671779b017ddefefcbf552"),
        GooglePackagePermission.entries.toSet()
    ),
    Pair(
        PackageAndCertHash("com.google.android.apps.dynamite", SHA1, "519c5a17a60596e6fe5933b9cb4285e7b0e5eb7b"),
        GooglePackagePermission.entries.toSet()
    ),
    Pair(
        PackageAndCertHash("com.google.android.projection.gearhead", SHA1, "9ca91f9e704d630ef67a23f52bf1577a92b9ca5d"),
        GooglePackagePermission.entries.toSet()
    ),
    Pair(
        PackageAndCertHash("com.google.stadia.android", SHA1, "133aad3b3d3b580e286573c37f20549f9d3d1cce"),
        GooglePackagePermission.entries.toSet()
    ),
    Pair(
        PackageAndCertHash("com.google.android.apps.kids.familylink", SHA1, "88652b8464743e5ce80da0d4b890d13f9b1873df"),
        GooglePackagePermission.entries.toSet()
    ),
    Pair(
        PackageAndCertHash("com.google.android.apps.walletnfcrel", SHA1, "82759e2db43f9ccbafce313bc674f35748fabd7a"),
        GooglePackagePermission.entries.toSet()
    ),
    Pair(
        PackageAndCertHash("com.google.android.apps.recorder", SHA1, "394d84cd2cf89d3453702c663f98ec6554afc3cd"),
        GooglePackagePermission.entries.toSet()
    ),
    Pair(
        PackageAndCertHash("com.google.android.apps.messaging", SHA1, "0980a12be993528c19107bc21ad811478c63cefc"),
        GooglePackagePermission.entries.toSet()
    ),
    Pair(
        PackageAndCertHash("com.google.android.apps.tachyon", SHA1, "a0bc09af527b6397c7a9ef171d6cf76f757becc3"),
        GooglePackagePermission.entries.toSet()
    ),
    Pair(
        PackageAndCertHash("com.google.android.apps.access.wifi.consumer", SHA1,"d850379540d68fbec82a742ab6a8321a3f9a4c7c"),
        GooglePackagePermission.entries.toSet()
    ),

    // New
    Pair(
        PackageAndCertHash("com.google.android.apps.jam", SHA256, "9db7ff389ab6a30d5f5c92a8629ff0baa93fa8430f0503c04d72640a1cf323f5"),
        setOf(ACCOUNT, AUTH, OWNER)
    )
)

// These are SHA-256 hashes of the Google privileged signing certificates
val KNOWN_GOOGLE_PRIVILEGED_CERT_HASHES = listOf(
    "f0fd6c5b410f25cb25c3b53346c8972fae30f8ee7411df910480ad6b2d60db83",
    "7ce83c1b71f3d572fed04c8d40c5cb10ff75e6d87d9df6fbd53f0468c2905053",
)

enum class GooglePackagePermission {
    ACCOUNT, // Find accounts
    AD_ID, // Advertising ID
    APP_CERT, // Receive certificate confirming valid app installation (incl. Spatula)
    AUTH, // Sign in to Google account without user interface confirmation
    CREDENTIALS, // Access to credentials
    GAMES, // Google Play Games first party access
    IMPERSONATE, // Allow to act as another package
    OWNER, // Details about own accounts (name, email, photo)
    PEOPLE, // Details about contacts
    REPORTING, // Access reporting service
    SAFETYNET, // Access SafetyNet UUID
}

data class PackageAndCertHash(val packageName: String, val algorithm: String, val certHash: String)

fun isGooglePackage(pkg: PackageAndCertHash): Boolean {
    if (pkg.algorithm == SHA256 && pkg.certHash in KNOWN_GOOGLE_PRIVILEGED_CERT_HASHES) return true
    return KNOWN_GOOGLE_PACKAGES.containsKey(pkg)
}
fun getGooglePackagePermissions(pkg: PackageAndCertHash): Set<GooglePackagePermission> {
    if (KNOWN_GOOGLE_PACKAGES.containsKey(pkg)) return KNOWN_GOOGLE_PACKAGES[pkg].orEmpty()
    if (pkg.algorithm == SHA256 && pkg.certHash in KNOWN_GOOGLE_PRIVILEGED_CERT_HASHES) return GooglePackagePermission.entries.toSet()
    return emptySet()
}
fun hasGooglePackagePermission(pkg: PackageAndCertHash, permission: GooglePackagePermission) = getGooglePackagePermissions(pkg).contains(permission)

fun isGooglePackage(packageName: String, certificate: CertData): Boolean =
    listOf(SHA1, SHA256).any { isGooglePackage(PackageAndCertHash(packageName, it, certificate.digest(it).toHexString())) }