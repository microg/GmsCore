/*
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.wearable;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.test.AndroidTestCase;

import com.google.android.gms.wearable.Asset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Exercises the production queries and cursor traversal on Android's SQLite implementation. */
public class NodeDatabaseHelperTest extends AndroidTestCase {
    private static final String PACKAGE = "test.app";
    private static final String SIGNATURE = "test-signature";
    private NodeDatabaseHelper helper;
    private long sequence;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        assertEquals("Tests must only use the dedicated test application's storage",
                "org.microg.gms.wearable.core.test", getContext().getPackageName());
        getContext().deleteDatabase("node.db");
        helper = new NodeDatabaseHelper(getContext());
        sequence = 10000;
    }

    @Override
    protected void tearDown() throws Exception {
        if (helper != null) helper.close();
        if (helper != null) getContext().deleteDatabase("node.db");
        super.tearDown();
    }

    public void testLiteralTrailingSlashDoesNotMatchDescendants() {
        put("node-a", "/items/");
        put("node-a", "/items/child");
        assertPaths("node-a", "/items/", DataItemQuery.FILTER_LITERAL, "/items/");
        assertPaths("node-a", "/items/", DataItemQuery.FILTER_PREFIX, "/items/", "/items/child");
        assertEquals(1, helper.deleteDataItems(PACKAGE, SIGNATURE, "node-a", "/items/").size());
        assertPaths("node-a", "/items/", DataItemQuery.FILTER_PREFIX, "/items/child");
    }

    public void testPrefixIsCaseSensitiveAndPreservesApplicationIdentity() {
        put("node-a", "/Items/child");
        put("node-a", "/items/child");
        put("node-b", "/items/other");
        put("other.app", SIGNATURE, "node-a", "/items/foreign", new String[0]);
        put(PACKAGE, "other-signature", "node-a", "/items/resigned", new String[0]);
        assertPaths("*", "/items", DataItemQuery.FILTER_PREFIX, "/items/child", "/items/other");
        assertEquals(2, helper.deleteDataItems(PACKAGE, SIGNATURE, "*", "/items", DataItemQuery.FILTER_PREFIX).size());
        assertPaths("*", "/", DataItemQuery.FILTER_PREFIX, "/Items/child");
        assertEquals(3, activeItemCount());
    }

    public void testWildcardCharactersAndQuotesInPathsAreLiteral() {
        String path = "/items/%_*'";
        put("node-a", path);
        put("node-a", path + "/child");
        put("node-a", "/items/anything");
        put("node-a", "/items/%_other'");
        assertPaths("node-a", path, DataItemQuery.FILTER_LITERAL, path);
        assertPaths("node-a", path, DataItemQuery.FILTER_PREFIX, path, path + "/child");
        assertEquals(2, helper.deleteDataItems(PACKAGE, SIGNATURE, "node-a", path, DataItemQuery.FILTER_PREFIX).size());
        assertEquals(2, activeItemCount());
    }

    public void testWildcardHostDoesNotBroadenExactLookupOrWrite() {
        put("node-a", "/same");
        put("node-b", "/same");
        assertEquals(2, queryCount("*", "/same", DataItemQuery.FILTER_LITERAL));
        try (Cursor cursor = helper.getDataItemsByHostAndPath(PACKAGE, SIGNATURE, "*", "/same")) {
            assertEquals(0, cursor.getCount());
        }
        put("*", "/same");
        put("*", "/same");
        assertEquals(3, activeItemCount());
        assertEquals(3, queryCount("*", "/same", DataItemQuery.FILTER_LITERAL));
        assertEquals(0, queryCount("node-*", "/same", DataItemQuery.FILTER_LITERAL));
        try (Cursor cursor = helper.getDataItemsByHostAndPath(PACKAGE, SIGNATURE, "*", "/same")) {
            assertEquals(1, cursor.getCount());
        }
    }

    public void testPrefixDeletionMatchesReadAndDoesNotRequireTrailingSlash() {
        put("node-a", "/prefix");
        put("node-a", "/prefix-child");
        put("node-b", "/prefix/child");
        put("node-a", "/other");
        assertEquals(3, queryCount("*", "/prefix", DataItemQuery.FILTER_PREFIX));
        List<DataItemRecord> deleted = helper.deleteDataItems(PACKAGE, SIGNATURE, "*", "/prefix", DataItemQuery.FILTER_PREFIX);
        assertEquals(3, deleted.size());
        assertEquals(0, queryCount("*", "/prefix", DataItemQuery.FILTER_PREFIX));
        assertEquals(1, activeItemCount());
        for (DataItemRecord record : deleted) {
            assertTrue(record.deleted);
            assertNull(record.dataItem.data);
        }
    }

    public void testInvalidFiltersAndMissingPathsCannotDeleteAnything() {
        put("node-a", "/keep");
        for (int filter : new int[]{-1, 2, Integer.MAX_VALUE}) {
            assertRejected("/keep", filter);
        }
        assertRejected(null, DataItemQuery.FILTER_LITERAL);
        assertRejected("", DataItemQuery.FILTER_PREFIX);
        assertEquals(1, activeItemCount());
        assertFalse(helper.getWritableDatabase().inTransaction());
    }

    public void testAssetRowsDoNotConsumeFollowingDataItemsOrShareAssets() {
        putWithSameSequence("/a", "a-first", "a-second");
        putWithSameSequence("/b", "b-only");
        putWithSameSequence("/c");
        List<DataItemRecord> records = readAllRecords();
        assertEquals(3, records.size());
        assertEquals("/a", records.get(0).dataItem.path);
        assertEquals(new HashSet<>(Arrays.asList("a-first", "a-second")), records.get(0).dataItem.getAssets().keySet());
        assertEquals("/b", records.get(1).dataItem.path);
        assertEquals(new HashSet<>(Arrays.asList("b-only")), records.get(1).dataItem.getAssets().keySet());
        assertEquals("/c", records.get(2).dataItem.path);
        assertTrue(records.get(2).dataItem.getAssets().isEmpty());
        assertEquals(37, records.get(0).v1SeqId);
    }

    public void testJoinedAssetsAreDeletedOncePerDataItem() {
        putWithSameSequence("/a", "a-first", "a-second");
        putWithSameSequence("/b", "b-only");
        putWithSameSequence("/c");
        List<DataItemRecord> deleted = helper.deleteDataItems(PACKAGE, SIGNATURE, "*", "/", DataItemQuery.FILTER_PREFIX);
        assertEquals(3, deleted.size());
        Set<String> paths = new HashSet<>();
        Set<Long> sequences = new HashSet<>();
        for (DataItemRecord record : deleted) {
            paths.add(record.dataItem.path);
            sequences.add(record.seqId);
        }
        assertEquals(new HashSet<>(Arrays.asList("/a", "/b", "/c")), paths);
        assertEquals(3, sequences.size());
        assertEquals(0, activeItemCount());
        try (Cursor cursor = helper.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM assetrefs", null)) {
            assertTrue(cursor.moveToFirst());
            assertEquals(0, cursor.getInt(0));
        }
    }

    public void testFinalAssetGroupLeavesCursorAtItsLastRow() {
        putWithSameSequence("/last", "first", "second");
        try (Cursor cursor = helper.getDataItemsByHostAndPath(PACKAGE, SIGNATURE, null, null)) {
            assertTrue(cursor.moveToNext());
            DataItemRecord record = DataItemRecord.fromCursor(cursor);
            assertEquals(2, record.dataItem.getAssets().size());
            assertTrue(cursor.isLast());
            assertFalse(cursor.moveToNext());
        }
    }

    public void testDeletionFailureRollsBackAndEndsTransaction() {
        put("node-a", "/a");
        put("node-a", "/b");
        SQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL("CREATE TRIGGER fail_delete BEFORE UPDATE OF deleted ON dataitems "
                + "WHEN OLD.path = '/b' BEGIN SELECT RAISE(ABORT, 'test rollback'); END");
        try {
            helper.deleteDataItems(PACKAGE, SIGNATURE, "*", "/", DataItemQuery.FILTER_PREFIX);
            fail("Injected database failure must propagate");
        } catch (SQLiteException expected) {
            assertEquals(2, activeItemCount());
            assertFalse(db.inTransaction());
        } finally {
            db.execSQL("DROP TRIGGER fail_delete");
        }
        assertEquals(2, helper.deleteDataItems(PACKAGE, SIGNATURE, "*", "/", DataItemQuery.FILTER_PREFIX).size());
    }

    public void testPutFailureEndsTransaction() {
        put("node-a", "/a");
        SQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL("CREATE TRIGGER fail_put BEFORE UPDATE ON dataitems "
                + "BEGIN SELECT RAISE(ABORT, 'test rollback'); END");
        try {
            put("node-a", "/a");
            fail("Injected database failure must propagate");
        } catch (SQLiteException expected) {
            assertEquals(1, activeItemCount());
            assertFalse(db.inTransaction());
        } finally {
            db.execSQL("DROP TRIGGER fail_put");
        }
        put("node-a", "/b");
        assertEquals(2, activeItemCount());
    }

    private void assertRejected(String path, int filter) {
        try {
            helper.deleteDataItems(PACKAGE, SIGNATURE, "*", path, filter);
            fail("Invalid filter or path must be rejected before deletion");
        } catch (IllegalArgumentException expected) {
            assertEquals(1, activeItemCount());
        }
        try (Cursor ignored = helper.getDataItemsForDataHolderByHostAndPath(PACKAGE, SIGNATURE, "*", path, filter)) {
            fail("Invalid filter or path must also be rejected for reads");
        } catch (IllegalArgumentException expected) {
            assertFalse(helper.getWritableDatabase().inTransaction());
        }
    }

    private List<DataItemRecord> readAllRecords() {
        List<DataItemRecord> records = new ArrayList<>();
        try (Cursor cursor = helper.getDataItemsByHostAndPath(PACKAGE, SIGNATURE, null, null)) {
            while (cursor.moveToNext()) records.add(DataItemRecord.fromCursor(cursor));
        }
        return records;
    }

    private void putWithSameSequence(String path, String... assets) {
        DataItemRecord record = newRecord(PACKAGE, SIGNATURE, "node-a", path, assets);
        record.seqId = 7;
        record.v1SeqId = 37;
        record.source = "source" + path;
        helper.putRecord(record);
    }

    private void put(String host, String path) {
        put(PACKAGE, SIGNATURE, host, path, new String[0]);
    }

    private void put(String packageName, String signature, String host, String path, String[] assets) {
        helper.putRecord(newRecord(packageName, signature, host, path, assets));
    }

    private DataItemRecord newRecord(String packageName, String signature, String host, String path, String[] assets) {
        DataItemRecord record = new DataItemRecord();
        record.packageName = packageName;
        record.signatureDigest = signature;
        record.dataItem = new DataItemInternal(host, path);
        record.dataItem.data = new byte[]{1, 2, 3};
        record.source = "test-source";
        record.seqId = sequence++;
        record.v1SeqId = record.seqId;
        record.lastModified = 1;
        for (String name : assets) {
            Asset asset = Asset.createFromRef("digest-" + name);
            helper.putAsset(asset, true);
            record.dataItem.addAsset(name, asset);
        }
        return record;
    }

    private int activeItemCount() {
        try (Cursor cursor = helper.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM dataitems WHERE deleted = 0", null)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getInt(0);
        }
    }

    private int queryCount(String host, String path, int filter) {
        try (Cursor cursor = helper.getDataItemsForDataHolderByHostAndPath(PACKAGE, SIGNATURE, host, path, filter)) {
            return cursor.getCount();
        }
    }

    private void assertPaths(String host, String path, int filter, String... expectedPaths) {
        Set<String> paths = new HashSet<>();
        try (Cursor cursor = helper.getDataItemsForDataHolderByHostAndPath(PACKAGE, SIGNATURE, host, path, filter)) {
            while (cursor.moveToNext()) paths.add(cursor.getString(cursor.getColumnIndexOrThrow("path")));
        }
        assertEquals(new HashSet<>(Arrays.asList(expectedPaths)), paths);
    }
}
