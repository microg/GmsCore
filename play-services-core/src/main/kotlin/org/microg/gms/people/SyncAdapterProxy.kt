/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.people

import android.accounts.Account
import android.accounts.AccountManager
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.collection.arraySetOf
import com.squareup.wire.GrpcClient
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.microg.gms.people.contacts.ContactData
import org.microg.gms.people.contacts.ContactGroupInfo
import org.microg.gms.people.contacts.ContactProviderHelper
import org.microg.gms.people.contacts.ContactSyncHelper
import java.io.IOException
import java.sql.SQLException

private const val TAG = "SyncAdapterProxy"

class SyncAdapterProxy(context: Context) : AbstractThreadedSyncAdapter(context, true) {

    companion object {
        @Volatile
        private var instance: SyncAdapterProxy? = null
        fun get(context: Context): SyncAdapterProxy {
            return instance ?: synchronized(this) {
                instance ?: SyncAdapterProxy(context).also { instance = it }
            }
        }
    }

    private val mAbortLock = Any()

    @Volatile
    private var mIsCancelled = false

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onPerformSync(account: Account, extras: Bundle, authority: String, provider: ContentProviderClient, syncResult: SyncResult) {
        try {
            Log.d(TAG, "onPerformSync: localThreadName: ${Thread.currentThread().name}")
            Log.d(TAG, "Method: onPerformSync called, account:${account.name} authority:$authority extras:$extras")
            synchronized(mAbortLock) {
                reset()
                if (mIsCancelled) {
                    return
                }
                try {
                    innerPerformSync(account, provider, syncResult)
                } catch (unused: RemoteException) {
                    Log.d(TAG, "onPerformSync: ", unused)
                    syncResult.stats.numParseExceptions++
                } catch (unused: IOException) {
                    Log.d(TAG, "onPerformSync: ", unused)
                    syncResult.stats.numIoExceptions++
                }
            }
        } catch (sqlException: SQLException) {
            try {
                syncResult.stats.numParseExceptions++
            } catch (unused: Throwable) {
                Log.d(TAG, "onPerformSync: ", unused)
                throw unused
            }
        } catch (unused: Throwable) {
            Log.d(TAG, "onPerformSync unused: ", unused)
            throw unused
        }
    }

    private fun innerPerformSync(account: Account, provider: ContentProviderClient, syncResult: SyncResult) {
        val peopleClient = getPeopleServiceClient(account)
        syncGroups(account, provider, peopleClient)
        syncContacts(account, provider, peopleClient, syncResult)
    }

    private fun syncContacts(
        account: Account,
        provider: ContentProviderClient,
        peopleClient: InternalPeopleServiceClient,
        syncResult: SyncResult
    ) {
        val localContacts = ContactProviderHelper.get(context).queryLocalContacts(account, provider)
        val insertList = arraySetOf<ContactData>()
        val updateList = arraySetOf<ContactData>()
        val deleteList = arraySetOf<ContactData>()
        for (values in localContacts) {
            if (values.deleted != null && values.deleted > 0) {
                deleteList.add(values)
            } else if (values.sourceId == null) {
                insertList.add(values)
            } else if (values.dirty != null && values.dirty > 0) {
                updateList.add(values)
            }
        }
        insertList.takeIf { it.isNotEmpty() }?.run {
            ContactSyncHelper.insertUpload(this, upload = {
                peopleClient.BulkInsertContacts().executeBlocking(it)
            }, sync = { contactId, person ->
                ContactProviderHelper.get(context).insertOrUpdateContacts(person, account, syncResult, provider, contactId)
            }, uploadPhoto = {
                runCatching { peopleClient.UpdatePersonPhoto().executeBlocking(it) }.getOrNull()
            }, syncPhoto = { sourceId, syncToken, url, bytes ->
                ContactProviderHelper.get(context).syncContactPhoto(sourceId, account, provider, url, syncToken, bytes)
            })
        }
        deleteList.takeIf { it.isNotEmpty() }?.run {
            ContactSyncHelper.deletedUpload(this, upload = {
                peopleClient.DeletePeople().executeBlocking(it)
            }, sync = {
                ContactProviderHelper.get(context).deleteContact(it, account, syncResult, provider)
            })
        }
        updateList.takeIf { it.isNotEmpty() }?.run {
            val groupInfo = ContactProviderHelper.get(context).getCurrentGroupList(account, provider).find { it.isDefault }
            ContactSyncHelper.dirtyUpload(this, groupInfo, upload = {
                runCatching { peopleClient.UpdatePerson().executeBlocking(it) }.getOrNull()
            }, sync = { contactId, person ->
                ContactProviderHelper.get(context).insertOrUpdateContacts(person, account, syncResult, provider, contactId)
            }, uploadPhoto = {
                runCatching { peopleClient.UpdatePersonPhoto().executeBlocking(it) }.getOrNull()
            }, deletePhoto = {
                runCatching { peopleClient.DeletePersonPhoto().executeBlocking(it) }
            }, syncPhoto = { sourceId, syncToken, url, bytes ->
                ContactProviderHelper.get(context).syncContactPhoto(sourceId, account, provider, url, syncToken, bytes)
            })
        }

        ContactSyncHelper.syncServerContact(lastToken = {
            ContactProviderHelper.get(context).lastSyncToken(account, provider)
        }, load = {
            peopleClient.SyncPeople().executeBlocking(it)
        }, sync = {
            ContactProviderHelper.get(context).insertOrUpdateContacts(it, account, syncResult, provider)
        }, saveToken = {
            ContactProviderHelper.get(context).saveSyncToken(account, provider, it)
        })
    }

