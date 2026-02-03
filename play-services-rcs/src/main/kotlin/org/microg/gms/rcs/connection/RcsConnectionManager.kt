/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsConnectionManager - Connection lifecycle and reconnection
 */

package org.microg.gms.rcs.connection

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.microg.gms.rcs.events.ConnectionStateChangedEvent
import org.microg.gms.rcs.events.RcsEventBus
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min
import kotlin.math.pow

class RcsConnectionManager(private val context: Context) {

    companion object {
        private const val TAG = "RcsConnection"
        private const val INITIAL_RECONNECT_DELAY_MS = 1000L
        private const val MAX_RECONNECT_DELAY_MS = 300000L
        private const val MAX_RECONNECT_ATTEMPTS = 10
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val eventBus = RcsEventBus.getInstance()
    
    private val isConnected = AtomicBoolean(false)
    private val isConnecting = AtomicBoolean(false)
    private val reconnectAttempts = AtomicInteger(0)
    
    private var currentNetwork: Network? = null
    private var reconnectJob: Job? = null
    private var connectionListener: ConnectionListener? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Network available")
            currentNetwork = network
            handleNetworkAvailable()
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "Network lost")
            if (currentNetwork == network) {
                currentNetwork = null
                handleNetworkLost()
            }
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            Log.d(TAG, "Network capabilities changed: hasInternet=$hasInternet")
        }
    }

    fun start() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        
        Log.d(TAG, "Connection manager started")
    }

    fun stop() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering callback", e)
        }
        
        reconnectJob?.cancel()
        isConnected.set(false)
        isConnecting.set(false)
        
        Log.d(TAG, "Connection manager stopped")
    }

    private fun handleNetworkAvailable() {
        reconnectAttempts.set(0)
        
        if (!isConnected.get() && !isConnecting.get()) {
            initiateConnection()
        }
        
        eventBus.publish(ConnectionStateChangedEvent(true, getNetworkType()))
    }

    private fun handleNetworkLost() {
        isConnected.set(false)
        
        eventBus.publish(ConnectionStateChangedEvent(false, null))
        connectionListener?.onDisconnected()
        
        scheduleReconnect()
    }

    private fun initiateConnection() {
        if (isConnecting.getAndSet(true)) {
            return
        }
        
        coroutineScope.launch {
            try {
                Log.d(TAG, "Initiating connection...")
                
                val success = connectionListener?.onConnecting() ?: true
                
                if (success) {
                    isConnected.set(true)
                    reconnectAttempts.set(0)
                    connectionListener?.onConnected()
                    Log.d(TAG, "Connection established")
                } else {
                    scheduleReconnect()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                scheduleReconnect()
            } finally {
                isConnecting.set(false)
            }
        }
    }

    private fun scheduleReconnect() {
        val attempts = reconnectAttempts.incrementAndGet()
        
        if (attempts > MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnect attempts exceeded")
            connectionListener?.onReconnectFailed()
            return
        }
        
        val delayMs = calculateBackoffDelay(attempts)
        Log.d(TAG, "Scheduling reconnect in ${delayMs}ms (attempt $attempts)")
        
        reconnectJob?.cancel()
        reconnectJob = coroutineScope.launch {
            delay(delayMs)
            
            if (currentNetwork != null && !isConnected.get()) {
                initiateConnection()
            }
        }
    }

    private fun calculateBackoffDelay(attempt: Int): Long {
        val exponentialDelay = INITIAL_RECONNECT_DELAY_MS * 2.0.pow(attempt.toDouble())
        val jitter = (Math.random() * 0.3 * exponentialDelay).toLong()
        return min((exponentialDelay + jitter).toLong(), MAX_RECONNECT_DELAY_MS)
    }

    private fun getNetworkType(): String {
        val network = currentNetwork ?: return "unknown"
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "unknown"
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            else -> "unknown"
        }
    }

    fun isConnected(): Boolean = isConnected.get()

    fun setConnectionListener(listener: ConnectionListener) {
        this.connectionListener = listener
    }

    fun forceReconnect() {
        reconnectAttempts.set(0)
        isConnected.set(false)
        initiateConnection()
    }
}

interface ConnectionListener {
    suspend fun onConnecting(): Boolean
    fun onConnected()
    fun onDisconnected()
    fun onReconnectFailed()
}
