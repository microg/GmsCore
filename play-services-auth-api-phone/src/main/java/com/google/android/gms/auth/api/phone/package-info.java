/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */
/**
 * {@code SmsRetriever} contains two APIs, the SMS Retriever API and the SMS User Consent API, that provide access to Google
 * services that help you retrieve SMS messages directed to your app, without having to ask for
 * {@code android.permission.READ_SMS} or {@code android.permission.RECEIVE_SMS}. The {@code SmsCodeRetriever} is for autofill
 * services and browser apps to retrieve SMS-based verification codes.
 * <p>
 * Many apps use phone numbers to verify the identity of a user. The app sends an SMS message containing an OTP (One
 * Time Passcode) to the user, who then enters the OTP from the received SMS message to prove ownership of the phone number.
 * <p>
 * In Android, to provide a streamlined UX, an app may request the SMS read permission, and retrieve the OTP
 * automatically. This is problematic since this permission allows the app to read other SMS messages which may contain
 * the user's private information. Also, the latest Play Store policy changes restrict access to SMS messages.
 * <p>
 * The SMS Retriever API solves this problem by providing app developers a way to automatically retrieve only the SMS
 * directed to the app without asking for the SMS read permission or gaining the ability to read any other SMS messages on the device.
 * <p>
 * The SMS User Consent API complements the SMS Retriever API by allowing an app to prompt the user to grant access to
 * the content of the next SMS message that contains an OTP. When a user gives consent, the app will then have access to
 * the entire message body to automatically complete SMS verification.
 * <p>
 * The SMS Retriever API completely automates the SMS-based OTP verification process for the user. However, there are
 * situations where you don’t control the format of the SMS message and as a result cannot use the SMS Retriever API.
 * In these situations, you can use the SMS User Consent API to streamline the process.
 * <p>
 * With the SMS Code Autofill API, a user-designated autofill service can retrieve the SMS verification codes from the SMS
 * inbox or new incoming SMS messages, then fill in this code for a user to complete any SMS verification requests in a
 * user app. For browser apps, you can achieve this by using the SMS Code Browser API.
 */
package com.google.android.gms.auth.api.phone;
