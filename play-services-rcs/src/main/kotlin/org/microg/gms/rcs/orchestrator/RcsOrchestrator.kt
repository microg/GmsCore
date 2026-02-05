/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsOrchestrator - Main coordination layer for all RCS components
 * The brain of the RCS service - coordinates all subsystems
 */

package org.microg.gms.rcs.orchestrator

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.microg.gms.rcs.config.ConfigKeys
import org.microg.gms.rcs.config.RcsConfigManager
import org.microg.gms.rcs.connection.ConnectionListener
import org.microg.gms.rcs.connection.RcsConnectionManager
import org.microg.gms.rcs.ProvisioningResult
import org.microg.gms.rcs.di.RcsServiceLocator
import org.microg.gms.rcs.error.RcsErrorHandler
import org.microg.gms.rcs.events.RcsEventBus
import org.microg.gms.rcs.events.RegistrationStateChangedEvent
import org.microg.gms.rcs.logging.RcsLogger
import org.microg.gms.rcs.metrics.RcsMetricNames
import org.microg.gms.rcs.metrics.RcsMetricsCollector
import org.microg.gms.rcs.RcsProvisioningManager
import org.microg.gms.rcs.state.RcsEvent
import org.microg.gms.rcs.state.RcsState
import org.microg.gms.rcs.state.RcsStateMachine
import org.microg.gms.rcs.state.StateChangeListener
import java.util.concurrent.atomic.AtomicBoolean

class RcsOrchestrator private constructor(private val context: Context) {

