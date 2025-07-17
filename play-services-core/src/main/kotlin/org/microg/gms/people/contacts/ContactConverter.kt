/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.people.contacts

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import android.text.TextUtils
import androidx.collection.ArraySet
import androidx.core.database.getBlobOrNull
import androidx.core.database.getFloatOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.google.android.gms.common.images.ImageManager
import org.microg.gms.people.Address
import org.microg.gms.people.Birthday
import org.microg.gms.people.Email
import org.microg.gms.people.Event
import org.microg.gms.people.FieldMetadata
import org.microg.gms.people.ImClient
import org.microg.gms.people.Membership
import org.microg.gms.people.Name
import org.microg.gms.people.NickName
import org.microg.gms.people.Note
import org.microg.gms.people.Organization
import org.microg.gms.people.Person
import org.microg.gms.people.Phone
import org.microg.gms.people.Photo
import org.microg.gms.people.Relation
import org.microg.gms.people.SipAddress
import org.microg.gms.people.UserDefined
import org.microg.gms.people.WebSite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val USER_DEFINED_FIELD: String = "vnd.com.google.cursor.item/contact_user_defined_field"

object ContactConverter {

    fun toContentValues(context: Context, person: Person, sourceId: String): List<ContentValues> {
        val result = arrayListOf<ContentValues>()
        person.email.takeIf { it.isNotEmpty() }?.filter { it.metadata?.sourceId == sourceId }?.onEach {
            addPropertyIdentity(it.metadata)?.run { result.add(this) }
        }?.map {
            ContentValues().apply {
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                put(ContactsContract.Data.IS_PRIMARY, if (it.metadata?.primary == true) 1 else 0)
                put(ContactsContract.CommonDataKinds.Email.ADDRESS, it.address)
                put(ContactsContract.CommonDataKinds.Email.TYPE, it.type)
                put(ContactsContract.CommonDataKinds.Email.LABEL, it.label)
                put(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME, it.displayName)
            }
        }?.forEach { result.add(it) }
        person.event.takeIf { it.isNotEmpty() }?.filter { it.metadata?.sourceId == sourceId }?.map {
            ContentValues().apply {
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                put(ContactsContract.Data.IS_PRIMARY, if (it.metadata?.primary == true) 1 else 0)
                put(ContactsContract.CommonDataKinds.Event.START_DATE, convertTimestampToDate(it.timestamp))
                put(ContactsContract.CommonDataKinds.Event.TYPE, it.type)
                put(ContactsContract.CommonDataKinds.Event.LABEL, it.label)
            }
        }?.forEach { result.add(it) }
        person.membership.takeIf { it.isNotEmpty() }?.filter { it.metadata?.sourceId == sourceId && it.type == null }?.map {
            ContentValues().apply {
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
                put(ContactsContract.Data.IS_PRIMARY, if (it.metadata?.primary == true) 1 else 0)
                put(ContactsContract.CommonDataKinds.GroupMembership.GROUP_SOURCE_ID, it.groupSourceId)
            }
        }?.forEach { result.add(it) }
        person.imClient.takeIf { it.isNotEmpty() }?.filter { it.metadata?.sourceId == sourceId }?.map {
            ContentValues().apply {
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE)
                put(ContactsContract.Data.IS_PRIMARY, if (it.metadata?.primary == true) 1 else 0)
                put(ContactsContract.CommonDataKinds.Im.DATA, it.username)
                put(ContactsContract.CommonDataKinds.Im.PROTOCOL, it.protocol)
                put(ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL, it.protocol)
                put(ContactsContract.CommonDataKinds.Im.TYPE, it.type)
                put(ContactsContract.CommonDataKinds.Im.LABEL, it.label)
            }
        }?.forEach { result.add(it) }
        person.nickName.takeIf { it.isNotEmpty() }?.filter { it.metadata?.sourceId == sourceId }?.map {
            ContentValues().apply {
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
                put(ContactsContract.Data.IS_PRIMARY, if (it.metadata?.primary == true) 1 else 0)
                put(ContactsContract.CommonDataKinds.Nickname.NAME, it.name)
                put(ContactsContract.CommonDataKinds.Nickname.TYPE, it.type)
                put(ContactsContract.CommonDataKinds.Nickname.LABEL, it.label)
            }
        }?.forEach { result.add(it) }
        person.note.takeIf { it.isNotEmpty() }?.filter { it.metadata?.sourceId == sourceId }?.map {
            ContentValues().apply {
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                put(ContactsContract.Data.IS_PRIMARY, if (it.metadata?.primary == true) 1 else 0)
                put(ContactsContract.CommonDataKinds.Note.NOTE, it.content)
            }
        }?.forEach { result.add(it) }
        person.organization.takeIf { it.isNotEmpty() }?.filter { it.metadata?.sourceId == sourceId }?.map {
            ContentValues().apply {
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                put(ContactsContract.Data.IS_PRIMARY, if (it.metadata?.primary == true) 1 else 0)
                put(ContactsContract.CommonDataKinds.Organization.COMPANY, it.company)
                put(ContactsContract.CommonDataKinds.Organization.TITLE, it.title)
                put(ContactsContract.CommonDataKinds.Organization.DEPARTMENT, it.department)
                put(ContactsContract.CommonDataKinds.Organization.JOB_DESCRIPTION, it.jobDescription)
                put(ContactsContract.CommonDataKinds.Organization.SYMBOL, it.symbol)
                put(ContactsContract.CommonDataKinds.Organization.PHONETIC_NAME, it.phoneticName)
                put(ContactsContract.CommonDataKinds.Organization.OFFICE_LOCATION, it.location)
                put(ContactsContract.CommonDataKinds.Organization.PHONETIC_NAME_STYLE, it.phoneticNameStyle)
                put(ContactsContract.CommonDataKinds.Organization.TYPE, it.type)
                put(ContactsContract.CommonDataKinds.Organization.LABEL, it.label)
            }
        }?.forEach { result.add(it) }
        person.phone.takeIf { it.isNotEmpty() }?.filter { it.metadata?.sourceId == sourceId }?.onEach {
            addPropertyIdentity(it.metadata)?.run { result.add(this) }
        }?.map {
            ContentValues().apply {
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                put(ContactsContract.Data.IS_PRIMARY, if (it.metadata?.primary == true) 1 else 0)
                put(ContactsContract.CommonDataKinds.Phone.NUMBER, it.number)
                put(ContactsContract.CommonDataKinds.Phone.TYPE, it.type)
                put(ContactsContract.CommonDataKinds.Phone.LABEL, it.label)
            }
        }?.forEach { result.add(it) }
        person.photo.takeIf { it.isNotEmpty() }?.filter { it.metadata?.sourceId == sourceId }?.map {
            ContentValues().apply {
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                put(ContactsContract.Data.IS_PRIMARY, if (it.metadata?.primary == true) 1 else 0)
                put(ContactsContract.CommonDataKinds.Photo.PHOTO, covertPhotoUrl(context, it.url))
                put(ContactsContract.CommonDataKinds.Photo.SYNC1, it.url)
                put(ContactsContract.CommonDataKinds.Photo.SYNC2, it.image)
            }
        }?.forEach { result.add(it) }
        person.relation.takeIf { it.isNotEmpty() }?.filter { it.metadata?.sourceId == sourceId }?.map {
            ContentValues().apply {
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE)
                put(ContactsContract.Data.IS_PRIMARY, if (it.metadata?.primary == true) 1 else 0)
                put(ContactsContract.CommonDataKinds.Relation.NAME, it.name)
                put(ContactsContract.CommonDataKinds.Relation.TYPE, it.type)
                put(ContactsContract.CommonDataKinds.Relation.LABEL, it.label)
            }
        }?.forEach { result.add(it) }
        person.sipAddress.takeIf { it.isNotEmpty() }?.filter { it.metadata?.sourceId == sourceId }?.map {
            ContentValues().apply {
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE)
                put(ContactsContract.Data.IS_PRIMARY, if (it.metadata?.primary == true) 1 else 0)
                put(ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS, it.address)
                put(ContactsContract.CommonDataKinds.SipAddress.TYPE, it.type)
                put(ContactsContract.CommonDataKinds.SipAddress.LABEL, it.label)
            }
        }?.forEach { result.add(it) }
        person.name.takeIf { it.isNotEmpty() }?.filter { it.metadata?.sourceId == sourceId }?.map {
            ContentValues().apply {
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                put(ContactsContract.Data.IS_PRIMARY, if (it.metadata?.primary == true) 1 else 0)
                put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, it.displayName)
                put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, it.givenName)
                put(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, it.familyName)
                put(ContactsContract.CommonDataKinds.StructuredName.PREFIX, it.prefix)
                put(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, it.middleName)
                put(ContactsContract.CommonDataKinds.StructuredName.SUFFIX, it.suffix)
                put(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME, it.phoneticGivenName)
                put(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_MIDDLE_NAME, it.phoneticMiddleName)
                put(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_FAMILY_NAME, it.phoneticFamilyName)
                put(ContactsContract.CommonDataKinds.StructuredName.FULL_NAME_STYLE, it.fullNameStyle)
                put(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_NAME_STYLE, it.phoneticNameStyle)
            }
        }?.forEach { result.add(it) }
        person.address.takeIf { it.isNotEmpty() }?.filter { it.metadata?.sourceId == sourceId }?.map {
            ContentValues().apply {
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                put(ContactsContract.Data.IS_PRIMARY, if (it.metadata?.primary == true) 1 else 0)
                put(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, it.address)
                put(ContactsContract.CommonDataKinds.StructuredPostal.STREET, it.streetAddress)
                put(ContactsContract.CommonDataKinds.StructuredPostal.POBOX, it.poBox)
                put(ContactsContract.CommonDataKinds.StructuredPostal.NEIGHBORHOOD, it.neighborhood)
                put(ContactsContract.CommonDataKinds.StructuredPostal.CITY, it.city)
                put(ContactsContract.CommonDataKinds.StructuredPostal.REGION, it.region)
                put(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE, it.postalCode)
                put(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY, it.country)
                put(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, it.type)
                put(ContactsContract.CommonDataKinds.StructuredPostal.LABEL, it.label)
            }
        }?.forEach { result.add(it) }
        person.website.takeIf { it.isNotEmpty() }?.filter { it.metadata?.sourceId == sourceId }?.onEach {
            addPropertyIdentity(it.metadata)?.run { result.add(this) }
        }?.map {
            ContentValues().apply {
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                put(ContactsContract.Data.IS_PRIMARY, if (it.metadata?.primary == true) 1 else 0)
                put(ContactsContract.CommonDataKinds.Website.URL, it.url)
                put(ContactsContract.CommonDataKinds.Website.TYPE, it.type)
                put(ContactsContract.CommonDataKinds.Website.LABEL, it.label)
            }
        }?.forEach { result.add(it) }
        person.userDefined.takeIf { it.isNotEmpty() }?.filter { it.metadata?.sourceId == sourceId }?.map {
            ContentValues().apply {
                put(ContactsContract.Data.MIMETYPE, USER_DEFINED_FIELD)
                put(ContactsContract.Data.IS_PRIMARY, if (it.metadata?.primary == true) 1 else 0)
                put(ContactsContract.Data.DATA1, it.label)
                put(ContactsContract.Data.DATA2, it.content)
            }
        }?.forEach { result.add(it) }
        person.birthday.takeIf { it.isNotEmpty() }?.filter { it.metadata?.sourceId == sourceId }?.map {
            ContentValues().apply {
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                put(ContactsContract.Data.IS_PRIMARY, if (it.metadata?.primary == true) 1 else 0)
                put(ContactsContract.CommonDataKinds.Event.START_DATE, convertTimestampToDate(it.timestamp))
                put(ContactsContract.CommonDataKinds.Event.TYPE, ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY)
                put(ContactsContract.CommonDataKinds.Event.LABEL, ContactsContract.CommonDataKinds.Event.getTypeResource(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY))
            }
        }?.forEach { result.add(it) }
        return result
    }

    fun toPersonProperty(contactData: ContactData, fieldMetadata: FieldMetadata, builder: Person.Builder) {
        when (contactData.mimeType) {
            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                contactData.data?.let {
                    Email.Builder().apply {
                        metadata(fieldMetadata)
                        address(it.getAsString(ContactsContract.CommonDataKinds.Email.ADDRESS))
                        displayName(it.getAsString(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME))
                        type(it.getAsString(ContactsContract.CommonDataKinds.Email.TYPE))
                        label(it.getAsString(ContactsContract.CommonDataKinds.Email.LABEL))
                    }.build()
                }?.also {
                    builder.email(ArraySet(builder.email).apply { add(it) }.toList())
                }
            }

            ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE -> {
                val type = contactData.data?.getAsInteger(ContactsContract.CommonDataKinds.Event.TYPE)
                val date = contactData.data?.getAsString(ContactsContract.CommonDataKinds.Event.START_DATE)
                val label = contactData.data?.getAsString(ContactsContract.CommonDataKinds.Event.LABEL)
                val time = convertDateToTimestamp(date)
                if (ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY == type) {
                    builder.birthday(ArraySet(builder.birthday).apply { add(Birthday(fieldMetadata, time, label)) }.toList())
                } else {
                    builder.event(ArraySet(builder.event).apply { add(Event(fieldMetadata, time, type, label)) }.toList())
                }
            }

            ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE -> {
                contactData.data?.let {
                    val sourceId = it.getAsString(ContactsContract.CommonDataKinds.GroupMembership.GROUP_SOURCE_ID)
                    if (sourceId.isNullOrEmpty()) {
                        null
                    } else {
                        Membership.Builder().apply {
                            metadata(fieldMetadata)
                            groupSourceId(sourceId)
                        }.build()
                    }
                }?.also {
                    builder.membership(ArraySet(builder.membership).apply { add(it) }.toList())
                }
            }

            ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE -> {
                contactData.data?.let {
                    ImClient.Builder().apply {
                        metadata(fieldMetadata)
                        username(it.getAsString(ContactsContract.CommonDataKinds.Im.DATA))
                        protocol(it.getAsString(ContactsContract.CommonDataKinds.Im.PROTOCOL))
                        type(it.getAsString(ContactsContract.CommonDataKinds.Im.TYPE))
                        label(it.getAsString(ContactsContract.CommonDataKinds.Im.LABEL))
                    }.build()
                }?.also {
                    builder.imClient(ArraySet(builder.imClient).apply { add(it) }.toList())
                }
            }

            ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE -> {
                contactData.data?.let {
                    NickName.Builder().apply {
                        metadata(fieldMetadata)
                        name(it.getAsString(ContactsContract.CommonDataKinds.Nickname.NAME))
                        type(it.getAsString(ContactsContract.CommonDataKinds.Nickname.TYPE))
                        label(it.getAsString(ContactsContract.CommonDataKinds.Nickname.LABEL))
                    }.build()
                }?.also {
                    builder.nickName(ArraySet(builder.nickName).apply { add(it) }.toList())
                }
            }

            ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE -> {
                contactData.data?.let {
                    Note.Builder().apply {
                        metadata(fieldMetadata)
                        content(it.getAsString(ContactsContract.CommonDataKinds.Note.NOTE))
                    }.build()
                }?.also {
                    builder.note(ArraySet(builder.note).apply { add(it) }.toList())
                }
            }

            ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> {
                contactData.data?.let {
                    Organization.Builder().apply {
                        metadata(fieldMetadata)
                        company(it.getAsString(ContactsContract.CommonDataKinds.Organization.COMPANY))
                        title(it.getAsString(ContactsContract.CommonDataKinds.Organization.TITLE))
                        department(it.getAsString(ContactsContract.CommonDataKinds.Organization.DEPARTMENT))
                        jobDescription(it.getAsString(ContactsContract.CommonDataKinds.Organization.JOB_DESCRIPTION))
                        symbol(it.getAsString(ContactsContract.CommonDataKinds.Organization.SYMBOL))
                        phoneticName(it.getAsString(ContactsContract.CommonDataKinds.Organization.PHONETIC_NAME))
                        location(it.getAsString(ContactsContract.CommonDataKinds.Organization.OFFICE_LOCATION))
                        phoneticNameStyle(it.getAsString(ContactsContract.CommonDataKinds.Organization.PHONETIC_NAME_STYLE))
                        type(it.getAsString(ContactsContract.CommonDataKinds.Organization.TYPE))
                        label(it.getAsString(ContactsContract.CommonDataKinds.Organization.LABEL))
                    }.build()
                }?.also {
                    builder.organization(ArraySet(builder.organization).apply { add(it) }.toList())
                }
            }

            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                contactData.data?.let {
                    Phone.Builder().apply {
                        metadata(fieldMetadata)
                        number(it.getAsString(ContactsContract.CommonDataKinds.Phone.NUMBER))
                        type(it.getAsString(ContactsContract.CommonDataKinds.Phone.TYPE))
                        label(it.getAsString(ContactsContract.CommonDataKinds.Phone.LABEL))
                    }.build()
                }?.also {
                    builder.phone(ArraySet(builder.phone).apply { add(it) }.toList())
                }
            }

            ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE -> {
                contactData.data?.let {
                    Photo.Builder().apply {
                        metadata(fieldMetadata)
                        url(it.getAsString(ContactsContract.CommonDataKinds.Photo.SYNC1))
                        image(it.getAsString(ContactsContract.CommonDataKinds.Photo.SYNC2))
                    }.build()
                }?.also {
                    builder.photo(arrayListOf(it))
                }
            }

            ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE -> {
                contactData.data?.let {
                    Relation.Builder().apply {
                        metadata(fieldMetadata)
                        name(it.getAsString(ContactsContract.CommonDataKinds.Relation.NAME))
                        type(it.getAsString(ContactsContract.CommonDataKinds.Relation.TYPE))
                        label(it.getAsString(ContactsContract.CommonDataKinds.Relation.LABEL))
                    }.build()
                }?.also {
                    builder.relation(ArraySet(builder.relation).apply { add(it) }.toList())
                }
            }

            ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE -> {
                contactData.data?.let {
                    SipAddress.Builder().apply {
                        metadata(fieldMetadata)
                        address(it.getAsString(ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS))
                        type(it.getAsString(ContactsContract.CommonDataKinds.SipAddress.TYPE))
                        label(it.getAsString(ContactsContract.CommonDataKinds.SipAddress.LABEL))
                    }.build()
                }?.also {
                    builder.sipAddress(ArraySet(builder.sipAddress).apply { add(it) }.toList())
                }
            }

            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE -> {
                contactData.data?.let {
                    Name.Builder().apply {
                        metadata(fieldMetadata)
                        displayName(it.getAsString(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME))
                        givenName(it.getAsString(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME))
                        familyName(it.getAsString(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME))
                        prefix(it.getAsString(ContactsContract.CommonDataKinds.StructuredName.PREFIX))
                        middleName(it.getAsString(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME))
                        suffix(it.getAsString(ContactsContract.CommonDataKinds.StructuredName.SUFFIX))
                        phoneticGivenName(it.getAsString(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME))
                        phoneticMiddleName(it.getAsString(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_MIDDLE_NAME))
                        phoneticFamilyName(it.getAsString(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_FAMILY_NAME))
                        fullNameStyle(it.getAsString(ContactsContract.CommonDataKinds.StructuredName.FULL_NAME_STYLE))
                        phoneticNameStyle(it.getAsString(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_NAME_STYLE))
                    }.build()
                }?.also {
                    builder.name(arrayListOf(it))
                }
            }

            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                contactData.data?.let {
                    Address.Builder().apply {
                        metadata(fieldMetadata)
                        address(it.getAsString(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS))
                        streetAddress(it.getAsString(ContactsContract.CommonDataKinds.StructuredPostal.STREET))
                        poBox(it.getAsString(ContactsContract.CommonDataKinds.StructuredPostal.POBOX))
                        neighborhood(it.getAsString(ContactsContract.CommonDataKinds.StructuredPostal.NEIGHBORHOOD))
                        city(it.getAsString(ContactsContract.CommonDataKinds.StructuredPostal.CITY))
                        region(it.getAsString(ContactsContract.CommonDataKinds.StructuredPostal.REGION))
                        postalCode(it.getAsString(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE))
                        country(it.getAsString(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY))
                        type(it.getAsString(ContactsContract.CommonDataKinds.StructuredPostal.TYPE))
                        label(it.getAsString(ContactsContract.CommonDataKinds.StructuredPostal.LABEL))
                    }.build()
                }?.also {
                    builder.address(arrayListOf(it))
                }
            }

            ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE -> {
                contactData.data?.let {
                    WebSite.Builder().apply {
                        metadata(fieldMetadata)
                        url(it.getAsString(ContactsContract.CommonDataKinds.Website.URL))
                        type(it.getAsString(ContactsContract.CommonDataKinds.Website.TYPE))
                        label(it.getAsString(ContactsContract.CommonDataKinds.Website.LABEL))
                    }.build()
                }?.also {
                    builder.website(ArraySet(builder.website).apply { add(it) }.toList())
                }
            }

            USER_DEFINED_FIELD -> {
                contactData.data?.let {
                    UserDefined.Builder().apply {
                        metadata(fieldMetadata)
                        label(it.getAsString(ContactsContract.Data.DATA1))
                        content(it.getAsString(ContactsContract.Data.DATA2))
                    }.build()
                }?.also {
                    builder.userDefined(ArraySet(builder.userDefined).apply { add(it) }.toList())
                }
            }
        }
    }

    private fun covertPhotoUrl(context: Context, url: String?): ByteArray? {
        if (url.isNullOrEmpty()) return null
        return ImageManager.create(context).covertBitmap(url)
    }

    private fun convertTimestampToDate(timestamp: Long?): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = Date(timestamp ?: return "")
        return dateFormat.format(date)
    }

    private fun convertDateToTimestamp(dateString: String?): Long {
        if (dateString == null) {
            return 0
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = dateFormat.parse(dateString)
        return date?.time ?: throw IllegalArgumentException("Invalid date format")
    }

    private fun addPropertyIdentity(fieldMetadata: FieldMetadata?): ContentValues? {
        if (fieldMetadata == null) return null
        val list = fieldMetadata.groupProfileMetadata.filter {
            it.groupProfile != null
        }.map { data ->
            "gprofile:${data.groupProfile?.path?.dropWhile { it == '0' }}"
        }
        if (list.isEmpty()) {
            return null
        }
        return ContentValues().apply {
            put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Identity.CONTENT_ITEM_TYPE)
            put(ContactsContract.CommonDataKinds.Identity.IDENTITY, TextUtils.join(",", list))
            put(ContactsContract.CommonDataKinds.Identity.NAMESPACE, "com.google")
        }
    }
}

