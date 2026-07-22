/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.billing

import android.accounts.Account
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.microg.vending.billing.core.PurchaseItem

object PurchaseManager {
    private val database by lazy { PurchaseDB(ContextProvider.context) }

    fun queryPurchases(account: Account, pkgName: String, type: String): List<PurchaseItem> =
        database.queryPurchases(account, pkgName, type)

    fun updatePurchase(purchaseItem: PurchaseItem) = database.updatePurchase(purchaseItem)

    fun removePurchase(purchaseToken: String) = database.removePurchase(purchaseToken)

    fun addPurchase(account: Account, pkgName: String, purchaseItem: PurchaseItem) =
        database.addPurchase(account, pkgName, purchaseItem)

    private class PurchaseDB(mContext: Context?) : SQLiteOpenHelper(
        mContext, DATABASE_NAME, null, DATABASE_VERSION
    ) {
        @Synchronized
        fun queryPurchases(account: Account, pkgName: String, type: String): List<PurchaseItem> {
            val result = mutableListOf<PurchaseItem>()
            val cursor = readableDatabase.query(
                PURCHASE_TABLE,
                null,
                "account=? and package_name=? and type=?",
                arrayOf(account.name, pkgName, type),
                null,
                null,
                null
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val item = PurchaseItem(
                        it.getString(2),
                        it.getString(3),
                        it.getString(1),
                        it.getString(4),
                        it.getInt(5),
                        it.getString(6),
                        it.getString(7),
                        it.getLong(8),
                        it.getLong(9)
                    )
                    result.add(item)
                }
            }

            return result
        }

        @Synchronized
        fun updatePurchase(purchaseItem: PurchaseItem): Int {
            val upItem = ContentValues()
            upItem.put("purchase_state", purchaseItem.purchaseState)
            upItem.put("json_data", purchaseItem.jsonData)
            upItem.put("signature", purchaseItem.signature)
            return writableDatabase.update(
                PURCHASE_TABLE,
                upItem,
                "purchase_token=?",
                arrayOf(purchaseItem.purchaseToken)
            )
        }

        @Synchronized
        fun removePurchase(purchaseToken: String) {
            writableDatabase.delete(PURCHASE_TABLE, "purchase_token=?", arrayOf(purchaseToken))
        }

        @Synchronized
        fun addPurchase(account: Account, pkgName: String, purchaseItem: PurchaseItem) {
            val cv = ContentValues()
            cv.put("account", account.name)
            cv.put("package_name", pkgName)
            cv.put("type", purchaseItem.type)
            cv.put("sku", purchaseItem.sku)
            cv.put("purchase_token", purchaseItem.purchaseToken)
            cv.put("purchase_state", purchaseItem.purchaseState)
            cv.put("json_data", purchaseItem.jsonData)
            cv.put("signature", purchaseItem.signature)
            cv.put("start_at", purchaseItem.startAt)
            cv.put("expire_at", purchaseItem.expireAt)
            writableDatabase.insertWithOnConflict(
                PURCHASE_TABLE,
                null,
                cv,
                SQLiteDatabase.CONFLICT_REPLACE
            )
        }

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(CREATE_TABLE_PURCHASES)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

        companion object {
            private const val TAG = "PurchaseDB"
            private const val DATABASE_NAME = "purchase.db"
            private const val PURCHASE_TABLE = "purchases"
            private const val DATABASE_VERSION = 1
            private const val CREATE_TABLE_PURCHASES =
                "CREATE TABLE IF NOT EXISTS $PURCHASE_TABLE ( " +
                        "account TEXT, " +
                        "package_name TEXT, " +
                        "type TEXT, " +
                        "sku TEXT, " +
                        "purchase_token TEXT, " +
                        "purchase_state INTEGER, " +
                        "json_data TEXT, " +
                        "signature TEXT, " +
                        "start_at INTEGER, " +
                        "expire_at INTEGER, " +
                        "PRIMARY KEY (purchase_token));"
        }
    }
}