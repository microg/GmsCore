/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.people.contacts

import android.accounts.Account
import android.annotation.SuppressLint
import android.content.ContentProviderClient
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.content.SyncResult
import android.provider.ContactsContract
import android.util.Log
import androidx.collection.arraySetOf
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import org.microg.gms.people.ContactGroup
import org.microg.gms.people.Person
import org.microg.gms.people.SyncAdapterUtils

private const val TAG = "ContactProviderHelper"

class ContactProviderHelper(private val context: Context) {

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: ContactProviderHelper? = null
        fun get(context: Context): ContactProviderHelper {
            return instance ?: synchronized(this) {
                instance ?: ContactProviderHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun insertOrUpdateContacts(
        person: Person, account: Account, syncResult: SyncResult, provider: ContentProviderClient, contactId: Long? = null
    ) {
        val operations = ArrayList<ContentProviderOperation>()
        val sourceIdList = person.metadata?.sourceId ?: return
        sourceIdList.map {
            convertToOperations(account, syncResult, person, it.toHexString().trimStart('0'), provider, contactId)
        }.forEach {
            operations.addAll(it)
        }
        if (operations.isNotEmpty()) {
            provider.applyBatch(operations)
        }
    }

    private fun convertToOperations(
        account: Account, syncResult: SyncResult, person: Person, sourceId: String, provider: ContentProviderClient, contactRowId: Long? = null
    ): List<ContentProviderOperation> {
        Log.d(TAG, "convertToOperations: sourceId: $sourceId")
        val contactId = getContactIdBySourceId(account, sourceId, provider) ?: contactRowId ?: -1
        if (person.metadata?.deleted == 1 || person.metadata?.createTime == 0) {
            Log.d(TAG, "convertToOperations: membership is empty, delete contactId:$contactId")
            deleteContact(contactId, account, syncResult, provider)
            return emptyList()
        }
        val personData = ContactConverter.toContentValues(context, person, sourceId)
        if (personData.isEmpty()) {
            return emptyList()
        }
        Log.d(TAG, "convertToOperations: update contactId: $contactId or insert sourceId: $sourceId")

        val operations = arrayListOf<ContentProviderOperation>()
        syncPersonProfile(person, sourceId, account, contactId).also { operations.addAll(it) }

        if (contactId < 0) {
            val rawContactId = operations.size - 1
            insertNewContact(rawContactId, account, personData, syncResult).also { operations.addAll(it) }
            return operations
        }

        val uri = SyncAdapterUtils.ContentUri.addQueryParameters(ContactsContract.Data.CONTENT_URI, account)
        val localContacts = queryLocalContacts(account, provider, sourceId)
        val (toInsert, toDelete, toUpdate) = compareContacts(personData, localContacts)
        for (type in toDelete) {
            Log.d(TAG, "toDelete: $type")
            val selection = "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?"
            val operation = ContentProviderOperation.newDelete(uri).withSelection(selection, arrayOf(contactId.toString(), type)).build()
            operations.add(operation)
            syncResult.stats.numDeletes++
        }
        for (contentValues in toInsert) {
            Log.d(TAG, "toInsert: $contentValues")
            operations.add(ContentProviderOperation.newInsert(uri).withValues(contentValues).withValue(ContactsContract.Data.RAW_CONTACT_ID, contactId).build())
            syncResult.stats.numInserts++
        }
        for (contentValues in toUpdate) {
            Log.d(TAG, "toUpdate: $contentValues")
            val imageUrl = contentValues.getAsString(ContactsContract.CommonDataKinds.Photo.SYNC1)
            val syncToken = contentValues.getAsString(ContactsContract.CommonDataKinds.Photo.SYNC2)
            val bytes = contentValues.getAsByteArray(ContactsContract.CommonDataKinds.Photo.PHOTO)
            syncContactPhoto(sourceId, account, provider, imageUrl, syncToken, bytes)
        }
        return operations
    }

    private fun compareContacts(serviceData: List<ContentValues>, localData: List<ContactData>): Triple<Set<ContentValues>, Set<String?>, Set<ContentValues>> {
        val toInsert = mutableSetOf<ContentValues>()
        val toDelete = mutableSetOf<String?>()
        val toUpdate = mutableSetOf<ContentValues>()
        for (service in serviceData) {
            val mimeType = service.getAsString(ContactsContract.Data.MIMETYPE)
            if (localData.any { it.mimeType == mimeType }) {
                if (mimeType == ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE) {
                    toUpdate.add(service)
                    continue
                }
                toDelete.add(mimeType)
            }
            toInsert.add(service)
        }
        val serviceMap = serviceData.associateBy { it.getAsString(ContactsContract.Data.MIMETYPE) }
        for (localDatum in localData) {
            if (serviceMap.all { it.key != localDatum.mimeType }) {
                toDelete.add(localDatum.mimeType)
            }
        }
        return Triple(toInsert, toDelete, toUpdate)
    }

    private fun insertNewContact(
        rawContactId: Int, account: Account, personData: List<ContentValues>, syncResult: SyncResult
    ): List<ContentProviderOperation> {
        val operations = arrayListOf<ContentProviderOperation>()
        val dataContactUri = SyncAdapterUtils.ContentUri.addQueryParameters(ContactsContract.Data.CONTENT_URI, account)
        for (personValue in personData) {
            operations.add(ContentProviderOperation.newInsert(dataContactUri).withValues(personValue).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactId).build())
            syncResult.stats.numInserts++
        }
        return operations
    }

    private fun syncPersonProfile(
        person: Person,
        sourceId: String,
        account: Account,
        contactId: Long,
    ): List<ContentProviderOperation> {
        person.metadata?.profileMetadata?.profile?.firstOrNull {
            it.sourceId == sourceId
        }?.also {
            val sync = ContentValues().apply {
                put(ContactsContract.RawContacts.DIRTY, 0)
                put(ContactsContract.RawContacts.SOURCE_ID, sourceId)
                put(ContactsContract.RawContacts.SYNC2, it.syncTag ?: "")
                put(ContactsContract.RawContacts.SYNC3, it.syncTime ?: 0)
            }
            val uri = SyncAdapterUtils.ContentUri.addQueryParameters(ContactsContract.RawContacts.CONTENT_URI, account)
            val operations = arrayListOf<ContentProviderOperation>()
            if (contactId >= 0) {
                val selection = "${ContactsContract.Data._ID}=?"
                val selectionArgs = arrayOf(contactId.toString())
                operations.add(ContentProviderOperation.newUpdate(uri).withValues(sync).withSelection(selection, selectionArgs).build())
            } else {
                operations.add(ContentProviderOperation.newInsert(uri).withValues(sync).build())
            }
            return operations
        }
        return emptyList()
    }

    private fun getContactIdBySourceId(account: Account, sourceId: String, provider: ContentProviderClient): Long? {
        val uri = SyncAdapterUtils.ContentUri.addQueryParameters(ContactsContract.RawContacts.CONTENT_URI, account)
        val projection = arrayOf(ContactsContract.Data._ID)
        val selection = SyncAdapterUtils.Selections.getExistPersonSelection(sourceId)
        provider.query(uri, projection, selection, null, null)?.use {
            if (it.moveToFirst()) {
                return it.getLongOrNull(it.getColumnIndexOrThrow(ContactsContract.Data._ID))
            }
        }
        return null
    }

    fun syncPersonGroup(contactGroup: ContactGroup?, currentGroupList: Set<ContactGroupInfo>, account: Account, provider: ContentProviderClient, deleteGroupId: String? = null) {
        val groupId = contactGroup?.responseBody?.groupId?.id ?: contactGroup?.groupId?.id ?: deleteGroupId ?: return
        val uri = SyncAdapterUtils.ContentUri.addQueryParameters(ContactsContract.Groups.CONTENT_URI, account)
        if (contactGroup?.responseBody?.groupOperate?.operate == 1 || currentGroupList.find { it.groupId == groupId }?.deleted == true) {
            provider.delete(uri, "${ContactsContract.Groups.SOURCE_ID}=?", arrayOf(groupId))
            return
        }
        val groupTitle = contactGroup?.responseBody?.groupInfo?.name ?: contactGroup?.responseBody?.groupInfo?.tag ?: return
        val groupType = contactGroup?.responseBody?.groupType?.type
        val groupUpdateTime = contactGroup?.responseBody?.updateTime?.date
        if ("Starred in Android" == groupTitle || (groupType == 2 && groupUpdateTime == null && groupId != "6")) {
            return
        }
        val values = ContentValues().apply {
            put(ContactsContract.Groups.TITLE, groupTitle)
            put(ContactsContract.Groups.SOURCE_ID, groupId)
            put(ContactsContract.Groups.SYNC1, contactGroup?.sync)
            put(ContactsContract.Groups.DELETED, 0)
            put(ContactsContract.Groups.DIRTY, 0)
            if ("6" == groupId) {
                put(ContactsContract.Groups.GROUP_IS_READ_ONLY, 1)
                put(ContactsContract.Groups.GROUP_VISIBLE, 1)
                put(ContactsContract.Groups.AUTO_ADD, 1)
            } else {
                put(ContactsContract.Groups.GROUP_VISIBLE, 0)
            }
        }
        if (currentGroupList.any { info -> info.groupTitle == groupTitle }) {
            provider.update(uri, values, "${ContactsContract.Groups.TITLE}=?", arrayOf(groupTitle))
        } else {
            provider.insert(uri, values)
        }
    }

    fun getCurrentGroupList(account: Account, provider: ContentProviderClient): Set<ContactGroupInfo> {
        val projection = arrayOf(
            ContactsContract.Groups.TITLE,
            ContactsContract.Groups.SOURCE_ID,
            ContactsContract.Groups.SYNC1,
            ContactsContract.Groups.DIRTY,
            ContactsContract.Groups.DELETED,
            ContactsContract.Groups.AUTO_ADD,
            ContactsContract.Groups.GROUP_VISIBLE
        )
        val allGroupList = arraySetOf<ContactGroupInfo>()
        val uri = SyncAdapterUtils.ContentUri.addQueryParameters(ContactsContract.Groups.CONTENT_URI, account)
        provider.query(uri, projection, null, null, null)?.use {
            while (it.moveToNext()) {
                val title = it.getStringOrNull(it.getColumnIndexOrThrow(ContactsContract.Groups.TITLE)) ?: continue
                val sourceId = it.getStringOrNull(it.getColumnIndexOrThrow(ContactsContract.Groups.SOURCE_ID))
                val syncStr = it.getStringOrNull(it.getColumnIndexOrThrow(ContactsContract.Groups.SYNC1))
                val delete = it.getIntOrNull(it.getColumnIndexOrThrow(ContactsContract.Groups.DELETED))
                val dirty = it.getIntOrNull(it.getColumnIndexOrThrow(ContactsContract.Groups.DIRTY))
                val default = it.getIntOrNull(it.getColumnIndexOrThrow(ContactsContract.Groups.AUTO_ADD))
                val groupInfo = ContactGroupInfo(default == 1, sourceId, title, syncStr, sourceId == null, delete == 1, dirty == 1)
                allGroupList.add(groupInfo)
            }
        }
        Log.d(TAG, "getCurrentGroupList: allGroupList -> $allGroupList")
        return allGroupList
    }

    fun queryLocalContacts(account: Account, provider: ContentProviderClient, sourceId: String? = null): List<ContactData> {
        val localList = mutableListOf<ContactData>()
        val selection = SyncAdapterUtils.Selections.getExistPersonSelection(sourceId)
        val uri = SyncAdapterUtils.ContentUri.addQueryParameters(ContactsContract.RawContactsEntity.CONTENT_URI, account)
        provider.query(uri, null, selection, null, null)?.use {
            while (it.moveToNext()) {
                val mimetype = it.getStringOrNull(it.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)) ?: continue
                val contactData = ContactData.parseToContactData(it, mimetype).also { data -> Log.d(TAG, data.toString()) }
                localList.add(contactData)
            }
        }
        return localList
    }

