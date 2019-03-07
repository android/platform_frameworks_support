<<<<<<< HEAD   (c6a768 Merge "Merge empty history for sparse-5330139-L6850000027064)
=======
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

package com.example.androidx.webkit;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;

/**
 * An {@link Activity} to show case a very simple use case of using
 * {@link androidx.webkit.WebViewAssetLoader}.
 */
public class AssetLoaderSimpleActivity extends AppCompatActivity {

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return false;
        }

        @Override
        @RequiresApi(21)
        public WebResourceResponse shouldInterceptRequest(WebView view,
                                            WebResourceRequest request) {
            return mAssetLoader.shouldInterceptRequest(request);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String request) {
            return mAssetLoader.shouldInterceptRequest(request);
        }
    }

    private WebViewAssetLoader mAssetLoader;
    private WebView mWebView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_asset_loader);
        setTitle(R.string.asset_loader_simple_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        mAssetLoader = (new WebViewAssetLoader.Builder(this)).build();
        mWebView = findViewById(R.id.webview_asset_loader_webview);
        mWebView.setWebViewClient(new MyWebViewClient());

        // Host application assets under http://appassets.androidplatform.net/assets/...
        Uri path = mAssetLoader.getAssetsHttpsPrefix().buildUpon()
                                                .appendPath("www")
                                                .appendPath("some_text.html").build();

        mWebView.loadUrl(path.toString());
    }
}
>>>>>>> BRANCH (085152 Merge "Merge cherrypicks of [922394] into sparse-5359448-L96)
