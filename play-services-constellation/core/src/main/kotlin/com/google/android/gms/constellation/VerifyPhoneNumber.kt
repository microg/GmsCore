package com.google.android.gms.constellation

import org.microg.gms.constellation.core.proto.VerificationMethod

val VerifyPhoneNumberRequest.verificationMethods: List<VerificationMethod>
    get() = verificationMethodsValues.mapNotNull { VerificationMethod.fromValue(it) }