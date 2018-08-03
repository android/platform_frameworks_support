/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.remotecallback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BasicReceiverTest {

    private static Uri sUri;
    private static String sStr;
    private static int sInt;
    private static Integer sNullableInt;
    private static CountDownLatch sLatch;

    private final Context mContext = InstrumentationRegistry.getContext();

    @Test
    public void testCreateCallback() throws PendingIntent.CanceledException, InterruptedException {
        Uri aUri = new Uri.Builder().authority("mine").build();
        String something = "something";
        int i = 42;
        sLatch = new CountDownLatch(1);

        sUri = null;
        sStr = null;
        sInt = -1;
        sNullableInt = 15;

        new BroadcastReceiver().createRemoteCallback(mContext, "myCallbackMethod",
                aUri, something, i, null).toPendingIntent().send();

        sLatch.await(30, TimeUnit.SECONDS);

        assertEquals(0, sLatch.getCount());
        assertEquals(aUri, sUri);
        assertEquals(something, sStr);
        assertEquals(i, sInt);
        assertNull(sNullableInt);
    }

    public static class BroadcastReceiver extends BroadcastReceiverWithCallbacks {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("BasicReceiverTest", "onReceive " + intent);
            super.onReceive(context, intent);
        }

        @RemoteCallable
        public void myCallbackMethod(Uri myUri, String myStr, int myInt, Integer myNullableInt) {
            Log.d("BasicReceiverTest",
                    "myCallbackMethod " + myUri + " " + myStr + " " + myInt + " " + myNullableInt,
                    new Throwable());
            sUri = myUri;
            sStr = myStr;
            sInt = myInt;
            sNullableInt = myNullableInt;
            if (sLatch != null) sLatch.countDown();
        }
    }
}
