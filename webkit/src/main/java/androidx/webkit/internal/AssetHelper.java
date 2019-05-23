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

package androidx.webkit.internal;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
  * Handles opening resources and assets.
  * Forked from the chromuim project org.chromium.android_webview.AndroidProtocolHandler
  */
public class AssetHelper {
    private static final String TAG = "AssetHelper";

    @NonNull private Context mContext;

    public AssetHelper(@NonNull Context context) {
        this.mContext = context;
    }

    @Nullable
    private static InputStream handleSvgzStream(@NonNull Uri uri, @Nullable InputStream stream) {
        if (stream != null && uri.getLastPathSegment().endsWith(".svgz")) {
            try {
                stream = new GZIPInputStream(stream);
            } catch (IOException e) {
                Log.e(TAG, "Error decompressing " + uri + " - " + e.getMessage());
                return null;
            }
        }
        return stream;
    }

    private int getFieldId(@NonNull String assetType, @NonNull String assetName) {
        String packageName = mContext.getPackageName();
        int id = mContext.getResources().getIdentifier(assetName, assetType, packageName);
        return id;
    }

    private int getValueType(int fieldId) {
        TypedValue value = new TypedValue();
        mContext.getResources().getValue(fieldId, value, true);
        return value.type;
    }

    /**
     * Open an InputStream for an Android resource.
     *
     * @param uri The uri to load. The path must be of the form "asset_type/asset_name.ext".
     * @return An InputStream to the Android resource.
     */
    @Nullable
    public InputStream openResource(@NonNull Uri uri) {
        // The path must be of the form "asset_type/asset_name.ext".
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() != 2) {
            Log.e(TAG, "Incorrect resource path: " + uri);
            return null;
        }
        String assetType = pathSegments.get(0);
        String assetName = pathSegments.get(1);

        // Drop the file extension.
        assetName = assetName.split("\\.")[0];
        try {
            int fieldId = getFieldId(assetType, assetName);
            int valueType = getValueType(fieldId);
            if (valueType == TypedValue.TYPE_STRING) {
                return handleSvgzStream(uri, mContext.getResources().openRawResource(fieldId));
            } else {
                Log.e(TAG, "Asset not of type string: " + uri);
                return null;
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resource not found from URL: " + uri, e);
            return null;
        }
    }

    /**
     * Open an InputStream for an Android asset.
     *
     * @param uri The uri to load.
     * @return An InputStream to the Android asset.
     */
    @Nullable
    public InputStream openAsset(@NonNull Uri uri) {
        String path = uri.getPath();
        // Strip leading slash if present.
        if (path.length() > 1 && path.charAt(0) == '/') {
            path = path.substring(1);
        }
        try {
            AssetManager assets = mContext.getAssets();
            return handleSvgzStream(uri, assets.open(path, AssetManager.ACCESS_STREAMING));
        } catch (IOException e) {
            Log.e(TAG, "Unable to open asset URL: " + uri);
            return null;
        }
    }

    /**
     * Open an InputStream for a file in application internal storage.
     *
     * @param uri The uri to load.
     * @return An InputStream to the requested file or null if an error happens.
     */
    @Nullable
    public InputStream openFile(@NonNull Uri uri) {
        String path = uri.getPath();
        try {
            File file = openNestedPath(mContext.getFilesDir(), uri.getPathSegments(), false);
            if (file == null) return null;
            FileInputStream fis = new FileInputStream(file);
            return handleSvgzStream(uri, fis);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "file is not found in application storage from URL: " + uri);
            return null;
        }
    }

    /**
     * Open a file with a nested path given the parent file.
     * It follows the path by opening a {@code File} for each segement in the path.
     *
     * @param parent the parent directory.
     * @param pathSegments path segements to open, last segment should not be a directory.
     * @param createIfNotExist create a directory if it doesn't exist.
     * @return {@code File} for the last segment in the path, or {@code null} if a segment doesn't
     *                      exist and createIfNotExist is {@code false}.
     */
    @VisibleForTesting
    /* package */ static File openNestedPath(@NonNull File parent,
            @NonNull List<String> pathSegments,
            boolean createIfNotExist) {
        File file = parent;
        for (String seg : pathSegments) {
            if (!file.exists()) {
                if (createIfNotExist) {
                    file.mkdirs();
                } else {
                    return null;
                }
            }
            if (!file.isDirectory()) {
                return null;
            }
            file = new File(file, seg);
        }
        if (createIfNotExist || file.exists()) {
            return file;
        }
        return null;
    }

}
