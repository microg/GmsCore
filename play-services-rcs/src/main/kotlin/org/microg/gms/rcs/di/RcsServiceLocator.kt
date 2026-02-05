/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsServiceLocator - Lightweight dependency injection container
 * 
 * Provides singleton and factory patterns for all RCS services.
 * Ensures consistent initialization and proper lifecycle management.
 * Thread-safe lazy initialization with double-checked locking.
 */

package org.microg.gms.rcs.di

import android.content.Context
import org.microg.gms.rcs.CarrierConfigurationManager
import org.microg.gms.rcs.DeviceIdentifierHelper
import org.microg.gms.rcs.DeviceIntegrityChecker
import org.microg.gms.rcs.NetworkHelper
import org.microg.gms.rcs.RcsAutoConfigClient
import org.microg.gms.rcs.RcsCapabilitiesManager
import org.microg.gms.rcs.RcsProvisioningManager
import org.microg.gms.rcs.SimCardHelper
import org.microg.gms.rcs.connection.RcsConnectionManager
import org.microg.gms.rcs.core.RcsRateLimiter
import org.microg.gms.rcs.events.RcsEventBus
import org.microg.gms.rcs.group.RcsGroupChatManager
import org.microg.gms.rcs.metrics.RcsMetricsCollector
import org.microg.gms.rcs.network.SecureHttpClient
import org.microg.gms.rcs.presence.RcsPresenceManager
import org.microg.gms.rcs.receipts.RcsReadReceiptManager
import org.microg.gms.rcs.security.RcsSecurityManager
import org.microg.gms.rcs.state.RcsStateMachine
import org.microg.gms.rcs.storage.RcsMessageStore
import org.microg.gms.rcs.transfer.RcsFileTransferManager
import java.util.concurrent.ConcurrentHashMap

object RcsServiceLocator {

    @Volatile
    private var applicationContext: Context? = null
    
    private val singletonRegistry = ConcurrentHashMap<Class<*>, Any>()
    private val factoryRegistry = ConcurrentHashMap<Class<*>, () -> Any>()
    
    private val initializationLock = Any()
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        
        synchronized(initializationLock) {
            if (isInitialized) return
            
            applicationContext = context.applicationContext
            registerDefaultServices()
            isInitialized = true
        }
    }

    private fun registerDefaultServices() {
        registerSingleton(RcsEventBus::class.java) { RcsEventBus.getInstance() }
        registerSingleton(SecureHttpClient::class.java) { SecureHttpClient.getInstance() }
        registerSingleton(RcsRateLimiter::class.java) { RcsRateLimiter.getInstance() }
        registerSingleton(RcsStateMachine::class.java) { RcsStateMachine() }
        
        registerSingleton(RcsSecurityManager::class.java) {
            RcsSecurityManager.getInstance(requireContext())
        }
        registerSingleton(RcsMetricsCollector::class.java) {
            RcsMetricsCollector.getInstance(requireContext())
        }
        
        registerFactory(RcsProvisioningManager::class.java) {
            RcsProvisioningManager(requireContext())
        }
        registerFactory(RcsCapabilitiesManager::class.java) {
            RcsCapabilitiesManager(requireContext())
        }
        registerFactory(RcsConnectionManager::class.java) {
            RcsConnectionManager(requireContext())
        }
        registerFactory(RcsMessageStore::class.java) {
            RcsMessageStore(requireContext())
        }
        registerFactory(RcsFileTransferManager::class.java) {
            RcsFileTransferManager(requireContext())
        }
        registerFactory(RcsPresenceManager::class.java) {
            RcsPresenceManager(requireContext())
        }
        registerFactory(RcsGroupChatManager::class.java) {
            RcsGroupChatManager(requireContext())
        }
        registerFactory(RcsReadReceiptManager::class.java) {
            RcsReadReceiptManager(requireContext())
        }
        
        registerFactory(DeviceIdentifierHelper::class.java) {
            DeviceIdentifierHelper(requireContext())
        }
        registerSingleton(SimCardHelper::class.java) {
            SimCardHelper
        }
        registerSingleton(CarrierConfigurationManager::class.java) {
            CarrierConfigurationManager
        }
        registerSingleton(DeviceIntegrityChecker::class.java) {
            DeviceIntegrityChecker
        }
        registerSingleton(RcsAutoConfigClient::class.java) {
            RcsAutoConfigClient
        }
    }

    fun <T> registerSingleton(clazz: Class<T>, provider: () -> T) {
        @Suppress("UNCHECKED_CAST")
        factoryRegistry[clazz] = provider as () -> Any
    }

    fun <T> registerFactory(clazz: Class<T>, factory: () -> T) {
        @Suppress("UNCHECKED_CAST")
        factoryRegistry[clazz] = factory as () -> Any
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(clazz: Class<T>): T {
        checkInitialized()
        
        singletonRegistry[clazz]?.let { return it as T }
        
        val factory = factoryRegistry[clazz]
            ?: throw IllegalArgumentException("No provider registered for ${clazz.name}")
        
        val instance = factory() as T
        
        if (isSingletonType(clazz)) {
            singletonRegistry[clazz] = instance as Any
        }
        
        return instance
    }

    fun registerSipClient(client: org.microg.gms.rcs.sip.RcsSipClient) {
        singletonRegistry[org.microg.gms.rcs.sip.RcsSipClient::class.java] = client
    }
    
    fun getSipClient(): org.microg.gms.rcs.sip.RcsSipClient? {
        return singletonRegistry[org.microg.gms.rcs.sip.RcsSipClient::class.java] as? org.microg.gms.rcs.sip.RcsSipClient
    }

    inline fun <reified T> get(): T = get(T::class.java)

    private fun isSingletonType(clazz: Class<*>): Boolean {
        return clazz in listOf(
            RcsEventBus::class.java,
            SecureHttpClient::class.java,
            RcsRateLimiter::class.java,
            RcsStateMachine::class.java,
            RcsSecurityManager::class.java,
            RcsMetricsCollector::class.java
        )
    }

    private fun requireContext(): Context {
        return applicationContext
            ?: throw IllegalStateException("RcsServiceLocator not initialized. Call initialize() first.")
    }

    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("RcsServiceLocator not initialized. Call initialize() first.")
        }
    }

    fun reset() {
        synchronized(initializationLock) {
            singletonRegistry.clear()
            factoryRegistry.clear()
            applicationContext = null
            isInitialized = false
        }
    }
}

inline fun <reified T> inject(): Lazy<T> = lazy { RcsServiceLocator.get() }
