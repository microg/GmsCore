/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.people

import android.accounts.Account
import android.net.Uri
import android.provider.ContactsContract
import okio.ByteString.Companion.toByteString
import org.microg.gms.profile.Build

object SyncAdapterUtils {

    private val PERSON_PROPERTIES = mutableListOf(
        "person.about", "person.address", "person.birthday", "person.calendar",
        "person.client_data", "person.contact_group_membership", "person.email", "person.event", "person.external_id",
        "person.file_as", "person.gender", "person.im", "person.interest", "person.language", "person.name", "person.nickname",
        "person.occupation", "person.organization", "person.other_keyword", "person.phone", "person.relation", "person.sip_address",
        "person.user_defined", "person.website"
    )

    private val SYNC_PROPERTIES = PERSON_PROPERTIES + listOf("person.photo", "person.metadata")

    object Selections {
        private const val QUERY_SOURCE_ID = "(sourceId in ('%s') OR (sync2 in ('%s') AND sourceId IS NULL)) OR (data_set IS NULL AND sourceId IS NULL AND sync3 IS NOT NULL)"
        private const val QUERY_EMPTY_SOURCE_ID = "data_set IS NULL AND (sourceId IS NULL OR dirty != 0 OR deleted != 0)"

        fun getExistPersonSelection(sourceId: String?) = sourceId?.let { String.format(QUERY_SOURCE_ID, it, it) } ?: QUERY_EMPTY_SOURCE_ID
    }

    object ContentUri {
        fun addQueryParameters(contentUri: Uri, account: Account?): Uri {
            if (account == null) {
                return contentUri
            }
            val builder =
                contentUri.buildUpon().appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_NAME, account.name).appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type)
                    .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
            return builder.build()
        }
    }

    private fun getGmsVersion() = GmsVersion.Builder().apply {
        model(Model.Builder().apply {
            model("GMS FSA2")
            version("22.46.19 (190408-515739919)")
        }.build())
        status(VersionStatus.Builder().apply {
            isPrimary(true)
        }.build())
    }.build()

    fun buildUpdatePersonPhoto(sourceId: String, data: ByteArray) = UpdatePhotoRequest.Builder().apply {
        type(1)
        sourceId(sourceId)
        content(1)
        photoBytes(data.toByteString())
        gmsVersion(getGmsVersion())
    }.build()

    fun buildDeletePersonPhoto(sourceId: String) = DeletePhotoRequest.Builder().apply {
        sourceId(sourceId)
        gmsVersion(getGmsVersion())
    }.build()

    fun buildCreateForGroup(groups: List<CreateGroupContent>) = CreateGroupRequest.Builder().apply {
        content(groups)
        gmsVersion(getGmsVersion())
    }.build()

    fun buildUpdateForGroup(groups: List<UpdateGroupContent>) = UpdateGroupRequest.Builder().apply {
        content(groups)
        gmsVersion(getGmsVersion())
    }.build()

    fun buildDeleteForGroup(groupIds: List<String>) = DeleteGroupRequest.Builder().apply {
        groupId(groupIds)
        gmsVersion(getGmsVersion())
    }.build()

    fun buildRequestForGroup(syncToken: String?) = ContactGroupRequest.Builder().apply {
        syncToken(syncToken)
        type(arrayListOf(1))
        typeList(GroupTypeList(arrayListOf(1)))
        syncSize(100)
        gmsVersion(getGmsVersion())
    }.build()

    fun buildRequestForSyncPeople(syncToken: String?) = SyncPeopleRequest.Builder().apply {
        token(Token.Builder().apply {
            token(syncToken)
            status(1)
        }.build())
        pageSize(1000)
        gmsVersion(getGmsVersion())
        property_(Property.Builder().apply {
            propertyList(PropertyList.Builder().apply { property_(SYNC_PROPERTIES) }.build())
            propertyStatus(PropertyStatus.Builder().apply { status(0) }.build())
        }.build())
        requestMetadata(RequestMetadata.Builder().apply {
            requestType(RequestType.Builder().apply { type(2) }.build())
            type(arrayListOf(3))
        }.build())
    }.build()

    fun buildRequestForInsert(dataList: List<PersonData>) = InsertRequest.Builder().apply {
        personData(dataList)
        propertyList(PropertyList.Builder().apply {
            property_(PERSON_PROPERTIES)
        }.build())
        requestData(RequestData.Builder().apply {
            type(1)
            gdata(GroupData.Builder().apply {
                compatibility(arrayListOf(8))
            }.build())
            gmsVersion(getGmsVersion())
            property_(Property.Builder().apply {
                propertyList(PropertyList.Builder().apply {
                    property_(SYNC_PROPERTIES)
                }.build())
            }.build())
            requestMetadata(RequestMetadata.Builder().apply {
                type(arrayListOf(3))
                requestType(RequestType.Builder().apply {
                    type(2)
                }.build())
            }.build())
        }.build())
    }.build()

    fun buildRequestForUpdate(person: Person) = UpdateRequest.Builder().apply {
        eTag(person.eTag)
        person(person)
        propertyList(PropertyList.Builder().apply {
            property_(PERSON_PROPERTIES)
        }.build())
        type(2)
        isPrimary(false)
        requestData(RequestData.Builder().apply {
            type(1)
            gdata(GroupData.Builder().apply { compatibility(arrayListOf(8)) }.build())
            gmsVersion(getGmsVersion())
            property_(Property.Builder().apply {
                propertyList(PropertyList.Builder().apply {
                    property_(SYNC_PROPERTIES)
                }.build())
            }.build())
            requestMetadata(RequestMetadata.Builder().apply {
                requestType(RequestType.Builder().apply { type(2) }.build())
                type(arrayListOf(3))
            }.build())
        }.build())
    }.build()

    fun buildRequestForDelete(sourceIdList: List<String>) = DeleteRequest.Builder().apply {
        source(sourceIdList)
        gmsVersion(getGmsVersion())
        deviceMetadata(DeviceMetadata.Builder().apply {
            deviceInfo(DeviceInfo.Builder().apply {
                type(6)
                deviceModel(DeviceModel.Builder().apply {
                    model(Build.MANUFACTURER + " - " + Build.MODEL)
                }.build())
            }.build())
        }.build())
    }.build()
}