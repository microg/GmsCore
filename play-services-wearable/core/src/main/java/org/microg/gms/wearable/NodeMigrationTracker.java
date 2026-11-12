package org.microg.gms.wearable;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class NodeMigrationTracker {
    private static final String TAG = "NodeMigrationTracker";

    static final String TABLE = "nodeMigration";
    static final String COL_NODE_ID = "nodeId";
    static final String COL_MIGRATING_FROM = "migratingFromNodeId";
    static final String COL_COMPLETE = "complete";

    private final Map<String, String> migrationMap = new HashMap<>();

    private final NodeDatabaseHelper db;

    public NodeMigrationTracker(NodeDatabaseHelper db) {
        this.db = db;
    }

    public void updateMigrationInfo(SQLiteDatabase writable, String newNodeId, String migratingFromNodeId) {
        Log.i(TAG, "setNodeMigratingFrom(" + newNodeId + ", " + migratingFromNodeId + ")");
        ContentValues cv = new ContentValues();
        cv.put(COL_NODE_ID, newNodeId);
        cv.put(COL_MIGRATING_FROM, migratingFromNodeId);
        cv.put(COL_COMPLETE, 0);
        writable.insertWithOnConflict(TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);

        if (!migrationMap.containsKey(newNodeId)) {
            migrationMap.put(newNodeId, migratingFromNodeId);
        }
    }

    public void clearMigrationInfo(String nodeId) {
        Log.i(TAG, "clearMigrationInfo: " + nodeId);
        try {
            db.getWritableDatabase().delete(TABLE, COL_NODE_ID + "=?", new String[]{nodeId});
        } catch (SQLiteException e) {
            Log.w(TAG, "clearMigrationInfo: DB error for" + nodeId, e);
        }
        migrationMap.remove(nodeId);
    }

    public String getMigratingFromNodeId(String nodeId) {
        String cached = migrationMap.get(nodeId);
        if (cached != null) {
            return cached;
        }

        Cursor c = db.getReadableDatabase().query(TABLE,
                new String[]{COL_MIGRATING_FROM},
                COL_NODE_ID + "=?", new String[]{nodeId},
                null, null, null);
        try {
            if (c.moveToFirst()) return c.getString(0);
        } finally {
            c.close();
        }
        return null;
    }

    public static boolean isNodeMigrationComplete(SQLiteDatabase db, String nodeId) {
        Cursor c = db.query(TABLE,
                new String[]{COL_COMPLETE},
                COL_NODE_ID + "=?", new String[]{nodeId},
                null, null, null);
        try {
            return c.moveToFirst() && c.getInt(0) == 1;
        } finally {
            c.close();
        }
    }

    public void setMigrationComplete(String nodeId) {
        Log.i(TAG, "setMigrationComplete(" + nodeId + ")");
        ContentValues cv = new ContentValues();
        cv.put(COL_COMPLETE, 1);
        try {
            db.getWritableDatabase().update(TABLE, cv,
                    COL_NODE_ID + "=?", new String[]{nodeId});
        } catch (SQLiteException e) {
            Log.w(TAG, "setMigrationComplete: DB error for " + nodeId, e);
        }
        migrationMap.remove(nodeId);
    }

    public boolean isMigrating(String nodeId) {
        return migrationMap.containsKey(nodeId);
    }
}