data class ContactData(
    val rowId: Long? = null,
    val mimeType: String? = null,
    val dataId: Long? = null,
    val dataVersion: Long? = null,
    val isPrimary: Long? = null,
    val dirty: Long? = null,
    val deleted: Long? = null,
    val sourceId: String? = null,
    val syncTag: String? = null,
    val syncTime: Long? = null,
    val isPhotoType: Boolean = false,
    val data: ContentValues? = null
) {
    companion object {
        fun parseToContactData(cursor: Cursor, mimeType: String): ContactData {
            val rowId = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(ContactsContract.Data._ID))
            val dataVersion = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA_VERSION))
            val isPrimary = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(ContactsContract.Data.IS_PRIMARY))
            val dirty = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.DIRTY))
            val deleted = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.DELETED))
            val sourceId = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.SOURCE_ID))
            val syncTag = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.SYNC2))
            val syncTime = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.SYNC3))
            val dataId = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.Entity.DATA_ID))
            var photoType = false
            val data = when (mimeType) {
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE -> {
                    ContentValues().apply {
                        cursor.coverToCV(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.StructuredName.PREFIX, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.StructuredName.SUFFIX, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_MIDDLE_NAME, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_FAMILY_NAME, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.StructuredName.FULL_NAME_STYLE, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_NAME_STYLE, this)
                    }
                }

                ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE -> {
                    ContentValues().apply {
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Nickname.NAME, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Nickname.TYPE, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Nickname.LABEL, this)
                    }
                }

                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                    ContentValues().apply {
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Phone.NUMBER, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Phone.TYPE, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Phone.LABEL, this)
                    }
                }

                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                    ContentValues().apply {
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Email.ADDRESS, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Email.TYPE, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Email.LABEL, this)
                    }
                }

                ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                    ContentValues().apply {
                        cursor.coverToCV(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.StructuredPostal.STREET, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.StructuredPostal.POBOX, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.StructuredPostal.NEIGHBORHOOD, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.StructuredPostal.CITY, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.StructuredPostal.REGION, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.StructuredPostal.LABEL, this)
                    }
                }

                ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE -> {
                    ContentValues().apply {
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Im.PROTOCOL, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Im.DATA, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Im.TYPE, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Im.LABEL, this)
                    }
                }

                ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> {
                    ContentValues().apply {
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Organization.COMPANY, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Organization.TITLE, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Organization.DEPARTMENT, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Organization.JOB_DESCRIPTION, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Organization.SYMBOL, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Organization.PHONETIC_NAME, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Organization.OFFICE_LOCATION, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Organization.PHONETIC_NAME_STYLE, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Organization.TYPE, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Organization.LABEL, this)
                    }
                }

                ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE -> {
                    ContentValues().apply {
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Relation.NAME, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Relation.TYPE, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Relation.LABEL, this)
                    }
                }

                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE -> {
                    ContentValues().apply {
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Event.TYPE, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Event.START_DATE, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Event.LABEL, this)
                    }
                }

                ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE -> {
                    photoType = true
                    ContentValues().apply {
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Photo.PHOTO, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Photo.PHOTO_FILE_ID, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Photo.SYNC1, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Photo.SYNC2, this)
                    }
                }

                ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE -> {
                    ContentValues().apply {
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Note.NOTE, this)
                    }
                }

                ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE -> {
                    ContentValues().apply {
                        cursor.coverToCV(ContactsContract.CommonDataKinds.GroupMembership.GROUP_SOURCE_ID, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID, this)
                    }
                }

                ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE -> {
                    ContentValues().apply {
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Website.URL, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Website.TYPE, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Website.LABEL, this)
                    }
                }

                ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE -> {
                    ContentValues().apply {
                        cursor.coverToCV(ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.SipAddress.TYPE, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.SipAddress.LABEL, this)
                    }
                }

                ContactsContract.CommonDataKinds.Identity.CONTENT_ITEM_TYPE -> {
                    ContentValues().apply {
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Identity.IDENTITY, this)
                        cursor.coverToCV(ContactsContract.CommonDataKinds.Identity.NAMESPACE, this)
                    }
                }

                USER_DEFINED_FIELD -> {
                    ContentValues().apply {
                        cursor.coverToCV(ContactsContract.Data.DATA1, this)
                        cursor.coverToCV(ContactsContract.Data.DATA2, this)
                    }
                }

                else -> null
            }
            return ContactData(rowId, mimeType, dataId, dataVersion, isPrimary, dirty, deleted, sourceId, syncTag, syncTime, photoType, data)
        }

        private fun Cursor.coverToCV(field: String, contentValues: ContentValues) {
            val index = getColumnIndexOrThrow(field)
            when (getType(index)) {
                Cursor.FIELD_TYPE_STRING -> contentValues.put(field, getStringOrNull(index))
                Cursor.FIELD_TYPE_INTEGER -> contentValues.put(field, getLongOrNull(index))
                Cursor.FIELD_TYPE_FLOAT -> contentValues.put(field, getFloatOrNull(index))
                Cursor.FIELD_TYPE_BLOB -> contentValues.put(field, getBlobOrNull(index))
                else -> contentValues.putNull(field)
            }
        }
    }

    override fun toString(): String {
        return "ContactData(rowId=$rowId, mimeType=$mimeType, dataId=$dataId, dataVersion=$dataVersion, isPrimary=$isPrimary, dirty=$dirty, deleted=$deleted, sourceId=$sourceId, syncTag=$syncTag, syncTime=$syncTime, data=$data)"
    }
}