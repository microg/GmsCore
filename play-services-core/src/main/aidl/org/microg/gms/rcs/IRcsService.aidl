package org.microg.gms.rcs;

import android.os.Bundle;

/**
 * RCS Service Interface
 * 
 * This interface defines the methods that Google Messages expects
 * to be available for RCS functionality. The implementation provides
 * comprehensive RCS support to enable Google Messages to work with microG.
 */
interface IRcsService {
    /**
     * Check if RCS is supported on this device
     * @return true if RCS is supported
     */
    boolean isRcsSupported();
    
    /**
     * Check if RCS is currently enabled
     * @return true if RCS is enabled
     */
    boolean isRcsEnabled();
    
    /**
     * Enable or disable RCS
     * @param enabled true to enable RCS, false to disable
     */
    void setRcsEnabled(boolean enabled);
    
    /**
     * Get the RCS version supported by this implementation
     * @return RCS version string
     */
    String getRcsVersion();
    
    /**
     * Check if this device is compatible with RCS
     * @return true if device is compatible
     */
    boolean isRcsCompatible();
    
    /**
     * Get the RCS profile type
     * @return RCS profile type (e.g., "UP_2.0")
     */
    String getRcsProfile();
    
    /**
     * Check if RCS is provisioned
     * @return true if RCS is provisioned
     */
    boolean isRcsProvisioned();
    
    /**
     * Get the RCS registration state
     * @return registration state (0=UNREGISTERED, 1=REGISTERING, 2=REGISTERED, 3=FAILED)
     */
    int getRcsRegistrationState();
    
    /**
     * Check if RCS capability discovery is enabled
     * @return true if capability discovery is enabled
     */
    boolean isCapabilityDiscoveryEnabled();
    
    /**
     * Enable or disable RCS capability discovery
     * @param enabled true to enable, false to disable
     */
    void setCapabilityDiscoveryEnabled(boolean enabled);
    
    /**
     * Get the RCS chat mode
     * @return chat mode (0 = SMS, 1 = RCS, 2 = hybrid)
     */
    int getRcsChatMode();
    
    /**
     * Set the RCS chat mode
     * @param mode chat mode to set
     */
    void setRcsChatMode(int mode);
    
    /**
     * Check if read receipts are enabled
     * @return true if read receipts are enabled
     */
    boolean isReadReceiptsEnabled();
    
    /**
     * Enable or disable read receipts
     * @param enabled true to enable, false to disable
     */
    void setReadReceiptsEnabled(boolean enabled);
    
    /**
     * Check if typing indicators are enabled
     * @return true if typing indicators are enabled
     */
    boolean isTypingIndicatorsEnabled();
    
    /**
     * Enable or disable typing indicators
     * @param enabled true to enable, false to disable
     */
    void setTypingIndicatorsEnabled(boolean enabled);
    
    /**
     * Get the RCS user agent string
     * @return user agent string
     */
    String getRcsUserAgent();
    
    /**
     * Get the RCS device ID
     * @return device ID string
     */
    String getRcsDeviceId();
    
    /**
     * Check if RCS is available for the given phone number
     * @param phoneNumber phone number to check
     * @return true if RCS is available for this number
     */
    boolean isRcsAvailableForNumber(String phoneNumber);
    
    /**
     * Get the RCS network type
     * @return network type (0=WIFI, 1=MOBILE, 2=UNKNOWN)
     */
    int getRcsNetworkType();
    
    /**
     * Check if RCS is connected
     * @return true if RCS is connected
     */
    boolean isRcsConnected();
    
    /**
     * Get the RCS connection state
     * @return connection state (0=DISCONNECTED, 1=CONNECTING, 2=CONNECTED, 3=FAILED)
     */
    int getRcsConnectionState();
    
    /**
     * Get the RCS error code
     * @return error code (0=NO_ERROR)
     */
    int getRcsErrorCode();
    
    /**
     * Get the RCS error message
     * @return error message string
     */
    String getRcsErrorMessage();
    
    /**
     * Check if RCS is in airplane mode
     * @return true if airplane mode is enabled
     */
    boolean isRcsAirplaneMode();
    
    /**
     * Get the RCS carrier name
     * @return carrier name string
     */
    String getRcsCarrierName();
    
    /**
     * Get RCS capabilities
     * @return Bundle containing RCS capabilities
     */
    Bundle getRcsCapabilities();
    
    /**
     * Set RCS capabilities
     * @param capabilities Bundle containing RCS capabilities
     */
    void setRcsCapabilities(Bundle capabilities);
    
    /**
     * Get RCS settings
     * @return Bundle containing RCS settings
     */
    Bundle getRcsSettings();
    
    /**
     * Set RCS settings
     * @param settings Bundle containing RCS settings
     */
    void setRcsSettings(Bundle settings);
    
    /**
     * Get RCS status
     * @return Bundle containing RCS status information
     */
    Bundle getRcsStatus();
    
    /**
     * Register RCS
     * @return true if registration was successful
     */
    boolean registerRcs();
    
    /**
     * Unregister RCS
     * @return true if unregistration was successful
     */
    boolean unregisterRcs();
    
    /**
     * Provision RCS
     * @return true if provisioning was successful
     */
    boolean provisionRcs();
    
    /**
     * Deprovision RCS
     * @return true if deprovisioning was successful
     */
    boolean deprovisionRcs();
    
    /**
     * Connect RCS
     * @return true if connection was successful
     */
    boolean connectRcs();
    
    /**
     * Disconnect RCS
     * @return true if disconnection was successful
     */
    boolean disconnectRcs();
    
    /**
     * Send RCS message
     * @param phoneNumber recipient phone number
     * @param message message content
     * @return true if message was sent successfully
     */
    boolean sendRcsMessage(String phoneNumber, String message);
    
    /**
     * Send RCS typing indicator
     * @param phoneNumber recipient phone number
     * @param isTyping true if user is typing, false if stopped typing
     * @return true if indicator was sent successfully
     */
    boolean sendRcsTypingIndicator(String phoneNumber, boolean isTyping);
    
    /**
     * Send RCS read receipt
     * @param phoneNumber recipient phone number
     * @param messageId ID of the message being acknowledged
     * @return true if receipt was sent successfully
     */
    boolean sendRcsReadReceipt(String phoneNumber, String messageId);
} 