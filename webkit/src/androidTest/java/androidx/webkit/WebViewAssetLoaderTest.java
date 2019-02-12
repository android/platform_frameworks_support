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

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import androidx.webkit.internal.AssetHelper;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class WebViewAssetLoaderTest {
    private static final String TAG = "WebViewAssetLoaderTest";

    // The androidplatform.net domain currently belongs to Google and has been reserved for the
    // purpose of Android applications intercepting navigations/requests directed there.
    static final String KNOWN_UNUSED_AUTHORITY = "androidplatform.net";

    private static class RandomString {
        private static final Random sRandom = new Random();

        public static String next(int length) {
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; ++i) {
                sb.append('a' + sRandom.nextInt('z' - 'a'));
            }
            return sb.toString();
        }
    }

    private static String readAsString(InputStream is, String encoding) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[512];
        int len = 0;
        try {
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            return new String(os.toByteArray(), encoding);
        } catch (IOException e) {
            Log.e(TAG, "exception when reading the string", e);
            return "";
        }
    }

    private static class MockAssetHelper extends AssetHelper {
        MockAssetHelper() {
            super(null);
        }

        @Override
        public InputStream openAsset(Uri uri) {
            return null;
        }

        @Override
        public InputStream openResource(Uri uri) {
            return null;
        }
    }

    @Test
    @SmallTest
    public void testCustomPathHandler() {
        WebViewAssetLoader assetLoader = new WebViewAssetLoader(new MockAssetHelper());
        final String contents = RandomString.next(2000);
        final String encoding = "utf-8";

        assetLoader.mResourcesHandler = new WebViewAssetLoader.PathHandler("androidplatform.net",
                                                                        "/test", true, true) {
            @Override
            public String getEncoding() {
                return encoding;
            }

            @Override
            public InputStream handle(Uri url) {
                try {
                    return new ByteArrayInputStream(contents.getBytes(encoding));
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "exception when creating response", e);
                }
                return null;
            }
        };

        WebResourceResponse response =
                assetLoader.shouldInterceptRequest("http://androidplatform.net/test");
        Assert.assertNotNull(response);

        Assert.assertEquals(encoding, response.getEncoding());
        Assert.assertEquals(contents, readAsString(response.getData(), encoding));

        Assert.assertNull(assetLoader.shouldInterceptRequest("http://foo.bar/"));
    }

    @Test
    @SmallTest
    public void testHostAssets() {
        final String testHtmlContents = "<body><div>hah</div></body>";

        WebViewAssetLoader assetLoader = new WebViewAssetLoader(new MockAssetHelper() {
            @Override
            public InputStream openAsset(Uri url) {
                if (url.getPath().equals("/www/test.html")) {
                    try {
                        return new ByteArrayInputStream(testHtmlContents.getBytes("utf-8"));
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to open asset URL: " + url);
                        return null;
                    }
                }
                return null;
            }
        });

        WebViewAssetLoader.AssetHostingDetails details =
                assetLoader.hostAssets("androidplatform.net", "/www", "/assets", true, true);
        Assert.assertEquals(details.getHttpPrefix(), Uri.parse("http://androidplatform.net/assets"));
        Assert.assertEquals(details.getHttpsPrefix(), Uri.parse("https://androidplatform.net/assets"));

        WebResourceResponse response =
                assetLoader.shouldInterceptRequest("http://androidplatform.net/assets/test.html");
        Assert.assertNotNull(response);
        Assert.assertEquals(testHtmlContents, readAsString(response.getData(), "utf-8"));
    }

    @Test
    @SmallTest
    public void testHostResources() {
        final String testHtmlContents = "<body><div>hah</div></body>";

        WebViewAssetLoader assetLoader = new WebViewAssetLoader(new MockAssetHelper() {
            @Override
            public InputStream openResource(Uri uri) {
                Log.i(TAG, "host res: " + uri);
                try {
                    if (uri.getPath().equals("/raw/test.html")) {
                        return new ByteArrayInputStream(testHtmlContents.getBytes("utf-8"));
                    }
                } catch (IOException e) {
                    Log.e(TAG, "exception when creating response", e);
                }
                return null;
            }
        });

        WebViewAssetLoader.AssetHostingDetails details =
                assetLoader.hostResources("androidplatform.net", "/res", true, true);
        Assert.assertEquals(details.getHttpPrefix(), Uri.parse("http://androidplatform.net/res"));
        Assert.assertEquals(details.getHttpsPrefix(), Uri.parse("https://androidplatform.net/res"));

        WebResourceResponse response =
                 assetLoader.shouldInterceptRequest("http://androidplatform.net/res/raw/test.html");
        Assert.assertNotNull(response);
        Assert.assertEquals(testHtmlContents, readAsString(response.getData(), "utf-8"));
    }

    // An Activity for Integeration tests
    public static class TestActivity extends Activity {
        private class MyWebViewClient extends WebViewClient {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                mOnPageFinishedUrl.add(url);
            }

            @SuppressWarnings({"deprecated"})
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return mAssetLoader.shouldInterceptRequest(url);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view,
                                                WebResourceRequest request) {
                return mAssetLoader.shouldInterceptRequest(request);
            }
        }

        private WebViewAssetLoader mAssetLoader;
        private WebView mWebView;
        private ArrayBlockingQueue<String> mOnPageFinishedUrl = new ArrayBlockingQueue<String>(5);

        public WebViewAssetLoader getAssetLoader() {
            return mAssetLoader;

        }

        public WebView getWebView() {
            return mWebView;
        }

        public ArrayBlockingQueue<String> getOnPageFinishedUrl() {
            return mOnPageFinishedUrl;
        }

        private void setUpWebView(WebView view) {
            view.setWebViewClient(new MyWebViewClient());
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mAssetLoader = new WebViewAssetLoader(this);
            mWebView = new WebView(this);
            setUpWebView(mWebView);
            setContentView(mWebView);
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            mWebView.destroy();
            mWebView = null;
        }
    }


    @Rule
    public final ActivityTestRule<TestActivity> mActivityRule =
                                    new ActivityTestRule<>(TestActivity.class);

    @Test
    @MediumTest
    public void integrationTest_testAssetHosting() throws Exception {
        final TestActivity activity = mActivityRule.getActivity();
        final AtomicReference<String> url = new AtomicReference<String>();
        final String test_with_title_path = "test_with_title.html";

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                WebViewAssetLoader.AssetHostingDetails hostingDetails =
                        activity.getAssetLoader().hostAssets("www/", "/", true, true);
                Uri.Builder testPath =
                        hostingDetails.getHttpPrefix().buildUpon().appendPath(test_with_title_path);
                url.set(testPath.toString());
                Log.i(TAG, "loading: " + url.get());
                activity.getWebView().loadUrl(url.get());
            }
        });

        String onPageFinishedUrl = activity.getOnPageFinishedUrl().take();
        Assert.assertEquals(url.get(), onPageFinishedUrl);

        final AtomicReference<String> title = new AtomicReference<String>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                title.set(activity.getWebView().getTitle());
            }
        });
        Assert.assertEquals("WebViewAssetLoaderTest", title.get());
    }

    @Test
    @MediumTest
    public void integrationTest_testResourcesHosting() throws Exception {
        final TestActivity activity = mActivityRule.getActivity();
        final AtomicReference<String> url = new AtomicReference<String>();
        final String test_with_title_path = "test_with_title.html";

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                WebViewAssetLoader.AssetHostingDetails hostingDetails =
                        activity.getAssetLoader().hostResources();
                Uri.Builder testPath =
                        hostingDetails.getHttpPrefix().buildUpon()
                        .appendPath("raw")
                        .appendPath(test_with_title_path);
                url.set(testPath.toString());
                activity.getWebView().loadUrl(url.get());
            }
        });

        String onPageFinishedUrl = activity.getOnPageFinishedUrl().take();
        Assert.assertEquals(url.get(), onPageFinishedUrl);

        final AtomicReference<String> title = new AtomicReference<String>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                title.set(activity.getWebView().getTitle());
            }
        });
        Assert.assertEquals("WebViewAssetLoaderTest", title.get());
    }
}
