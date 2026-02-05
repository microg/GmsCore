package org.microg.gms.rcs

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

class RcsProvisioningTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    @Mock
    private lateinit var mockConnectivityChecker: ConnectivityChecker

    private lateinit var provisioningManager: RcsProvisioningManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Mock SharedPreferences behavior
        whenever(mockSharedPreferences.edit()).thenReturn(mockEditor)
        whenever(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        whenever(mockEditor.putLong(anyString(), anyLong())).thenReturn(mockEditor)
        whenever(mockEditor.putBoolean(anyString(), any())).thenReturn(mockEditor)
        
        // We pass the mockSharedPreferences and mockConnectivityChecker directly via the constructor
        provisioningManager = RcsProvisioningManager(mockContext, mockSharedPreferences, mockConnectivityChecker)
    }

    @Test
    fun testApplyAutoConfiguration_savesValues() {
        // Arrange
        val configData = mapOf(
            "validity" to "3600",
            "token" to "test_token",
            "acs_url" to "https://test.acs.url"
        )
        val response = AutoConfigResponse(true, 0, "", configData)

        // Use reflection to verify private method logic
        val method = RcsProvisioningManager::class.java.getDeclaredMethod("applyAutoConfiguration", AutoConfigResponse::class.java)
        method.isAccessible = true
        method.invoke(provisioningManager, response)

        // Assert
        verify(mockEditor).putString("rcs_config_validity", "3600")
        verify(mockEditor).putString("rcs_config_token", "test_token")
        verify(mockEditor).putString("rcs_acs_url", "https://test.acs.url")
        verify(mockEditor).apply()
    }

    @Test
    fun testLegacyRegistration_offlineReturnsFalse() {
        // Arrange
        whenever(mockConnectivityChecker.isNetworkAvailable(any())).thenReturn(false)
        
        // Act
        val method = RcsProvisioningManager::class.java.getDeclaredMethod("performLegacyRegistration", String::class.java, com.google.android.gms.rcs.RcsConfiguration::class.java)
        method.isAccessible = true
        
        val result = method.invoke(provisioningManager, "123456", mock(com.google.android.gms.rcs.RcsConfiguration::class.java)) as Boolean
        
        // Assert
        assertFalse("Should return false when offline", result)
    }
    
    @Test
    fun testLegacyRegistration_onlineReturnsTrue() {
        // Arrange
        whenever(mockConnectivityChecker.isNetworkAvailable(any())).thenReturn(true)
        
        // Act
        val method = RcsProvisioningManager::class.java.getDeclaredMethod("performLegacyRegistration", String::class.java, com.google.android.gms.rcs.RcsConfiguration::class.java)
        method.isAccessible = true
        
        val result = method.invoke(provisioningManager, "123456", mock(com.google.android.gms.rcs.RcsConfiguration::class.java)) as Boolean
        
        // Assert
        assertTrue("Should return true when online", result)
    }
}
