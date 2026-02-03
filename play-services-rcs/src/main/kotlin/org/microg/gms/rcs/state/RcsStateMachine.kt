/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsStateMachine - Finite state machine for RCS lifecycle management
 * 
 * Implements proper state transitions with guards and side effects.
 * Ensures the RCS service is always in a valid, predictable state.
 * Prevents invalid state transitions that could cause crashes.
 */

package org.microg.gms.rcs.state

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.CopyOnWriteArrayList

class RcsStateMachine {

    companion object {
        private const val TAG = "RcsStateMachine"
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val transitionMutex = Mutex()
    
    private val mutableCurrentState = MutableStateFlow<RcsState>(RcsState.Uninitialized)
    val currentState: StateFlow<RcsState> = mutableCurrentState.asStateFlow()
    
    private val stateHistory = mutableListOf<StateTransitionRecord>()
    private val listeners = CopyOnWriteArrayList<StateChangeListener>()
    
    private val transitionTable = mapOf(
        // From Uninitialized
        TransitionKey(RcsState.Uninitialized, RcsEvent.Initialize) to TransitionRule(
            targetState = RcsState.Initializing,
            guard = { true },
            sideEffect = { Log.d(TAG, "Starting initialization") }
        ),
        
        // From Initializing
        TransitionKey(RcsState.Initializing, RcsEvent.InitializationComplete) to TransitionRule(
            targetState = RcsState.Disconnected,
            guard = { true },
            sideEffect = { Log.d(TAG, "Initialization complete") }
        ),
        TransitionKey(RcsState.Initializing, RcsEvent.InitializationFailed) to TransitionRule(
            targetState = RcsState.Error,
            guard = { true },
            sideEffect = { Log.e(TAG, "Initialization failed") }
        ),
        
        // From Disconnected
        TransitionKey(RcsState.Disconnected, RcsEvent.Connect) to TransitionRule(
            targetState = RcsState.Connecting,
            guard = { true },
            sideEffect = { Log.d(TAG, "Starting connection") }
        ),
        TransitionKey(RcsState.Disconnected, RcsEvent.Shutdown) to TransitionRule(
            targetState = RcsState.ShuttingDown,
            guard = { true },
            sideEffect = { Log.d(TAG, "Shutting down from disconnected") }
        ),
        
        // From Connecting
        TransitionKey(RcsState.Connecting, RcsEvent.ConnectionEstablished) to TransitionRule(
            targetState = RcsState.Connected,
            guard = { true },
            sideEffect = { Log.d(TAG, "Connection established") }
        ),
        TransitionKey(RcsState.Connecting, RcsEvent.ConnectionFailed) to TransitionRule(
            targetState = RcsState.Disconnected,
            guard = { true },
            sideEffect = { Log.w(TAG, "Connection failed, returning to disconnected") }
        ),
        TransitionKey(RcsState.Connecting, RcsEvent.Timeout) to TransitionRule(
            targetState = RcsState.Disconnected,
            guard = { true },
            sideEffect = { Log.w(TAG, "Connection timeout") }
        ),
        
        // From Connected
        TransitionKey(RcsState.Connected, RcsEvent.StartRegistration) to TransitionRule(
            targetState = RcsState.Registering,
            guard = { true },
            sideEffect = { Log.d(TAG, "Starting registration") }
        ),
        TransitionKey(RcsState.Connected, RcsEvent.Disconnect) to TransitionRule(
            targetState = RcsState.Disconnecting,
            guard = { true },
            sideEffect = { Log.d(TAG, "Disconnecting") }
        ),
        TransitionKey(RcsState.Connected, RcsEvent.ConnectionLost) to TransitionRule(
            targetState = RcsState.Reconnecting,
            guard = { true },
            sideEffect = { Log.w(TAG, "Connection lost, attempting reconnect") }
        ),
        
        // From Registering
        TransitionKey(RcsState.Registering, RcsEvent.RegistrationComplete) to TransitionRule(
            targetState = RcsState.Registered,
            guard = { true },
            sideEffect = { Log.i(TAG, "Registration successful") }
        ),
        TransitionKey(RcsState.Registering, RcsEvent.RegistrationFailed) to TransitionRule(
            targetState = RcsState.Connected,
            guard = { true },
            sideEffect = { Log.w(TAG, "Registration failed, returning to connected") }
        ),
        TransitionKey(RcsState.Registering, RcsEvent.AuthenticationRequired) to TransitionRule(
            targetState = RcsState.Authenticating,
            guard = { true },
            sideEffect = { Log.d(TAG, "Authentication required") }
        ),
        
        // From Authenticating
        TransitionKey(RcsState.Authenticating, RcsEvent.AuthenticationSuccess) to TransitionRule(
            targetState = RcsState.Registering,
            guard = { true },
            sideEffect = { Log.d(TAG, "Authentication successful, resuming registration") }
        ),
        TransitionKey(RcsState.Authenticating, RcsEvent.AuthenticationFailed) to TransitionRule(
            targetState = RcsState.Connected,
            guard = { true },
            sideEffect = { Log.e(TAG, "Authentication failed") }
        ),
        
        // From Registered
        TransitionKey(RcsState.Registered, RcsEvent.Deregister) to TransitionRule(
            targetState = RcsState.Deregistering,
            guard = { true },
            sideEffect = { Log.d(TAG, "Starting deregistration") }
        ),
        TransitionKey(RcsState.Registered, RcsEvent.ConnectionLost) to TransitionRule(
            targetState = RcsState.Reconnecting,
            guard = { true },
            sideEffect = { Log.w(TAG, "Connection lost while registered") }
        ),
        TransitionKey(RcsState.Registered, RcsEvent.RegistrationExpired) to TransitionRule(
            targetState = RcsState.Registering,
            guard = { true },
            sideEffect = { Log.d(TAG, "Registration expired, re-registering") }
        ),
        TransitionKey(RcsState.Registered, RcsEvent.Shutdown) to TransitionRule(
            targetState = RcsState.ShuttingDown,
            guard = { true },
            sideEffect = { Log.d(TAG, "Shutting down from registered state") }
        ),
        
        // From Deregistering
        TransitionKey(RcsState.Deregistering, RcsEvent.DeregistrationComplete) to TransitionRule(
            targetState = RcsState.Connected,
            guard = { true },
            sideEffect = { Log.d(TAG, "Deregistration complete") }
        ),
        
        // From Reconnecting
        TransitionKey(RcsState.Reconnecting, RcsEvent.ConnectionEstablished) to TransitionRule(
            targetState = RcsState.Registering,
            guard = { true },
            sideEffect = { Log.d(TAG, "Reconnected, re-registering") }
        ),
        TransitionKey(RcsState.Reconnecting, RcsEvent.ReconnectFailed) to TransitionRule(
            targetState = RcsState.Disconnected,
            guard = { true },
            sideEffect = { Log.e(TAG, "Reconnection failed") }
        ),
        
        // From Disconnecting
        TransitionKey(RcsState.Disconnecting, RcsEvent.DisconnectionComplete) to TransitionRule(
            targetState = RcsState.Disconnected,
            guard = { true },
            sideEffect = { Log.d(TAG, "Disconnection complete") }
        ),
        
        // From Error
        TransitionKey(RcsState.Error, RcsEvent.Reset) to TransitionRule(
            targetState = RcsState.Uninitialized,
            guard = { true },
            sideEffect = { Log.d(TAG, "Resetting from error state") }
        ),
        TransitionKey(RcsState.Error, RcsEvent.Retry) to TransitionRule(
            targetState = RcsState.Initializing,
            guard = { true },
            sideEffect = { Log.d(TAG, "Retrying from error state") }
        ),
        
        // From ShuttingDown
        TransitionKey(RcsState.ShuttingDown, RcsEvent.ShutdownComplete) to TransitionRule(
            targetState = RcsState.Terminated,
            guard = { true },
            sideEffect = { Log.i(TAG, "Shutdown complete") }
        )
    )

    suspend fun processEvent(event: RcsEvent): TransitionResult {
        return transitionMutex.withLock {
            val currentStateValue = mutableCurrentState.value
            val transitionKey = TransitionKey(currentStateValue, event)
            val rule = transitionTable[transitionKey]
            
            if (rule == null) {
                Log.w(TAG, "No transition defined for state=$currentStateValue, event=$event")
                return@withLock TransitionResult.InvalidTransition(currentStateValue, event)
            }
            
            if (!rule.guard()) {
                Log.w(TAG, "Guard failed for transition: $currentStateValue -> ${rule.targetState}")
                return@withLock TransitionResult.GuardFailed(currentStateValue, event)
            }
            
            val previousState = currentStateValue
            mutableCurrentState.value = rule.targetState
            
            val record = StateTransitionRecord(
                fromState = previousState,
                toState = rule.targetState,
                event = event,
                timestamp = System.currentTimeMillis()
            )
            stateHistory.add(record)
            
            if (stateHistory.size > 100) {
                stateHistory.removeAt(0)
            }
            
            try {
                rule.sideEffect()
            } catch (e: Exception) {
                Log.e(TAG, "Side effect failed", e)
            }
            
            notifyListeners(previousState, rule.targetState, event)
            
            Log.d(TAG, "State transition: $previousState -> ${rule.targetState} (event: $event)")
            
            TransitionResult.Success(previousState, rule.targetState, event)
        }
    }

    fun getCurrentState(): RcsState = mutableCurrentState.value

    fun isInState(vararg states: RcsState): Boolean {
        return mutableCurrentState.value in states
    }

    fun canProcessEvent(event: RcsEvent): Boolean {
        val transitionKey = TransitionKey(mutableCurrentState.value, event)
        return transitionTable.containsKey(transitionKey)
    }

    fun getStateHistory(): List<StateTransitionRecord> {
        return stateHistory.toList()
    }

    fun addListener(listener: StateChangeListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: StateChangeListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners(fromState: RcsState, toState: RcsState, event: RcsEvent) {
        coroutineScope.launch {
            listeners.forEach { listener ->
                try {
                    listener.onStateChanged(fromState, toState, event)
                } catch (e: Exception) {
                    Log.e(TAG, "Listener error", e)
                }
            }
        }
    }
}

sealed class RcsState {
    object Uninitialized : RcsState() { override fun toString() = "Uninitialized" }
    object Initializing : RcsState() { override fun toString() = "Initializing" }
    object Disconnected : RcsState() { override fun toString() = "Disconnected" }
    object Connecting : RcsState() { override fun toString() = "Connecting" }
    object Connected : RcsState() { override fun toString() = "Connected" }
    object Registering : RcsState() { override fun toString() = "Registering" }
    object Authenticating : RcsState() { override fun toString() = "Authenticating" }
    object Registered : RcsState() { override fun toString() = "Registered" }
    object Deregistering : RcsState() { override fun toString() = "Deregistering" }
    object Reconnecting : RcsState() { override fun toString() = "Reconnecting" }
    object Disconnecting : RcsState() { override fun toString() = "Disconnecting" }
    object ShuttingDown : RcsState() { override fun toString() = "ShuttingDown" }
    object Terminated : RcsState() { override fun toString() = "Terminated" }
    object Error : RcsState() { override fun toString() = "Error" }
}

sealed class RcsEvent {
    object Initialize : RcsEvent() { override fun toString() = "Initialize" }
    object InitializationComplete : RcsEvent() { override fun toString() = "InitializationComplete" }
    object InitializationFailed : RcsEvent() { override fun toString() = "InitializationFailed" }
    object Connect : RcsEvent() { override fun toString() = "Connect" }
    object ConnectionEstablished : RcsEvent() { override fun toString() = "ConnectionEstablished" }
    object ConnectionFailed : RcsEvent() { override fun toString() = "ConnectionFailed" }
    object ConnectionLost : RcsEvent() { override fun toString() = "ConnectionLost" }
    object Disconnect : RcsEvent() { override fun toString() = "Disconnect" }
    object DisconnectionComplete : RcsEvent() { override fun toString() = "DisconnectionComplete" }
    object StartRegistration : RcsEvent() { override fun toString() = "StartRegistration" }
    object RegistrationComplete : RcsEvent() { override fun toString() = "RegistrationComplete" }
    object RegistrationFailed : RcsEvent() { override fun toString() = "RegistrationFailed" }
    object RegistrationExpired : RcsEvent() { override fun toString() = "RegistrationExpired" }
    object AuthenticationRequired : RcsEvent() { override fun toString() = "AuthenticationRequired" }
    object AuthenticationSuccess : RcsEvent() { override fun toString() = "AuthenticationSuccess" }
    object AuthenticationFailed : RcsEvent() { override fun toString() = "AuthenticationFailed" }
    object Deregister : RcsEvent() { override fun toString() = "Deregister" }
    object DeregistrationComplete : RcsEvent() { override fun toString() = "DeregistrationComplete" }
    object ReconnectFailed : RcsEvent() { override fun toString() = "ReconnectFailed" }
    object Timeout : RcsEvent() { override fun toString() = "Timeout" }
    object Shutdown : RcsEvent() { override fun toString() = "Shutdown" }
    object ShutdownComplete : RcsEvent() { override fun toString() = "ShutdownComplete" }
    object Reset : RcsEvent() { override fun toString() = "Reset" }
    object Retry : RcsEvent() { override fun toString() = "Retry" }
}

data class TransitionKey(
    val fromState: RcsState,
    val event: RcsEvent
)

data class TransitionRule(
    val targetState: RcsState,
    val guard: () -> Boolean = { true },
    val sideEffect: () -> Unit = {}
)

data class StateTransitionRecord(
    val fromState: RcsState,
    val toState: RcsState,
    val event: RcsEvent,
    val timestamp: Long
)

sealed class TransitionResult {
    data class Success(
        val fromState: RcsState,
        val toState: RcsState,
        val event: RcsEvent
    ) : TransitionResult()
    
    data class InvalidTransition(
        val currentState: RcsState,
        val event: RcsEvent
    ) : TransitionResult()
    
    data class GuardFailed(
        val currentState: RcsState,
        val event: RcsEvent
    ) : TransitionResult()
}

interface StateChangeListener {
    fun onStateChanged(fromState: RcsState, toState: RcsState, event: RcsEvent)
}
