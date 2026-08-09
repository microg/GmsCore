/*
 * Simple reference Remote DroidGuard server (for illustration).
 * Package this into an Android app + foreground service.
 * Requires: implementation of a HTTP server (NanoHTTPD, Ktor, etc.)
 * On device that has working local DroidGuard (stock or properly modded phone).
 */

import android.content.Context
import android.util.Base64
import com.google.android.gms.droidguard.DroidGuard
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap

// Pseudocode - adapt to your HTTP server library
class RemoteDroidGuardHttpHandler(private val androidContext: Context) {

    private val activeSessions = ConcurrentHashMap<String, String>() // sid -> state

    fun handleRequest(query: Map<String, String>, postBody: String?): String {
        val flow = query["flow"] ?: return "ERROR: no flow"
        val sourcePkg = query["source"] ?: "unknown"
        val sid = query["sid"]
        val action = query["action"]

        val dgRequest = DroidGuardResultsRequest().apply {
            query.filterKeys { it.startsWith("x-request-") }.forEach { (k, v) ->
                val key = k.removePrefix("x-request-")
                // Decode if it looks base64
                val decoded = try { Base64.decode(v, Base64.NO_WRAP) } catch (_: Exception) { null }
                if (decoded != null && decoded.isNotEmpty()) {
                    bundle.putByteArray(key, decoded)
                } else {
                    bundle.putString(key, URLDecoder.decode(v, "UTF-8"))
                }
            }
        }

        val dataMap = mutableMapOf<String, String>()
        postBody?.split("&")?.forEach { pair ->
            val parts = pair.split("=", limit = 2)
            if (parts.size == 2) {
                dataMap[URLDecoder.decode(parts[0], "UTF-8")] =
                    URLDecoder.decode(parts[1], "UTF-8")
            }
        }

        return try {
            when {
                action == "init" -> {
                    // Start multi-step session for Play Integrity
                    val newSid = java.util.UUID.randomUUID().toString()
                    // Optional: actually create DroidGuardHandle here and store it
                    activeSessions[newSid] = flow
                    // Perform an initial getResults or init
                    val initial = DroidGuard.getClient(androidContext)
                        .getResults(flow, dataMap, dgRequest).get()
                    "$newSid|${Base64.encodeToString(initial.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)}"
                }
                action == "close" && sid != null -> {
                    activeSessions.remove(sid)
                    "CLOSED"
                }
                else -> {
                    // Regular snapshot or single step
                    val result = DroidGuard.getClient(androidContext)
                        .getResults(flow, dataMap, dgRequest).get()
                    Base64.encodeToString(result.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                }
            }
        } catch (e: Exception) {
            "ERROR: ${e.javaClass.simpleName}: ${e.message}"
        }
    }
}
