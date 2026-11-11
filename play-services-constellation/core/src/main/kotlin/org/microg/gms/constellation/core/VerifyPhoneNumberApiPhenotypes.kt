package org.microg.gms.constellation.core

object VerifyPhoneNumberApiPhenotypes {
    val PACKAGES_ALLOWED_TO_CALL = listOf(
        "com.google.android.gms",
        "com.google.android.apps.messaging",
        "com.google.android.ims",
        "com.google.android.apps.tachyon",
        "com.google.android.dialer",
        "com.google.android.apps.nbu.paisa.user.dev",
        "com.google.android.apps.nbu.paisa.user.qa",
        "com.google.android.apps.nbu.paisa.user.teamfood2",
        "com.google.android.apps.nbu.paisa.user.partner",
        "com.google.android.apps.nbu.paisa.user",
        "com.google.android.gms.constellation.getiidtoken",
        "com.google.android.gms.constellation.ondemandconsent",
        "com.google.android.gms.constellation.ondemandconsentv2",
        "com.google.android.gms.constellation.readphonenumber",
        "com.google.android.gms.constellation.verifyphonenumberlite",
        "com.google.android.gms.constellation.verifyphonenumber",
        "com.google.android.gms.test",
        "com.google.android.apps.stargate",
        "com.google.android.gms.firebase.fpnv",
        "com.google.firebase.pnv.testapp",
        "com.google.firebase.pnv"
    )
    val POLICY_IDS_ALLOWED_FOR_LOCAL_READ = listOf("emergency_location")
    val READ_ONLY_POLICY_IDS = listOf(
        "business_voice",
        "verifiedsmsconsent",
        "hint",
        "nearbysharing"
    )
    const val ENABLE_CLIENT_SIGNATURE = true
    const val ENABLE_LOCAL_READ_FLOW = true
    const val ENABLE_READ_FLOW = true
}