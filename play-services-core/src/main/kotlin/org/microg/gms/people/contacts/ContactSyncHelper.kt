/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.people.contacts

import android.provider.ContactsContract
import android.util.Log
import androidx.collection.ArraySet
import androidx.collection.arraySetOf
import org.microg.gms.people.ContactGroup
import org.microg.gms.people.ContactGroupRequest
import org.microg.gms.people.ContactGroupResponse
import org.microg.gms.people.CreateGroupContent
import org.microg.gms.people.CreateGroupRequest
import org.microg.gms.people.CreateGroupResponse
import org.microg.gms.people.DeleteGroup
import org.microg.gms.people.DeleteGroupRequest
import org.microg.gms.people.DeleteGroupResponse
import org.microg.gms.people.DeletePhotoRequest
import org.microg.gms.people.DeleteRequest
import org.microg.gms.people.DeleteResponse
import org.microg.gms.people.FieldMetadata
import org.microg.gms.people.GroupId
import org.microg.gms.people.GroupInfo
import org.microg.gms.people.GroupSource
import org.microg.gms.people.GroupType
import org.microg.gms.people.InsertRequest
import org.microg.gms.people.InsertResponse
import org.microg.gms.people.Membership
import org.microg.gms.people.Person
import org.microg.gms.people.PersonData
import org.microg.gms.people.PersonMetadata
import org.microg.gms.people.Profile
import org.microg.gms.people.ProfileMetadata
import org.microg.gms.people.SyncAdapterUtils
import org.microg.gms.people.SyncAdapterUtils.buildDeletePersonPhoto
import org.microg.gms.people.SyncAdapterUtils.buildRequestForInsert
import org.microg.gms.people.SyncAdapterUtils.buildRequestForUpdate
import org.microg.gms.people.SyncAdapterUtils.buildUpdatePersonPhoto
import org.microg.gms.people.SyncPeopleRequest
import org.microg.gms.people.SyncPeopleResponse
import org.microg.gms.people.UpdateGroupContent
import org.microg.gms.people.UpdateGroupRequest
import org.microg.gms.people.UpdatePhotoRequest
import org.microg.gms.people.UpdatePhotoResponse
import org.microg.gms.people.UpdateRequest
import org.microg.gms.people.UpdateResponse
import java.security.SecureRandom

private const val TAG = "ContactSyncHelper"

object ContactSyncHelper {

    fun syncServerGroup(lastToken: () -> String?, load: (ContactGroupRequest) -> ContactGroupResponse, sync: (ContactGroup) -> Unit, saveToken: (String?) -> Unit) {
        val response = load(SyncAdapterUtils.buildRequestForGroup(lastToken()))
        Log.d(TAG, "syncServerGroup res -> $response")
        response.contactGroup.forEach { sync(it) }
        saveToken(response.syncToken)
        Log.d(TAG, "syncServerGroup success")
    }

    fun insertGroupUpload(list: Set<ContactGroupInfo>, upload: (CreateGroupRequest) -> CreateGroupResponse, sync: (ContactGroup) -> Unit) {
        val contactGroupList = list.map {
            CreateGroupContent.Builder().apply {
                groupSource(GroupSource.Builder().apply {
                    groupInfo(GroupInfo(it.groupTitle, it.groupTitle))
                    groupType(GroupType(1))
                }.build())
            }.build()
        }
        val createGroupResponse = upload(SyncAdapterUtils.buildCreateForGroup(contactGroupList))
        for (contactGroup in createGroupResponse.content) {
            sync(contactGroup)
        }
    }

    fun updateGroupUpload(list: Set<ContactGroupInfo>, upload: (UpdateGroupRequest) -> CreateGroupResponse, sync: (ContactGroup) -> Unit) {
        val contactGroupList = list.map {
            UpdateGroupContent.Builder().apply {
                groupId(GroupId(it.groupId))
                groupSource(GroupSource.Builder().apply {
                    groupId(GroupId(it.groupId))
                    groupInfo(GroupInfo(it.groupTitle, it.groupTitle))
                    groupType(GroupType(1))
                }.build())
                syncStr(it.syncStr)
            }.build()
        }
        val createGroupResponse = upload(SyncAdapterUtils.buildUpdateForGroup(contactGroupList))
        for (contactGroup in createGroupResponse.content) {
            sync(contactGroup)
        }
    }

