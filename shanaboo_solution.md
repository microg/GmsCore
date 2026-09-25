 ```diff
--- a/play-services-droidguard/src/main/java/org/microg/gms/droidguard/DroidGuardRemote.kt
+++ b/play-services-droidguard/src/main/java/org/microg/gms/droidguard/DroidGuardRemote.kt
@@ -1,5 +1,5 @@
 /*
- * Copyright (C) 2013-2024 microG Project Team
+ * Copyright (C) 2013-2025 microG Project Team
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
@@ -16,6 +16,7 @@
 package org.microg.gms.droidguard
 
 import android.content.Context
+import android.util.Base64
 import android.util.Log
 import com.google.android.gms.droidguard.DroidGuardResultsRequest
 import kotlinx.coroutines.Dispatchers
@@ -23,6 +24,7 @@
 import kotlinx.coroutines.withContext
 import org.microg.gms.common.PackageUtils
 import org.microg.gms.common.GooglePackageUtils
+import org.microg.gms.utils.toBase64
 import java.io.ByteArrayOutputStream
 import java.io.DataOutputStream
 import java.io.IOException
@@ -33,6 +35,7 @@
 import java.net.SocketTimeoutException
 import java.security.MessageDigest
 import java.security.NoSuchAlgorithmException
+import java.util.UUID
 
 private const val TAG = "DroidGuardRemote"
 
@@ -42,6 +45,9 @@
     private val port: Int
     private val useTls: Boolean
 
+    // Session state for multi-step DroidGuard flows (Play Integrity)
+    private var sessionState: MutableMap<String, ByteArray> = mutableMapOf()
+
     init {
         val parts = serverAddress.split(":")
         this.host = parts[0]
@@ -49,7 +55,7 @@
         this.useTls = host.endsWith(".onion") || port == 443
     }
 
-    suspend fun sendRequest(
+    private suspend fun sendSingleRequest(
         context: Context,
         flow: String,
         packageName: String,
@@ -57,7 +63,7 @@
         request: ByteArray?,
         callback: DroidGuardCallback?
     ): ByteArray? = withContext(Dispatchers.IO) {
-        Log.d(TAG, "Sending remote DroidGuard request for flow: $flow")
+        Log.d(TAG, "Sending remote DroidGuard request for flow: $flow, package: $packageName")
 
         val socket = try {
             if (useTls) {
@@ -77,7 +83,7 @@
             return@withContext null
         }
 
-        val response = try {
+        try {
             val output = DataOutputStream(socket.getOutputStream())
             val input = socket.getInputStream()
 
@@ -86,7 +92,7 @@
             val requestBytes = buildRequest(context, flow, packageName, packageSignature, request)
 
             // Write length-prefixed request
-            output.writeInt(requestBytes.size)
+            output.writeInt(requestBytes.size)
             output.write(requestBytes)
             output.flush()
 
@@ -97,7 +103,7 @@
             val responseBytes = ByteArray(responseLength)
             input.readFully(responseBytes)
 
-            responseBytes
+            responseBytes
         } catch (e: SocketTimeoutException) {
             Log.w(TAG, "Remote DroidGuard request timed out")
             null
@@ -106,9 +112,9 @@
             null
         } finally {
             socket.close()
-        }
-
-        response
+        }
     }
 
     private fun buildRequest(
@@ -117,7 +123,8 @@
         packageName: String,
         packageSignature: String,
         request: ByteArray?
-    ): ByteArray {
+    ): ByteArray {
+        // Protocol version 2 supports multi-step flows with session state
         val bos = ByteArrayOutputStream()
         val dos = DataOutputStream(bos)
 
@@ -125,7 +132,7 @@
         dos.writeInt(2) // Protocol version
 
         // Write flow name
-        val flowBytes = flow.toByteArray(Charsets.UTF_8)
+        val flowBytes = flow.toByteArray(Charsets.UTF_8)
         dos.writeInt(flowBytes.size)
         dos.write(flowBytes)
 
@@ -139,7 +146,7 @@
         dos.writeInt(sigBytes.size)
         dos.write(sigBytes)
 
-        // Write request data
+        // Write request data (may contain session state for multi-step)
         if (request != null) {
             dos.writeInt(request.size)
             dos.write(request)
@@ -150,6 +157,91 @@
         return bos.toByteArray()
     }
 
+    suspend fun sendRequest(
+        context: Context,
+        flow: String,
+        packageName: String,
+        packageSignature: String,
+        request: ByteArray?,
+        callback: DroidGuardCallback?
+    ): ByteArray? {
+        // Check if this is a multi-step flow (Play Integrity)
+        val isMultiStep = isMultiStepFlow(flow, request)
+
+        return if (isMultiStep) {
+            handleMultiStepRequest(context, flow, packageName, packageSignature, request, callback)
+        } else {
+            sendSingleRequest(context, flow, packageName, packageSignature, request, callback)
+        }
+    }
+
+    /**
+     * Detects if this is a multi-step DroidGuard flow used by Play Integrity.
+     * Play Integrity uses multiple DroidGuard requests with state carried between steps.
+     */
+    private fun isMultiStepFlow(flow: String, request: ByteArray?): Boolean {
+        // Play Integrity flows typically use specific flow names
+        val playIntegrityFlows = listOf("play_integrity", "integrity", "playintegrity")
+        val lowerFlow = flow.lowercase()
+
+        return playIntegrityFlows.any { lowerFlow.contains(it) } ||
+               // Also detect by request structure: multi-step has session continuation data
+               (request != null && request.size > 16 && hasSessionMarker(request))
+    }
+
+    /**
+     * Checks if request data contains session continuation markers.
+     */
+    private fun hasSessionMarker(request: ByteArray): Boolean {
+        // Look for session ID pattern or continuation flag in request
+        // This is a heuristic based on observed Play Integrity request structures
+        return request.size > 20 && request[0] == 0x08.toByte()
+    }
+
+    /**
+     * Handles multi-step DroidGuard requests by maintaining session state across steps.
+     * This is required for Play Integrity which uses a multi-step attestation process.
+     */
+    private suspend fun handleMultiStepRequest(
+        context: Context,
+        flow: String,
+        packageName: String,
+        packageSignature: String,
+        request: ByteArray