    companion object {
        private const val TAG = "RcsOrchestrator"
        
        @Volatile
        private var instance: RcsOrchestrator? = null
        
        fun getInstance(context: Context): RcsOrchestrator {
            return instance ?: synchronized(this) {
                instance ?: RcsOrchestrator(context.applicationContext).also { instance = it }
            }
        }
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val stateMachine = RcsStateMachine()
    private val eventBus = RcsEventBus.getInstance()
    private val errorHandler = RcsErrorHandler.getInstance()
    private val configManager by lazy { RcsConfigManager.getInstance(context) }
    private val metricsCollector by lazy { RcsMetricsCollector.getInstance(context) }
    private val connectionManager by lazy { RcsConnectionManager(context) }
    private val provisioningManager by lazy { RcsProvisioningManager(context) }
    
    private val isInitialized = AtomicBoolean(false)
    private var currentPhoneNumber: String? = null

    fun initialize() {
        if (isInitialized.getAndSet(true)) {
            RcsLogger.w(TAG, "Orchestrator already initialized")
            return
        }
        
        RcsLogger.i(TAG, "Initializing RCS Orchestrator")
        
        RcsServiceLocator.initialize(context)
        
        setupStateMachineListeners()
        setupConnectionManager()
        
        coroutineScope.launch {
            stateMachine.processEvent(RcsEvent.Initialize)
            
            performInitialization()
        }
    }

    private fun setupStateMachineListeners() {
        stateMachine.addListener(object : StateChangeListener {
            override fun onStateChanged(fromState: RcsState, toState: RcsState, event: RcsEvent) {
                RcsLogger.d(TAG, "State: $fromState -> $toState")
                
                when (toState) {
                    RcsState.Disconnected -> handleDisconnectedState()
                    RcsState.Connected -> handleConnectedState()
                    RcsState.Registered -> handleRegisteredState()
                    RcsState.Error -> handleErrorState()
                    else -> {}
                }
            }
        })
    }

    private fun setupConnectionManager() {
        connectionManager.setConnectionListener(object : ConnectionListener {
            override suspend fun onConnecting(): Boolean {
                RcsLogger.d(TAG, "Connection attempt starting")
                return true
            }
            
            override fun onConnected() {
                RcsLogger.i(TAG, "Network connected")
                coroutineScope.launch {
                    stateMachine.processEvent(RcsEvent.ConnectionEstablished)
                }
            }
            
            override fun onDisconnected() {
                RcsLogger.w(TAG, "Network disconnected")
                coroutineScope.launch {
                    stateMachine.processEvent(RcsEvent.ConnectionLost)
                }
            }
            
            override fun onReconnectFailed() {
                RcsLogger.e(TAG, "Reconnection failed")
                coroutineScope.launch {
                    stateMachine.processEvent(RcsEvent.ReconnectFailed)
                }
            }
        })
    }

    private suspend fun performInitialization() {
        try {
            RcsLogger.d(TAG, "Performing initialization checks")
            
            connectionManager.start()
            
            stateMachine.processEvent(RcsEvent.InitializationComplete)
            
            if (configManager.getBoolean(ConfigKeys.AUTO_RECONNECT, true)) {
                stateMachine.processEvent(RcsEvent.Connect)
            }
            
        } catch (e: Exception) {
            RcsLogger.e(TAG, "Initialization failed", e)
            errorHandler.handleException(e, TAG)
            stateMachine.processEvent(RcsEvent.InitializationFailed)
        }
    }

    private fun handleDisconnectedState() {
        eventBus.publish(RegistrationStateChangedEvent(false, null))
    }

    private fun handleConnectedState() {
        coroutineScope.launch {
            if (provisioningManager.isProvisioned()) {
                stateMachine.processEvent(RcsEvent.StartRegistration)
            } else {
                RcsLogger.d(TAG, "Not provisioned, starting provisioning")
                startProvisioning()
            }
        }
    }

    private fun handleRegisteredState() {
        metricsCollector.incrementCounter(RcsMetricNames.REGISTRATION_SUCCESS)
        eventBus.publish(RegistrationStateChangedEvent(true, currentPhoneNumber))
        RcsLogger.i(TAG, "RCS registration successful for $currentPhoneNumber")
    }

    private fun handleErrorState() {
        RcsLogger.e(TAG, "RCS entered error state")
    }

    private fun startProvisioning() {
        coroutineScope.launch {
            try {
                metricsCollector.incrementCounter(RcsMetricNames.REGISTRATION_ATTEMPTS)
                
                val result = provisioningManager.provision()
                
                if (result.isSuccessful) {
                    currentPhoneNumber = result.phoneNumber
                    stateMachine.processEvent(RcsEvent.StartRegistration)
                    performRegistration()
                } else {
                    RcsLogger.e(TAG, "Provisioning failed: ${result.errorMessage}")
                    stateMachine.processEvent(RcsEvent.RegistrationFailed)
                }
            } catch (e: Exception) {
                errorHandler.handleException(e, TAG)
                stateMachine.processEvent(RcsEvent.RegistrationFailed)
            }
        }
    }

    private suspend fun performRegistration() {
        try {
            RcsLogger.d(TAG, "Starting SIP registration flow")
            
            // 1. Load SIP Configuration from Provisioning
            val sipConfig = provisioningManager.loadSipConfiguration()
            if (sipConfig == null) {
                RcsLogger.e(TAG, "Failed to load SIP configuration")
                stateMachine.processEvent(RcsEvent.RegistrationFailed)
                return
            }
            
            // 2. Initialize SIP Client
            // Note: In a real DI system we'd inject this, but for now we create/get it here
            // to ensure meaningful connection.
            val sipClient = org.microg.gms.rcs.sip.RcsSipClient(context, sipConfig)
            
            // 3. Connect Socket
            RcsLogger.d(TAG, "Connecting to SIP server...")
            val connected = sipClient.connect()
            if (!connected) {
                RcsLogger.e(TAG, "Failed to connect to SIP socket")
                stateMachine.processEvent(RcsEvent.RegistrationFailed)
                return
            }
            
            // 4. Register
            val phoneNumber = currentPhoneNumber ?: sipConfig.userPhoneNumber
            val imei = org.microg.gms.rcs.DeviceIdentifierHelper.getImei(context) ?: "0000000000000000"
            
            RcsLogger.d(TAG, "Sending SIP REGISTER for $phoneNumber")
            val result = sipClient.register(phoneNumber, imei)
            
            if (result.isSuccessful) {
                RcsLogger.i(TAG, "SIP Registration Successful!")
                // Store client reference for later use (messaging)
                RcsServiceLocator.registerSipClient(sipClient)
                stateMachine.processEvent(RcsEvent.RegistrationComplete)
            } else {
                RcsLogger.e(TAG, "SIP Registration Failed: ${result.errorCode} - ${result.errorMessage}")
                sipClient.disconnect()
                stateMachine.processEvent(RcsEvent.RegistrationFailed)
            }
            
        } catch (e: Exception) {
            RcsLogger.e(TAG, "Registration exception", e)
            errorHandler.handleException(e, TAG)
            stateMachine.processEvent(RcsEvent.RegistrationFailed)
        }
    }

    fun getCurrentState(): RcsState = stateMachine.getCurrentState()

    fun isRegistered(): Boolean = stateMachine.isInState(RcsState.Registered)

    fun isConnected(): Boolean = stateMachine.isInState(
        RcsState.Connected,
        RcsState.Registering,
        RcsState.Registered
    )

    fun getPhoneNumber(): String? = currentPhoneNumber

    fun shutdown() {
        coroutineScope.launch {
            RcsLogger.i(TAG, "Shutting down RCS Orchestrator")
            
            stateMachine.processEvent(RcsEvent.Shutdown)
            connectionManager.stop()
            
            stateMachine.processEvent(RcsEvent.ShutdownComplete)
            
            isInitialized.set(false)
            instance = null
        }
    }

    fun forceReconnect() {
        coroutineScope.launch {
            RcsLogger.d(TAG, "Force reconnecting")
            connectionManager.forceReconnect()
        }
    }

    fun forceReregister() {
        coroutineScope.launch {
            if (stateMachine.isInState(RcsState.Registered)) {
                stateMachine.processEvent(RcsEvent.Deregister)
                kotlinx.coroutines.delay(1000)
            }
            stateMachine.processEvent(RcsEvent.StartRegistration)
            performRegistration()
        }
    }
}
