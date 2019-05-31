/*
 * Copyright 2019 The Android Open Source Project
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

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An open helper that will copy & open a prepackaged database if it doesn't exists in internal
 * storage.
 */
class SQLiteCopyOpenHelper implements SupportSQLiteOpenHelper {

    @NonNull
    private final Context mContext;
    @NonNull
    private final String mCopyFromFilePath;
    private final boolean mCopyFromExternalAsset;
    @NonNull
    private final SupportSQLiteOpenHelper mDelegate;

    private SupportSQLiteDatabase mDatabase;

    SQLiteCopyOpenHelper(
            @NonNull Context context,
            @NonNull String copyFromFilePath,
            boolean copyFromExternalAsset,
            @NonNull SupportSQLiteOpenHelper supportSQLiteOpenHelper) {
        mContext = context;
        mCopyFromFilePath = copyFromFilePath;
        mCopyFromExternalAsset = copyFromExternalAsset;
        mDelegate = supportSQLiteOpenHelper;
    }

    @Override
    public String getDatabaseName() {
        return mDelegate.getDatabaseName();
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void setWriteAheadLoggingEnabled(boolean enabled) {
        mDelegate.setWriteAheadLoggingEnabled(enabled);
    }

    @Override
    public synchronized SupportSQLiteDatabase getWritableDatabase() {
        if (mDatabase == null) {
            verifyDatabaseFile();
        }
        mDatabase = mDelegate.getWritableDatabase();
        return mDatabase;
    }

    @Override
    public synchronized SupportSQLiteDatabase getReadableDatabase() {
        if (mDatabase == null) {
            verifyDatabaseFile();
        }
        mDatabase =  mDelegate.getWritableDatabase();
        return mDatabase;
    }

    @Override
    public synchronized void close() {
        mDelegate.close();
        mDatabase = null;
    }

    private void verifyDatabaseFile() {
        String databaseName = getDatabaseName();
        File databaseFile = mContext.getDatabasePath(databaseName);
        if (databaseFile.exists()) {
            return;
        }
        copyDatabaseFile(databaseFile);
    }

    private void copyDatabaseFile(File destinationFile) {
        try {
            InputStream input;
            if (mCopyFromExternalAsset) {
                input = new FileInputStream(mCopyFromFilePath);
            } else {
                input = mContext.getAssets().open(mCopyFromFilePath);
            }
            OutputStream output = new FileOutputStream(destinationFile);
            copy(input, output);
        } catch (IOException e) {
            throw new RuntimeException("Unable to copy pre-packaged database file.", e);
        }
    }

    private void copy(InputStream input, OutputStream output) throws IOException {
        try {
            int length;
            byte[] buffer = new byte[1024 * 4];
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
        } finally {
            input.close();
            output.close();
        }
    }
}
