/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsValidator - Input validation framework
 */

package org.microg.gms.rcs.validation

import java.util.regex.Pattern

object RcsValidator {

    private val PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$")
    private val E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{6,14}$")
    private val IMEI_PATTERN = Pattern.compile("^\\d{15}$")
    private val MCCMNC_PATTERN = Pattern.compile("^\\d{5,6}$")

    fun validatePhoneNumber(phoneNumber: String): ValidationResult {
        if (phoneNumber.isBlank()) {
            return ValidationResult.Invalid("Phone number cannot be empty")
        }
        
        val normalized = phoneNumber.replace(Regex("[\\s\\-().]"), "")
        
        if (!PHONE_PATTERN.matcher(normalized).matches()) {
            return ValidationResult.Invalid("Invalid phone number format")
        }
        
        return ValidationResult.Valid(normalized)
    }

    fun validateE164PhoneNumber(phoneNumber: String): ValidationResult {
        if (phoneNumber.isBlank()) {
            return ValidationResult.Invalid("Phone number cannot be empty")
        }
        
        if (!E164_PATTERN.matcher(phoneNumber).matches()) {
            return ValidationResult.Invalid("Invalid E.164 phone number format")
        }
        
        return ValidationResult.Valid(phoneNumber)
    }

    fun validateImei(imei: String): ValidationResult {
        if (imei.isBlank()) {
            return ValidationResult.Invalid("IMEI cannot be empty")
        }
        
        if (!IMEI_PATTERN.matcher(imei).matches()) {
            return ValidationResult.Invalid("Invalid IMEI format (must be 15 digits)")
        }
        
        if (!validateLuhnChecksum(imei)) {
            return ValidationResult.Invalid("Invalid IMEI checksum")
        }
        
        return ValidationResult.Valid(imei)
    }

    fun validateMccMnc(mccMnc: String): ValidationResult {
        if (mccMnc.isBlank()) {
            return ValidationResult.Invalid("MCC/MNC cannot be empty")
        }
        
        if (!MCCMNC_PATTERN.matcher(mccMnc).matches()) {
            return ValidationResult.Invalid("Invalid MCC/MNC format (must be 5-6 digits)")
        }
        
        return ValidationResult.Valid(mccMnc)
    }

    fun validateMessageContent(content: String, maxLength: Int = 160000): ValidationResult {
        if (content.isEmpty()) {
            return ValidationResult.Invalid("Message content cannot be empty")
        }
        
        if (content.length > maxLength) {
            return ValidationResult.Invalid("Message exceeds maximum length of $maxLength characters")
        }
        
        return ValidationResult.Valid(content)
    }

    fun validateFileSize(sizeBytes: Long, maxSizeMb: Int = 100): ValidationResult {
        if (sizeBytes <= 0) {
            return ValidationResult.Invalid("File size must be positive")
        }
        
        val maxBytes = maxSizeMb.toLong() * 1024 * 1024
        if (sizeBytes > maxBytes) {
            return ValidationResult.Invalid("File exceeds maximum size of ${maxSizeMb}MB")
        }
        
        return ValidationResult.Valid(sizeBytes.toString())
    }

    fun validateMimeType(mimeType: String): ValidationResult {
        if (mimeType.isBlank()) {
            return ValidationResult.Invalid("MIME type cannot be empty")
        }
        
        val allowedTypes = setOf(
            "text/plain", "text/html", "text/vcard",
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/heic",
            "video/mp4", "video/3gpp", "video/webm",
            "audio/aac", "audio/amr", "audio/mp3", "audio/ogg", "audio/wav",
            "application/pdf", "application/vcard",
            "application/vnd.gsma.rcs-ft-http+xml",
            "application/vnd.gsma.rcspushlocation+xml",
            "message/cpim"
        )
        
        if (!allowedTypes.any { mimeType.startsWith(it.substringBefore("/")) }) {
            return ValidationResult.Invalid("Unsupported MIME type: $mimeType")
        }
        
        return ValidationResult.Valid(mimeType)
    }

    fun validateGroupName(name: String): ValidationResult {
        if (name.isBlank()) {
            return ValidationResult.Invalid("Group name cannot be empty")
        }
        
        if (name.length > 50) {
            return ValidationResult.Invalid("Group name cannot exceed 50 characters")
        }
        
        return ValidationResult.Valid(name)
    }

    fun validateGroupParticipants(participants: List<String>, maxSize: Int = 100): ValidationResult {
        if (participants.isEmpty()) {
            return ValidationResult.Invalid("Group must have at least one participant")
        }
        
        if (participants.size > maxSize) {
            return ValidationResult.Invalid("Group cannot have more than $maxSize participants")
        }
        
        val invalidNumbers = participants.filter { 
            validatePhoneNumber(it) !is ValidationResult.Valid 
        }
        
        if (invalidNumbers.isNotEmpty()) {
            return ValidationResult.Invalid("Invalid phone numbers: ${invalidNumbers.joinToString()}")
        }
        
        val duplicates = participants.groupBy { it }.filter { it.value.size > 1 }.keys
        if (duplicates.isNotEmpty()) {
            return ValidationResult.Invalid("Duplicate participants: ${duplicates.joinToString()}")
        }
        
        return ValidationResult.Valid(participants.joinToString(","))
    }

    private fun validateLuhnChecksum(number: String): Boolean {
        var sum = 0
        var alternate = false
        
        for (i in number.length - 1 downTo 0) {
            var digit = number[i].digitToInt()
            
            if (alternate) {
                digit *= 2
                if (digit > 9) {
                    digit -= 9
                }
            }
            
            sum += digit
            alternate = !alternate
        }
        
        return sum % 10 == 0
    }
}

sealed class ValidationResult {
    data class Valid(val normalizedValue: String) : ValidationResult()
    data class Invalid(val errorMessage: String) : ValidationResult()
}

inline fun ValidationResult.onValid(action: (String) -> Unit): ValidationResult {
    if (this is ValidationResult.Valid) {
        action(normalizedValue)
    }
    return this
}

inline fun ValidationResult.onInvalid(action: (String) -> Unit): ValidationResult {
    if (this is ValidationResult.Invalid) {
        action(errorMessage)
    }
    return this
}
