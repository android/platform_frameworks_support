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

package androidx.webkit;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.webkit.internal.AssetHelper;

import java.io.InputStream;
import java.net.URLConnection;

/**
 * Helper class meant to be used with the android.webkit.WebView class to enable hosting assets,
 * resources and other data on 'virtual' http(s):// URL.
 * Hosting assets and resources on http(s):// URLs is desirable as it is compatible with the
 * Same-Origin policy.
 *
 * This class is intended to be used from within the
 * {@link android.webkit.WebViewClient#shouldInterceptRequest(android.webkit.WebView,
 * android.webkit.WebResourceRequest)}
 * methods.
 * <pre>
 *     WebViewAssetLoader localServer = new WebViewAssetLoader(this);
 *     // For security WebViewAssetLoader uses a unique subdomain by default.
 *     AssetHostingDetails ahd = localServer.hostAssets();
 *     webView.setWebViewClient(new WebViewClient() {
 *         @Override
 *         public WebResourceResponse shouldInterceptRequest(WebView view,
 *                                          WebResourceRequest request) {
 *             return localServer.shouldInterceptRequest(request);
 *         }
 *     });
 *     // If your application's assets are in the "main/assets" folder this will read the file
 *     // from "main/assets/www/index.html" and load it as if it were hosted on:
 *     // https://apkassets.androidplatform.net/assets/www/index.html
 *     webview.loadUrl(ahd.getHttpsPrefix().buildUpon().appendPath("index.html")
 *                              .build().toString());
 *
 * </pre>
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class WebViewAssetLoader {
    private static final String TAG = "WebViewAssetLoader";

    /**
     * Using http(s):// URL to access local resources may conflict with a real website. This means
     * that local resources should only be hosted on domains that the user has control of or which
     * have been dedicated for this purpose.
     *
     * The androidplatform.net domain currently belongs to Google and has been reserved for the
     * purpose of Android applications intercepting navigations/requests directed there. It'll be
     * used by default unless the user specified a different domain.
     *
     * A subdomain "apkassets" will be used to even make sure no such collisons would happen.
     */
    public static final String KNOWN_UNUSED_AUTHORITY = "apkassets.androidplatform.net";

    private static final String HTTP_SCHEME = "http";
    private static final String HTTPS_SCHEME = "https";

    @NonNull private final AssetHelper mAssetHelper;
    @Nullable @VisibleForTesting PathHandler mAssetsHandler;
    @Nullable @VisibleForTesting PathHandler mResourcesHandler;

    /**
     * A handler that produces responses for paths on the virtual asset server.
     *
     * Methods of this handler will be invoked on a background thread and care must be taken to
     * correctly synchronize access to any shared state.
     *
     * On Android KitKat and above these methods may be called on more than one thread. This thread
     * may be different than the thread on which the shouldInterceptRequest method was invoked.
     * This means that on Android KitKat and above it is possible to block in this method without
     * blocking other resources from loading. The number of threads used to parallelize loading
     * is an internal implementation detail of the WebView and may change between updates which
     * means that the amount of time spent blocking in this method should be kept to an absolute
     * minimum.
     */
    @VisibleForTesting
    /*package*/ abstract static class PathHandler {
        @Nullable private String mMimeType;
        @Nullable private String mEncoding;

        final boolean mHttpEnabled;
        @NonNull final String mAuthority;
        @NonNull final String mPath;

        /**
         * Add a URI to match, and the handler to return when this URI is
         * matched. Matches URIs on the form: "scheme://authority/path/**"
         *
         * @param scheme the scheme (http/https) to match
         * @param authority the authority to match (For example example.com)
         * @param path the prefix path to match. Should start with a slash "/".
         * @param httpEnabled whether to enable hosting using the http scheme.
         */
        PathHandler(@NonNull final String authority, @NonNull final String path,
                            boolean httpEnabled) {
            this.mMimeType = null;
            this.mEncoding = null;
            this.mAuthority = authority;
            this.mPath = path;
            this.mHttpEnabled = httpEnabled;
        }

        @Nullable
        public abstract InputStream handle(@NonNull Uri url);

        /**
         * Match happens when:
         *      - Scheme is "https" or the scheme is "http" and http is enabled.
         *      - AND authority exact matches the given URI's authority.
         *      - AND path is a prefix of the given URI's path.
         * @param uri The URI whose path we will match against.
         *
         * @return  true if match happens, false otherwise.
         */
        @Nullable
        public boolean match(@NonNull Uri uri) {
            if (!(mHttpEnabled && uri.getScheme().equals(HTTP_SCHEME))
                    && !uri.getScheme().equals(HTTPS_SCHEME)) {
                return false;
            }
            if (!uri.getAuthority().equals(mAuthority)) {
                return false;
            }

            if (uri.getPath().equals(mPath)) {
                return true;
            }

            return uri.getPath().startsWith(mPath.endsWith("/") ? mPath : mPath + "/");
        }

        @Nullable
        public String getMimeType() {
            return mMimeType;
        }

        @Nullable
        public String getEncoding() {
            return mEncoding;
        }

        void setMimeType(@Nullable String mimeType) {
            mMimeType = mimeType;
        }

        void setEncoding(@Nullable String encoding) {
            mEncoding = encoding;
        }
    }

    /**
     * Information about the URLs used to host the assets in the WebView.
     */
    public static class AssetHostingDetails {
        @Nullable private Uri mHttpPrefix;
        @Nullable private Uri mHttpsPrefix;

        /*package*/ AssetHostingDetails(@Nullable Uri httpPrefix, @Nullable Uri httpsPrefix) {
            this.mHttpPrefix = httpPrefix;
            this.mHttpsPrefix = httpsPrefix;
        }

        /**
         * Gets the http: scheme prefix at which assets are hosted.
         * @return  the http: scheme prefix at which assets are hosted. Can return null.
         */
        @Nullable
        public Uri getHttpPrefix() {
            return mHttpPrefix;
        }

        /**
         * Gets the https: scheme prefix at which assets are hosted.
         * @return  the https: scheme prefix at which assets are hosted. Can return null.
         */
        @Nullable
        public Uri getHttpsPrefix() {
            return mHttpsPrefix;
        }
    }

    @VisibleForTesting
    /*package*/ WebViewAssetLoader(@NonNull AssetHelper assetHelper) {
        this.mAssetHelper = assetHelper;
    }

    /**
     * Creates a new instance of the WebView local server.
     * Will use a default domain on the form of:
     * {app_package_name_sha1_hex}.androidplatform.net
     * Where app_package_name_sha1_hex is the hexadecimal representaion of the SHA-1 hash code of
     * the application package name.
     *
     * @param context context used to resolve resources/assets.
     */
    public WebViewAssetLoader(@NonNull Context context) {
        this(new AssetHelper(context.getApplicationContext()));
    }

    @Nullable
    private static Uri parseAndVerifyUrl(@Nullable String url) {
        if (url == null) {
            return null;
        }
        Uri uri = Uri.parse(url);
        if (uri == null) {
            Log.e(TAG, "Malformed URL: " + url);
            return null;
        }
        String path = uri.getPath();
        if (path == null || path.length() == 0) {
            Log.e(TAG, "URL does not have a path: " + url);
            return null;
        }
        return uri;
    }

    /**
     * Attempt to retrieve the WebResourceResponse associated with the given <code>request</code>.
     * This method should be invoked from within
     * {@link android.webkit.WebViewClient#shouldInterceptRequest(android.webkit.WebView,
     * android.webkit.WebResourceRequest)}.
     *
     * @param request the request to process.
     * @return a response if the request URL had a matching handler, null if no handler was found.
     */
    @RequiresApi(21)
    @Nullable
    public WebResourceResponse shouldInterceptRequest(WebResourceRequest request) {
        PathHandler handler;

        if (mAssetsHandler != null && mAssetsHandler.match(request.getUrl())) {
            handler = mAssetsHandler;
        } else if (mResourcesHandler != null && mResourcesHandler.match(request.getUrl())) {
            handler = mResourcesHandler;
        } else {
            return null;
        }

        InputStream is = handler.handle(request.getUrl());
        return new WebResourceResponse(handler.getMimeType(), handler.getEncoding(), is);
    }

    /**
     * Attempt to retrieve the WebResourceResponse associated with the given <code>url</code>.
     * This method should be invoked from within
     * {@link android.webkit.WebViewClient#shouldInterceptRequest(android.webkit.WebView, String)}.
     *
     * @param url the url to process.
     * @return a response if the request URL had a matching handler, null if no handler was found.
     */
    @Nullable
    public WebResourceResponse shouldInterceptRequest(@Nullable String url) {
        PathHandler handler = null;
        Uri uri = parseAndVerifyUrl(url);
        if (uri == null) {
            return null;
        }

        if (mAssetsHandler != null && mAssetsHandler.match(uri)) {
            handler = mAssetsHandler;
        } else if (mResourcesHandler != null && mResourcesHandler.match(uri)) {
            handler = mResourcesHandler;
        } else {
            return null;
        }

        InputStream is = handler.handle(uri);
        return new WebResourceResponse(handler.getMimeType(), handler.getEncoding(), is);
    }

    /**
     * Hosts the application's assets on an http(s):// URL. It will be available under
     * <code>http(s)://apkassets.androidplatform.net/assets/...</code>.
     *
     * @return prefixes under which the assets are hosted.
     */
    @NonNull
    public AssetHostingDetails hostAssets() {
        return hostAssets(KNOWN_UNUSED_AUTHORITY, "/assets", true);
    }


    /**
     * Hosts the application's assets on an http(s):// URL. It will be available under
     * <code>http(s)://apkassets.androidplatform.net/{virtualAssetPath}/...</code>.
     *
     * @param virtualAssetPath the path on the local server under which the assets should be hosted.
     *                         Should start with a leading slash (for example "/assets/www").
     * @param enableHttp whether to enable hosting using the http scheme.
     * @return prefixes under which the assets are hosted.
     */
    @NonNull
    public AssetHostingDetails hostAssets(@NonNull final String virtualAssetPath,
                                            boolean enableHttp) {
        return hostAssets(KNOWN_UNUSED_AUTHORITY, virtualAssetPath, enableHttp);
    }

    /**
     * Hosts the application's assets on an http(s):// URL. It will be available under
     * <code>http(s)://{domain}/{virtualAssetPath}/...</code>.
     *
     * @param domain custom domain on which the assets should be hosted (for example "example.com").
     * @param virtualAssetPath the path on the local server under which the assets should be hosted.
     *                         Should start with a leading slash (for example "/assets/www").
     * @param enableHttp whether to enable hosting using the http scheme.
     * @return prefixes under which the assets are hosted.
     */
    @NonNull
    public AssetHostingDetails hostAssets(@NonNull final String domain,
                                          @NonNull final String virtualAssetPath,
                                          boolean enableHttp) {
        if (virtualAssetPath.indexOf('*') != -1) {
            throw new IllegalArgumentException(
                    "virtualAssetPath cannot contain the '*' character.");
        }
        if (virtualAssetPath.isEmpty() || virtualAssetPath.charAt(0) != '/') {
            throw new IllegalArgumentException(
                    "virtualAssetPath should start with a slash '/' character.");
        }

        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(domain);
        uriBuilder.path(virtualAssetPath);
        uriBuilder.scheme(HTTPS_SCHEME);

        Uri httpsPrefix = uriBuilder.build();
        Uri httpPrefix = null;

        if (enableHttp) {
            uriBuilder.scheme(HTTP_SCHEME);
            httpPrefix = uriBuilder.build();
        }

        mAssetsHandler = new PathHandler(httpPrefix.getAuthority(), httpPrefix.getPath(),
                                            enableHttp) {
            @Override
            public InputStream handle(Uri url) {
                String path = url.getPath().replaceFirst(virtualAssetPath, "");
                Uri.Builder assetUriBuilder = new Uri.Builder();
                assetUriBuilder.path(path);
                Uri assetUri = assetUriBuilder.build();

                InputStream stream = mAssetHelper.openAsset(assetUri);
                this.setMimeType(URLConnection.guessContentTypeFromName(assetUri.getPath()));

                return stream;
            }
        };

        return new AssetHostingDetails(httpPrefix, httpsPrefix);
    }

    /**
     * Hosts the application's resources on an http(s):// URL. Resources
     * <code>http(s)://apkassets.androidplatform.net/res/{resource_type}/
     * {resource_name}</code>.
     *
     * @return prefixes under which the resources are hosted.
     */
    @NonNull
    public AssetHostingDetails hostResources() {
        return hostResources(KNOWN_UNUSED_AUTHORITY, "/res", true);
    }

    /**
     * Hosts the application's resources on an http(s):// URL. Resources
     * <code>http(s)://apkassets.androidplatform.net/{virtualResourcesPath}/
     * {resource_type}/{resource_name}</code>.
     *
     * @param virtualResourcesPath the path on the local server under which the resources should
     *                             be hosted. Should start with a leading slash (for example
     *                              "/resources").
     * @param enableHttp whether to enable hosting using the http scheme.
     * @return prefixes under which the resources are hosted.
     */
    @NonNull
    public AssetHostingDetails hostResources(@NonNull final String virtualResourcesPath,
                                             boolean enableHttp) {
        return hostResources(KNOWN_UNUSED_AUTHORITY, virtualResourcesPath, enableHttp);
    }

    /**
     * Hosts the application's resources on an http(s):// URL. Resources
     * <code>http(s)://{domain}/{virtualResourcesPath}/{resource_type}/{resource_name}</code>.
     *
     * @param domain custom domain on which the assets should be hosted (for example "example.com").
     * @param virtualResourcesPath the path on the local server under which the resources
     *                             should be hosted. Should start with a leading slash (for example
     *                             "/resources").
     * @param enableHttp whether to enable hosting using the http scheme.
     * @return prefixes under which the resources are hosted.
     */
    @NonNull
    public AssetHostingDetails hostResources(@NonNull final String domain,
                                             @NonNull final String virtualResourcesPath,
                                             boolean enableHttp) {
        if (virtualResourcesPath.indexOf('*') != -1) {
            throw new IllegalArgumentException(
                    "virtualResourcesPath cannot contain the '*' character.");
        }
        if (virtualAssetPath.isEmpty() || virtualAssetPath.charAt(0) != '/') {
            throw new IllegalArgumentException(
                    "virtualAssetPath should start with a slash '/' character.");
        }

        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(domain);
        uriBuilder.path(virtualResourcesPath);
        uriBuilder.scheme(HTTPS_SCHEME);

        Uri httpsPrefix = uriBuilder.build();
        Uri httpPrefix = null;
        if (enableHttp) {
            uriBuilder.scheme(HTTP_SCHEME);
            httpPrefix = uriBuilder.build();
        }

        mResourcesHandler = new PathHandler(httpPrefix.getAuthority(), httpPrefix.getPath(),
                                            enableHttp) {
            @Override
            public InputStream handle(Uri url) {
                String path = url.getPath().replaceFirst(virtualResourcesPath, "");
                Uri.Builder resourceUriBuilder = new Uri.Builder();
                resourceUriBuilder.path(path);
                Uri resourceUri = resourceUriBuilder.build();

                InputStream stream  = mAssetHelper.openResource(resourceUri);
                this.setMimeType(URLConnection.guessContentTypeFromName(resourceUri.getPath()));

                return stream;
            }
        };

        return new AssetHostingDetails(httpPrefix, httpsPrefix);
    }
}
