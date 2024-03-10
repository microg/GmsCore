package org.microg.vending.billing.core

data class AuthData(
    val email: String,
    val authToken: String,
    val gsfId: String = "",
    val deviceCheckInConsistencyToken: String = "",
    val deviceConfigToken: String = "",
    val experimentsConfigToken: String = "",
    val dfeCookie: String = ""
)