    fun syncContactPhoto(sourceId: String, account: Account, provider: ContentProviderClient, imageUrl: String?, syncToken: String?, imageBytes: ByteArray?) {
        val rawUri = SyncAdapterUtils.ContentUri.addQueryParameters(ContactsContract.RawContacts.CONTENT_URI, account)
        provider.query(rawUri, arrayOf(ContactsContract.RawContacts._ID), "${ContactsContract.RawContacts.SOURCE_ID} = ?", arrayOf(sourceId), null)?.use {
            if (it.moveToFirst()) {
                val rawContactId = it.getLong(it.getColumnIndexOrThrow(ContactsContract.RawContacts._ID))
                val uri = SyncAdapterUtils.ContentUri.addQueryParameters(ContactsContract.Data.CONTENT_URI, account)
                provider.query(
                    uri,
                    arrayOf(ContactsContract.Data._ID, ContactsContract.CommonDataKinds.Photo.SYNC2),
                    "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                    arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE),
                    null
                )?.use { photoCursor ->
                    if (photoCursor.moveToFirst()) {
                        val dataId = photoCursor.getLongOrNull(photoCursor.getColumnIndexOrThrow(ContactsContract.Data._ID))
                        val lastSyncToken = photoCursor.getStringOrNull(photoCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Photo.SYNC2))
                        if (syncToken != lastSyncToken) {
                            val sync = ContentValues().apply {
                                put(ContactsContract.CommonDataKinds.Photo.PHOTO, imageBytes)
                                put(ContactsContract.CommonDataKinds.Photo.SYNC1, imageUrl)
                                put(ContactsContract.CommonDataKinds.Photo.SYNC2, syncToken)
                            }
                            provider.update(uri, sync, "${ContactsContract.Data._ID} = ?", arrayOf(dataId.toString()))
                        } else { }
                    } else {
                        val sync = ContentValues().apply {
                            put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                            put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                            put(ContactsContract.CommonDataKinds.Photo.PHOTO, imageBytes)
                            put(ContactsContract.CommonDataKinds.Photo.SYNC1, imageUrl)
                            put(ContactsContract.CommonDataKinds.Photo.SYNC2, syncToken)
                        }
                        provider.insert(uri, sync)
                    }
                }
            }
        }
    }

    fun deleteContact(contactId: Long, account: Account, syncResult: SyncResult, provider: ContentProviderClient) {
        if (contactId == -1L) return
        val uri = SyncAdapterUtils.ContentUri.addQueryParameters(ContactsContract.RawContacts.CONTENT_URI, account)
        val selection = "${ContactsContract.Data._ID}=?"
        val selectionArgs = arrayOf(contactId.toString())
        val delete = provider.delete(uri, selection, selectionArgs)
        Log.d(TAG, "deleteContact: deleted -> $delete")
        syncResult.stats.numDeletes++
    }

    fun saveSyncToken(account: Account, provider: ContentProviderClient, syncToken: String?) {
        runCatching {
            ContactsContract.SyncState.set(provider, account, syncToken?.toByteArray(Charsets.UTF_8))
        }
    }

    fun lastSyncToken(account: Account, provider: ContentProviderClient) = runCatching {
        ContactsContract.SyncState.get(provider, account)?.toString(Charsets.UTF_8)
    }.getOrNull()

    fun saveProfileSyncToken(account: Account, provider: ContentProviderClient, syncToken: String?) {
        runCatching {
            ContactsContract.ProfileSyncState.set(provider, account, syncToken?.toByteArray(Charsets.UTF_8))
        }
    }

    fun lastProfileSyncToken(account: Account, provider: ContentProviderClient) = runCatching {
        ContactsContract.ProfileSyncState.get(provider, account)?.toString(Charsets.UTF_8)
    }.getOrNull()
}