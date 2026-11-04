/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import android.Manifest
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Process
import android.util.Base64
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest
import org.json.JSONException
import org.json.JSONObject
import com.google.android.gms.droidguard.DroidGuardHandle
import org.microg.gms.droidguard.GuardCallback
import org.microg.gms.droidguard.Utils

/** Explicitly enabled local-operator bridge; ordinary application Binder access is unchanged. */
class DroidGuardSessionProvider : ContentProvider() {
    private lateinit var sessions: DroidGuardSessionStore
    private val factory by lazy { NetworkHandleProxyFactory(requireNotNull(context)) }
    private val runtimeLock = Any()

    override fun onCreate(): Boolean {
        sessions = DroidGuardSessionStore(::openNativeHandle)
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val ctx = requireNotNull(context)
        val owner = Binder.getCallingUid()
        ctx.enforceCallingOrSelfPermission(Manifest.permission.DUMP, "DroidGuard server requires local operator access")
        if (owner != Process.myUid() && owner != 2000 && owner != 0) {
            throw SecurityException("DroidGuard server is restricted to the app, shell, or root UID")
        }
        // Check the actual Binder caller before dispatch or changing identity. Remote source is
        // operator-supplied metadata, never proof that an ordinary app can impersonate that package.
        val identity = Binder.clearCallingIdentity()
        try {
            if (method != "close" && !DroidGuardPreferences.isLocalAvailable(ctx)) {
                sessions.clear()
                throw DroidGuardSessionException(503, "Enable local Embedded DroidGuard on the server device")
            }
            val input = decode(arg)
            val response = JSONObject().put("status", "ok")
            when (method) {
                "begin" -> response.put("sessionId", sessions.begin(owner,
                    requiredString(input, "flow", 256), requiredString(input, "source", 256),
                    stringMap(optionalObject(input, "request"), 64)))
                "snapshot" -> {
                    val id = requiredString(input, "sessionId", 64)
                    val result = sessions.snapshot(owner, id, stringMap(optionalObject(input, "data"), 256))
                    if (result.length > MAX_RESULT_CHARS) {
                        sessions.close(owner, id)
                        throw DroidGuardSessionException(502, "Native result is too large")
                    }
                    response.put("result", result)
                }
                "close" -> sessions.close(owner, requiredString(input, "sessionId", 64))
                else -> throw DroidGuardSessionException(400, "Unknown DroidGuard operation")
            }
            return encode(response)
        } catch (e: DroidGuardSessionException) {
            return encode(JSONObject().put("status", "error").put("code", e.code).put("error", e.message))
        } catch (_: IllegalArgumentException) {
            return encode(JSONObject().put("status", "error").put("code", 400).put("error", "Invalid request"))
        } catch (_: JSONException) {
            return encode(JSONObject().put("status", "error").put("code", 400).put("error", "Invalid JSON request"))
        } catch (_: Exception) {
            return encode(JSONObject().put("status", "error").put("code", 502).put("error", "Native DroidGuard operation failed"))
        } finally {
            Binder.restoreCallingIdentity(identity)
        }
    }

    private fun openNativeHandle(flow: String, source: String, parameters: Map<String, String>): DroidGuardHandle {
        val ctx = requireNotNull(context)
        if (!DroidGuardPreferences.isLocalAvailable(ctx)) throw DroidGuardSessionException(503, "Local DroidGuard is disabled")
        val request = DroidGuardResultsRequest()
        for ((key, value) in parameters) {
            when (key) {
                "fd", "networkToUse" -> throw DroidGuardSessionException(400, "Parcelable request fields are not supported")
                "clientVersion", "timeoutMs", "openHandles" -> {
                    val number = value.toIntOrNull() ?: throw DroidGuardSessionException(400, "Invalid integer request field")
                    if (number < 0 || (key == "timeoutMs" && number == 0)) throw DroidGuardSessionException(400, "Invalid integer request field")
                    request.bundle.putInt(key, number)
                }
                else -> request.bundle.putString(key, value)
            }
        }
        synchronized(runtimeLock) {
            HardwareAttestationBlockingProvider.ensureEnabled(DroidGuardPreferences.isHardwareAttestationBlocked(ctx))
            SerialUnflaky.fetch()
            ServiceCallProxy.maySetBlockDumpForService(ctx, "SurfaceFlinger")
            ServiceCallProxy.maySetBlockDumpForService(ctx, "thermalservice")
        }
        val handle = DroidGuardHandleImpl(ctx, source, factory, GuardCallback(ctx, source))
        try {
            // The ordinary client consumes a non-null init reply by loading the returned VM,
            // initializing it once, and closing it before using the retained handle.
            consumeInitReply(handle.initWithRequest(flow, request))
            if (!handle.isReady()) throw DroidGuardSessionException(502, "Native DroidGuard initialization failed")
            return object : DroidGuardHandle {
                @Volatile private var opened = true
                override fun isOpened() = opened && handle.isReady()
                override fun snapshot(data: Map<String, String>): String {
                    check(opened)
                    return Utils.toBase64(handle.snapshot(LinkedHashMap<Any?, Any?>(data)))
                }
                override fun close() {
                    if (opened) {
                        opened = false
                        handle.close()
                    }
                }
            }
        } catch (e: Exception) {
            handle.close()
            throw e
        }
    }

    private fun consumeInitReply(reply: com.google.android.gms.droidguard.internal.DroidGuardInitReply) {
        val pfd = reply.pfd ?: return
        try {
            val bundle = reply.object as? Bundle
                ?: throw DroidGuardSessionException(502, "Unsupported native DroidGuard init reply")
            val vmKey = bundle.getString("h")
                ?: throw DroidGuardSessionException(502, "Native DroidGuard init reply is missing VM key")
            val followUp = factory.createHandle(vmKey, pfd, bundle)
            try {
                followUp.init()
            } finally {
                followUp.close()
            }
        } finally {
            try {
                pfd.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun decode(arg: String?): JSONObject {
        require(arg != null && arg.length <= 65_536)
        val bytes = Base64.decode(arg, Base64.URL_SAFE or Base64.NO_WRAP)
        require(bytes.size <= 48 * 1024)
        return JSONObject(String(bytes, Charsets.UTF_8))
    }

    private fun encode(value: JSONObject) = Bundle().apply {
        putString("response", Base64.encodeToString(value.toString().toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
    }

    private fun requiredString(value: JSONObject, key: String, maxLength: Int): String {
        val text = value.get(key)
        require(text is String && text.isNotBlank() && text.length <= maxLength)
        return text
    }

    private fun optionalObject(value: JSONObject, key: String): JSONObject {
        if (!value.has(key)) return JSONObject()
        return value.getJSONObject(key)
    }

    private fun stringMap(value: JSONObject, maxEntries: Int): Map<String, String> {
        require(value.length() <= maxEntries)
        return linkedMapOf<String, String>().apply {
            val keys = value.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val text = value.get(key)
                require(key.length <= 512 && text is String && text.length <= 32 * 1024)
                put(key, text)
            }
        }
    }

    override fun shutdown() {
        sessions.close()
        super.shutdown()
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException()
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException()
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException()

    companion object {
        private const val MAX_RESULT_CHARS = 256 * 1024
    }
}