    fun deleteGroupUpload(list: Set<ContactGroupInfo>, upload: (DeleteGroupRequest) -> DeleteGroupResponse, sync: (DeleteGroup) -> Unit) {
        val sourceIdList = list.filter { it.groupId != null }.map { it.groupId!! }
        val deleteGroupResponse = upload(SyncAdapterUtils.buildDeleteForGroup(sourceIdList))
        for (contactGroup in deleteGroupResponse.delete) {
            sync(contactGroup)
        }
    }

    fun syncServerContact(lastToken: () -> String?, load: (SyncPeopleRequest) -> SyncPeopleResponse, sync: (Person) -> Unit, saveToken: (String?) -> Unit) {
        var count: Int
        do {
            val res = load(SyncAdapterUtils.buildRequestForSyncPeople(lastToken()))
            Log.d(TAG, "syncServerContact res -> ${res.person}")
            for (person in res.person) {
                sync(person)
            }
            count = res.person.size
            saveToken(res.syncToken ?: res.token)
        } while (count > 0)
        Log.d(TAG, "syncServerContact success")
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun insertUpload(
        insertList: Set<ContactData>,
        upload: (InsertRequest) -> InsertResponse,
        sync: (Long, Person) -> Unit,
        uploadPhoto: (UpdatePhotoRequest) -> UpdatePhotoResponse?,
        syncPhoto: (String, String?, String?, ByteArray?) -> Unit
    ) {
        val contactsMap = hashMapOf<Long, Person.Builder>()
        for (contactData in insertList) {
            if (contactData.data == null) continue
            val contactId = contactData.rowId ?: continue
            val fieldMetadata = FieldMetadata(type = 2, primary = contactData.isPrimary == 1L)
            val personBuilder = contactsMap[contactId] ?: Person.Builder()
            ContactConverter.toPersonProperty(contactData, fieldMetadata, personBuilder)
            contactsMap[contactId] = personBuilder
        }
        val sourceIdMap = hashMapOf<Long, Long>()
        val personDataList = mutableListOf<PersonData>()
        for (entry in contactsMap) {
            val sourceId = (SecureRandom().nextLong() and 0x7FFFFFFF87FFFFFFL or 0x8000000L)
            sourceIdMap[sourceId] = entry.key
            val personData = PersonData(person = entry.value.build(), sourceId = sourceId)
            personDataList.add(personData)
        }
        val insertResponse = upload(buildRequestForInsert(personDataList))
        val insertPhoto = hashMapOf<Long, ContactData>()
        for (contact in insertResponse.contact) {
            val person = contact.content?.person ?: continue
            val sourceId = person.metadata?.sourceId?.firstOrNull() ?: continue
            val contactId = sourceIdMap[sourceId] ?: continue
            sync(contactId, person)
            for (contactData in insertList) {
                if (contactData.isPhotoType && contactData.rowId == contactId) {
                    insertPhoto[sourceId] = contactData
                }
            }
        }
        insertPhoto.forEach {
            val imageId = it.value.data?.getAsString(ContactsContract.CommonDataKinds.Photo.PHOTO_FILE_ID)
            val bytes = it.value.data?.getAsByteArray(ContactsContract.CommonDataKinds.Photo.PHOTO)
            if (imageId != null && bytes != null) {
                val sourceId = it.key.toHexString().trimStart('0')
                val photoResponse = uploadPhoto(buildUpdatePersonPhoto(sourceId, bytes))
                syncPhoto(sourceId, photoResponse?.syncToken, photoResponse?.url, bytes)
            }
        }
        Log.d(TAG, "syncServerContacts success")
    }

    fun dirtyUpload(
        dirtyList: Set<ContactData>,
        defaultGroup: ContactGroupInfo?,
        upload: (UpdateRequest) -> UpdateResponse?,
        sync: (Long, Person) -> Unit,
        uploadPhoto: (UpdatePhotoRequest) -> UpdatePhotoResponse?,
        deletePhoto: (DeletePhotoRequest) -> Unit,
        syncPhoto: (String, String?, String?, ByteArray?) -> Unit
    ) {
        val contactsMap = hashMapOf<Long, Person.Builder>()
        val photoList = hashSetOf<ContactData>()
        for (contactData in dirtyList) {
            if (contactData.data == null) continue
            val contactId = contactData.rowId ?: continue
            val sourceId = contactData.sourceId ?: continue
            if (contactData.isPhotoType) {
                photoList.add(contactData)
                continue
            }
            val fieldMetadata = FieldMetadata(sourceId = sourceId, type = 2, primary = contactData.isPrimary == 1L)
            val personBuilder = contactsMap[contactId] ?: Person.Builder()
            if (personBuilder.metadata == null) {
                val longSourceId = sourceId.toLong(16)
                personBuilder.eTag("c$longSourceId")
                val profileMetadata = ProfileMetadata(arrayListOf(Profile(sourceId, contactData.syncTime, contactData.syncTag, 2)))
                val personMetadata = PersonMetadata(profileMetadata = profileMetadata, objectType = 2, sourceId = arrayListOf(longSourceId))
                personBuilder.metadata(personMetadata)
            }
            ContactConverter.toPersonProperty(contactData, fieldMetadata, personBuilder)
            if (!personBuilder.membership.any { it.groupSourceId == "6" }) {
                val membership = Membership(fieldMetadata, defaultGroup?.groupId)
                personBuilder.membership(ArraySet(personBuilder.membership).apply {
                    add(membership)
                }.toList())
            }
            contactsMap[contactId] = personBuilder
        }
        for (entry in contactsMap) {
            val person = entry.value.build()
            val res = upload(buildRequestForUpdate(person))
            val localPerson = res?.content?.person ?: person
            sync(entry.key, localPerson)
        }
        photoList.forEach {
            val imageId = it.data?.getAsString(ContactsContract.CommonDataKinds.Photo.PHOTO_FILE_ID)
            val bytes = it.data?.getAsByteArray(ContactsContract.CommonDataKinds.Photo.PHOTO)
            val lastSyncToken = it.data?.getAsString(ContactsContract.CommonDataKinds.Photo.SYNC2)
            if (imageId != null && bytes != null) {
                val photoResponse = uploadPhoto(buildUpdatePersonPhoto(it.sourceId!!, bytes))
                syncPhoto(it.sourceId, photoResponse?.syncToken, photoResponse?.url, bytes)
            } else if (bytes == null && lastSyncToken != null) {
                deletePhoto(buildDeletePersonPhoto(it.sourceId!!))
                syncPhoto(it.sourceId, null, null, null)
            }
        }
        Log.d(TAG, "dirtyUpload success")
    }

    fun deletedUpload(deleteList: Set<ContactData>, upload: (DeleteRequest) -> DeleteResponse, sync: (Long) -> Unit) {
        val deletedContactRowIds = arraySetOf<Long>()
        val deletedSourceIds = arraySetOf<String>()
        for (delValue in deleteList) {
            if (!delValue.sourceId.isNullOrEmpty()) {
                deletedSourceIds.add(delValue.sourceId)
            }
            deletedContactRowIds.add(delValue.rowId!!)
        }
        deletedContactRowIds.forEach { sync(it) }
        if (deletedSourceIds.isNotEmpty()) {
            val response = upload(SyncAdapterUtils.buildRequestForDelete(deletedSourceIds.toList()))
            Log.d(TAG, "deleted Upload success $response")
        }
        Log.d(TAG, "deletedUpload success")
    }

}