    private fun syncGroups(account: Account, provider: ContentProviderClient, peopleClient: InternalPeopleServiceClient) {
        val allGroupList = ContactProviderHelper.get(context).getCurrentGroupList(account, provider)
        val createdGroups = arraySetOf<ContactGroupInfo>()
        val updatedGroups = arraySetOf<ContactGroupInfo>()
        val deletedGroups = arraySetOf<ContactGroupInfo>()
        for (groupInfo in allGroupList) {
            if (groupInfo.deleted) {
                deletedGroups.add(groupInfo)
            } else if (groupInfo.created) {
                createdGroups.add(groupInfo)
            } else if (groupInfo.updated) {
                updatedGroups.add(groupInfo)
            }
        }
        createdGroups.takeIf { it.isNotEmpty() }?.run {
            ContactSyncHelper.insertGroupUpload(this, upload = {
                peopleClient.CreateContactGroups().executeBlocking(it)
            }, sync = {
                ContactProviderHelper.get(context).syncPersonGroup(it, allGroupList, account, provider)
            })
        }
        updatedGroups.takeIf { it.isNotEmpty() }?.run {
            ContactSyncHelper.updateGroupUpload(this, upload = {
                peopleClient.UpdateContactGroups().executeBlocking(it)
            }, sync = {
                ContactProviderHelper.get(context).syncPersonGroup(it, allGroupList, account, provider)
            })
        }
        deletedGroups.takeIf { it.isNotEmpty() }?.run {
            ContactSyncHelper.deleteGroupUpload(this, upload = {
                peopleClient.DeleteContactGroups().executeBlocking(it)
            }, sync = {
                ContactProviderHelper.get(context).syncPersonGroup(null, allGroupList, account, provider, deleteGroupId = it.groupId?.id)
            })
        }
        ContactSyncHelper.syncServerGroup(lastToken = {
            ContactProviderHelper.get(context).lastProfileSyncToken(account, provider)
        }, load = {
            peopleClient.ListContactGroups().executeBlocking(it)
        }, sync = {
            ContactProviderHelper.get(context).syncPersonGroup(it, allGroupList, account, provider)
        }, saveToken = {
            ContactProviderHelper.get(context).saveProfileSyncToken(account, provider, it)
        })
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onSyncCanceled() {
        synchronized(this.mAbortLock) {
            Log.d(TAG, "Calling the sync off...")
            this.mIsCancelled = true
        }
    }

    private fun reset() {
        synchronized(this.mAbortLock) {
            this.mIsCancelled = false
        }
    }

    private fun getPeopleServiceClient(account: Account): InternalPeopleServiceClient {
        val authTokenType = "oauth2:https://www.googleapis.com/auth/peopleapi.readwrite"
        val token = AccountManager.get(context).blockingGetAuthToken(account, authTokenType, true)
        val client = OkHttpClient().newBuilder().addInterceptor(HeaderInterceptor(token)).build()
        val grpcClient = GrpcClient.Builder().client(client).baseUrl("https://people-pa.googleapis.com").minMessageToCompress(Long.MAX_VALUE).build()
        return grpcClient.create(InternalPeopleServiceClient::class)
    }

    private class HeaderInterceptor(
        private val oauthToken: String,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val original = chain.request().newBuilder().header("Authorization", "Bearer $oauthToken")
            return chain.proceed(original.build())
        }
    }
}