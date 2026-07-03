++ b/play-services-constellation/core/src/main/kotlin/com/google/android/gms/constellation/VerifyPhoneNumber.kt
package com.google.android.gms.constellation

import org.microg.gms.constellation.core.proto.VerificationMethod

val VerifyPhoneNumberRequest.verificationMethods: List<VerificationMethod>
    get() = verificationMethodsValues.mapNotNull { VerificationMethod.fromValue(it) }
