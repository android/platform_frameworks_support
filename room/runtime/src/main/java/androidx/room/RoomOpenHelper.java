/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.room;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.room.migration.Migration;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import java.util.List;

/**
 * An open helper that holds a reference to the configuration until the database is opened.
 *
 * @hide
 */
@SuppressWarnings("unused")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class RoomOpenHelper extends SupportSQLiteOpenHelper.Callback {
    @Nullable
    private DatabaseConfiguration mConfiguration;
    @NonNull
    private final Delegate mDelegate;
    @NonNull
    private final String mIdentityHash;
    /**
     * Room v1 had a bug where the hash was not consistent if fields are reordered.
     * The new has fixes it but we still need to accept the legacy hash.
     */
    @NonNull // b/64290754
    private final String mLegacyHash;

    @Nullable
    private final List<RoomOpenHelper> mChildCallbacks;

    private final String mDbId;

    public RoomOpenHelper(@NonNull DatabaseConfiguration configuration, @NonNull Delegate delegate,
            @NonNull String identityHash, @NonNull String legacyHash,
            @Nullable String dbId,
            @Nullable List<RoomOpenHelper> childCallbacks) {
        super(delegate.version);
        mConfiguration = configuration;
        mDelegate = delegate;
        mIdentityHash = identityHash;
        mLegacyHash = legacyHash;
        if (dbId == null) {
            mDbId = RoomMasterTable.DEFAULT_ID;
        } else {
            mDbId = dbId;
        }
        mChildCallbacks = childCallbacks;
    }

    @Deprecated
    public RoomOpenHelper(@NonNull DatabaseConfiguration configuration, @NonNull Delegate delegate,
            @NonNull String legacyHash) {
        this(configuration, delegate, "", legacyHash);
    }

    @Deprecated
    public RoomOpenHelper(@NonNull DatabaseConfiguration configuration, @NonNull Delegate delegate,
            @NonNull String identityHash, @NonNull String legacyHash) {
        this(configuration, delegate, identityHash, legacyHash, null, null);
    }

    @Override
    public void onConfigure(SupportSQLiteDatabase db) {
        super.onConfigure(db);
    }

    @Override
    public void onCreate(SupportSQLiteDatabase db) {
        boolean isEmptyDatabase = hasEmptySchema(db);
        mDelegate.createAllTables(db);
        if (!isEmptyDatabase) {
            // A 0 version pre-populated database goes through the create path because the
            // framework's SQLiteOpenHelper thinks the database was just created from scratch. If we
            // find the database not to be empty, then it is a pre-populated or external database,
            // we must validate it to see if its suitable for usage.
            // TODO: Use better error message indicating pre-packaged DB issue instead of migration.
            mDelegate.validateMigration(db);
        }
        updateIdentity(db);
        mDelegate.onCreate(db);
    }

    @Override
    public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
        boolean migrated = false;
        if (mConfiguration != null) {
            List<Migration> migrations = mConfiguration.migrationContainer.findMigrationPath(
                    oldVersion, newVersion);
            if (migrations != null) {
                mDelegate.onPreMigrate(db);
                for (Migration migration : migrations) {
                    migration.migrate(db);
                }
                mDelegate.validateMigration(db);
                mDelegate.onPostMigrate(db);
                updateIdentity(db);
                migrated = true;
            }
        }
        if (!migrated) {
            if (mConfiguration != null
                    && !mConfiguration.isMigrationRequired(oldVersion, newVersion)) {
                mDelegate.dropAllTables(db);
                mDelegate.createAllTables(db);
            } else {
                throw new IllegalStateException("A migration from " + oldVersion + " to "
                        + newVersion + " was required but not found. Please provide the "
                        + "necessary Migration path via "
                        + "RoomDatabase.Builder.addMigration(Migration ...) or allow for "
                        + "destructive migrations via one of the "
                        + "RoomDatabase.Builder.fallbackToDestructiveMigration* methods.");
            }
        }
    }

    @Override
    public void onDowngrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    @Override
    public void onOpen(SupportSQLiteDatabase db) {
        super.onOpen(db);
        checkIdentity(db);
        openChildDatabases(db);
        mDelegate.onOpen(db);
        // there might be too many configurations etc, just clear it.
        mConfiguration = null;
    }

    private void openChildDatabases(SupportSQLiteDatabase db) {
        if (mChildCallbacks == null) {
            return;
        }
        for(RoomOpenHelper callback : mChildCallbacks) {
            callback.openAsChild(callback, db);

        }
    }

    private void openAsChild(RoomOpenHelper childHelper, SupportSQLiteDatabase db) {
        if (RoomMasterTable.COLUMN_ID.equals(mDbId)) {
            throw new IllegalStateException("version for the master table should be sqlite"
                    + " version");
        }
        int existingVersion = readChildVersion(db);
        if (existingVersion == 0) {
            // create
            mDelegate.createAllTables(db);
        } else if (existingVersion < mDelegate.version){
            onUpgrade(db, existingVersion, mDelegate.version);
        } else {
            onDowngrade(db, existingVersion, mDelegate.version);
        }
        onOpen(db);
    }

    private int readChildVersion(SupportSQLiteDatabase db) {
        Cursor cursor = db.query(RoomMasterTable.createReadVersionQuery(mDbId));
        //noinspection TryFinallyCanBeTryWithResources
        try {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } finally {
            cursor.close();
        }
        return 0;
    }

    private void checkIdentity(SupportSQLiteDatabase db) {
        if (hasRoomMasterTable(db)) {
            String identityHash = null;
            Cursor cursor = db.query(RoomMasterTable.createReadIdentityQuery(mDbId));
            //noinspection TryFinallyCanBeTryWithResources
            try {
                if (cursor.moveToFirst()) {
                    identityHash = cursor.getString(0);
                }
            } finally {
                cursor.close();
            }
            if (!mIdentityHash.equals(identityHash) && !mLegacyHash.equals(identityHash)) {
                throw new IllegalStateException("Room cannot verify the data integrity. Looks like"
                        + " you've changed schema but forgot to update the version number. You can"
                        + " simply fix this by increasing the version number.");
            }
        } else {
            if (!RoomMasterTable.DEFAULT_ID.equals(mDbId)) {
                throw new IllegalStateException("Inconsistency detected, child database "
                        + "should be created after master database is created");
            }
            // No room_master_table, this might an an external DB or pre-populated DB, we must
            // validate to see if its suitable for usage.
            // TODO: Use better error message indicating pre-packaged DB issue instead of migration
            mDelegate.validateMigration(db);
            mDelegate.onPostMigrate(db);
            updateIdentity(db);
        }
    }

    private void updateIdentity(SupportSQLiteDatabase db) {
        createMasterTableIfNotExists(db);
        db.execSQL(RoomMasterTable.createInsertQuery(mDbId, mDelegate.version, mIdentityHash));
    }

    private static void createMasterTableIfNotExists(SupportSQLiteDatabase db) {
        db.execSQL(RoomMasterTable.CREATE_QUERY);
    }

    private static boolean hasRoomMasterTable(SupportSQLiteDatabase db) {
        TableInfo masterTableName = TableInfo.read(db, RoomMasterTable.NAME);
        TableInfo.Column idColumn = masterTableName.columns.get(RoomMasterTable.COLUMN_ID);
        if (idColumn == null) {
            return false;
        }
        // migrate id column to TEXT
        if (idColumn.affinity == ColumnInfo.INTEGER) {
            // upgrade it to string
            try {
                String tmpTableName = "_tmp_" + RoomMasterTable.TABLE_NAME;
                db.beginTransaction();
                db.execSQL(
                        "ALTER TABLE " + RoomMasterTable.TABLE_NAME + " RENAME TO " + tmpTableName);
                createMasterTableIfNotExists(db);
                db.execSQL("INSERT INTO " + RoomMasterTable.TABLE_NAME + " SELECT * FROM "
                        + tmpTableName);
                db.execSQL("DROP TABLE " + tmpTableName);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
        return true;
    }

    private static void upgradeMasterTable(SupportSQLiteDatabase db) {
        Cursor cursor = db.query("PRAGMA table_info(room_master_table)");
        if (!cursor.moveToFirst()) {
            return;
        }
        int nameIndex = cursor.getColumnIndex("name");
    }

    private static boolean hasEmptySchema(SupportSQLiteDatabase db) {
        Cursor cursor = db.query(
                "SELECT count(*) FROM sqlite_master WHERE name != 'android_metadata'");
        //noinspection TryFinallyCanBeTryWithResources
        try {
            return cursor.moveToFirst() && cursor.getInt(0) == 0;
        } finally {
            cursor.close();
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public abstract static class Delegate {
        public final int version;

        public Delegate(int version) {
            this.version = version;
        }

        protected abstract void dropAllTables(SupportSQLiteDatabase database);

        protected abstract void createAllTables(SupportSQLiteDatabase database);

        protected abstract void onOpen(SupportSQLiteDatabase database);

        protected abstract void onCreate(SupportSQLiteDatabase database);

        /**
         * Called after a migration run to validate database integrity.
         *
         * @param db The SQLite database.
         */
        protected abstract void validateMigration(SupportSQLiteDatabase db);


        /**
         * Called before migrations execute to perform preliminary work.
         * @param database The SQLite database.
         */
        protected void onPreMigrate(SupportSQLiteDatabase database) {

        }

        /**
         * Called after migrations execute to perform additional work.
         * @param database The SQLite database.
         */
        protected void onPostMigrate(SupportSQLiteDatabase database) {

        }
    }

    static class ChildOpenHelper implements SupportSQLiteOpenHelper {
        private final SupportSQLiteOpenHelper mParent;
        private final SupportSQLiteOpenHelper mChild;
        public ChildOpenHelper(SupportSQLiteOpenHelper parentOpenHelper, SupportSQLiteOpenHelper childOpenHelper) {
            mParent = parentOpenHelper;
            mChild = childOpenHelper;
        }

        @Override
        public String getDatabaseName() {
            return null;
        }

        @Override
        public void setWriteAheadLoggingEnabled(boolean enabled) {

        }

        @Override
        public SupportSQLiteDatabase getWritableDatabase() {
            return mParent.getWritableDatabase();
        }

        @Override
        public SupportSQLiteDatabase getReadableDatabase() {
            return mParent.getReadableDatabase();
        }

        @Override
        public void close() {
            // TODO or close for reals ?
            throw new UnsupportedOperationException("Cannot close a child database");
        }
    }
}
