/*
 * SPDX-FileCopyrightText: 2024-2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 *
 * IRcsService.aidl - Main RCS Service AIDL Interface
 * 
 * This interface defines the contract between Google Messages and
 * the microG RCS implementation. It handles:
 * - RCS registration and provisioning
 * - Capability exchange (IMEI, phone number verification)
 * - Message send/receive orchestration
 * - Configuration management
 */

package com.google.android.gms.rcs;

import com.google.android.gms.rcs.IRcsServiceCallback;
import com.google.android.gms.rcs.IRcsCapabilitiesCallback;
import com.google.android.gms.rcs.IRcsProvisioningCallback;
import com.google.android.gms.rcs.IRcsStateListener;
import com.google.android.gms.rcs.RcsConfiguration;

/**
 * Main RCS Service Interface
 * 
 * Provides Rich Communication Services functionality for Google Messages
 * and other RCS-enabled applications.
 */
interface IRcsService {

    /**
     * Get current RCS service version
     * @return Version code of the RCS implementation
     */
    int getVersion();

    /**
     * Check if RCS is available on this device
     * @return true if RCS can be enabled, false otherwise
     */
    boolean isRcsAvailable();

    /**
     * Check if RCS is currently enabled and connected
     * @return true if RCS is active, false otherwise
     */
    boolean isRcsEnabled();

    /**
     * Enable RCS service for the current user
     * @param callback Callback for provisioning result
     */
    void enableRcs(in IRcsProvisioningCallback callback);

    /**
     * Disable RCS service
     * @param callback Callback for result
     */
    void disableRcs(in IRcsServiceCallback callback);

    /**
     * Get RCS provisioning status
     * @param callback Callback with provisioning status
     */
    void getProvisioningStatus(in IRcsProvisioningCallback callback);

    /**
     * Start RCS provisioning process
     * @param callback Callback for provisioning progress and result
     */
    void startProvisioning(in IRcsProvisioningCallback callback);

    /**
     * Get RCS capabilities for a phone number
     * @param phoneNumber The phone number to check
     * @param callback Callback with capabilities result
     */
    void getCapabilities(String phoneNumber, in IRcsCapabilitiesCallback callback);

    /**
     * Get RCS capabilities for multiple phone numbers
     * @param phoneNumbers List of phone numbers to check
     * @param callback Callback with capabilities results
     */
    void getCapabilitiesBulk(in List<String> phoneNumbers, in IRcsCapabilitiesCallback callback);

    /**
     * Publish own RCS capabilities
     * @param capabilities Bitmask of supported capabilities
     * @param callback Callback for result
     */
    void publishCapabilities(int capabilities, in IRcsServiceCallback callback);

    /**
     * Get current RCS configuration
     * @return Current RCS configuration object
     */
    RcsConfiguration getConfiguration();

    /**
     * Update RCS configuration
     * @param config New configuration to apply
     * @param callback Callback for result
     */
    void updateConfiguration(in RcsConfiguration config, in IRcsServiceCallback callback);

    /**
     * Register for RCS state change notifications
     * @param listener Listener to receive state changes
     */
    void registerRcsStateListener(in IRcsStateListener listener);

    /**
     * Unregister from RCS state change notifications
     * @param listener Previously registered listener
     */
    void unregisterRcsStateListener(in IRcsStateListener listener);

    /**
     * Get the RCS-registered phone number
     * @return The phone number registered for RCS, or null if not registered
     */
    String getRegisteredPhoneNumber();

    /**
     * Set the preferred phone number for RCS
     * @param phoneNumber Phone number to use for RCS
     * @param callback Callback for result
     */
    void setPreferredPhoneNumber(String phoneNumber, in IRcsServiceCallback callback);

    /**
     * Force refresh of RCS registration
     * @param callback Callback for result
     */
    void refreshRegistration(in IRcsProvisioningCallback callback);

    /**
     * Get carrier-specific RCS configuration
     * @param mccMnc Mobile Country Code + Mobile Network Code
     * @return Carrier configuration or null if not available
     */
    RcsConfiguration getCarrierConfiguration(String mccMnc);

    /**
     * Check if the device passes integrity checks for RCS
     * @return true if device is trusted, false otherwise
     */
    boolean isDeviceTrusted();
}
