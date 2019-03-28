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

package androidx.media2.test.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;

import androidx.media2.MediaItem;
import androidx.media2.MediaMetadata;
import androidx.media2.MediaUtils;
import androidx.media2.UriMediaItem;
import androidx.media2.test.service.MediaTestUtils;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.versionedparcelable.ParcelImpl;
import androidx.versionedparcelable.ParcelUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests {@link MediaItem}.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MediaItemTest {
    private Context mContext;
<<<<<<< HEAD   (60b11c Merge "Merge empty history for sparse-5338950-L0630000027955)
=======
    private MediaItem mTestItem;

    private static final MediaItemFactory sMediaItemFactory = new MediaItemFactory() {
        @Override
        public MediaItem create(Context context) {
            final MediaMetadata testMetadata = new MediaMetadata.Builder()
                    .putLong("MediaItemTest", 1).build();
            return new MediaItem.Builder()
                    .setMetadata(testMetadata)
                    .setStartPosition(1)
                    .setEndPosition(10)
                    .build();
        }
    };

    private static final MediaItemFactory sUriMediaItemFactory = new MediaItemFactory() {
        @Override
        public MediaItem create(Context context) {
            final MediaMetadata testMetadata = new MediaMetadata.Builder()
                    .putString("MediaItemTest", "MediaItemTest").build();
            return new UriMediaItem.Builder(Uri.parse("test://test"))
                    .setMetadata(testMetadata)
                    .setStartPosition(1)
                    .setEndPosition(1000)
                    .build();
        }
    };

    private static final MediaItemFactory sCallbackMediaItemFactory = new MediaItemFactory() {
        @Override
        public MediaItem create(Context context) {
            final MediaMetadata testMetadata = new MediaMetadata.Builder()
                    .putText("MediaItemTest", "testtest").build();
            final DataSourceCallback callback = new DataSourceCallback() {
                @Override
                public int readAt(long position, @NonNull byte[] buffer, int offset, int size)
                        throws IOException {
                    return 0;
                }

                @Override
                public long getSize() throws IOException {
                    return 0;
                }

                @Override
                public void close() throws IOException {
                    // no-op
                }
            };
            return new CallbackMediaItem.Builder(callback)
                    .setMetadata(testMetadata)
                    .setStartPosition(0)
                    .setEndPosition(0)
                    .build();
        }
    };

    private static final MediaItemFactory sFileMediaItemFactory = new MediaItemFactory() {
        @Override
        public MediaItem create(Context context) {
            int resId = R.raw.midi8sec;
            try (AssetFileDescriptor afd = context.getResources().openRawResourceFd(resId)) {
                return new FileMediaItem.Builder(
                        ParcelFileDescriptor.dup(afd.getFileDescriptor())).build();
            } catch (Exception e) {
                return null;
            }
        }
    };

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {sMediaItemFactory, MediaItem.Builder.class},
                {sUriMediaItemFactory, UriMediaItem.Builder.class},
                {sCallbackMediaItemFactory, CallbackMediaItem.Builder.class},
                {sFileMediaItemFactory, FileMediaItem.Builder.class}});
    }

    public MediaItemTest(MediaItemFactory factory, Class builderClass) {
        mItemFactory = factory;
        mItemBuilderClass = builderClass;
    }
>>>>>>> BRANCH (e95ebf Merge "Merge cherrypicks of [936611, 936612] into sparse-541)

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testSubclass_sameProcess() {
        final UriMediaItem testUriItem = createUriMediaItem();
        final ParcelImpl parcel = MediaUtils.toParcelable(testUriItem);

        final MediaItem testRemoteItem = MediaUtils.fromParcelable(parcel);
        assertEquals(testUriItem, testRemoteItem);
    }

    @Test
    public void testSubclass_acrossProcessWithMediaUtils() {
        final UriMediaItem testUriItem = createUriMediaItem();

        // Mocks the binder call across the processes by using writeParcelable/readParcelable
        // which only happens between processes. Code snippets are copied from
        // VersionedParcelIntegTest#parcelCopy.
        final Parcel p = Parcel.obtain();
        p.writeParcelable(MediaUtils.toParcelable(testUriItem), 0);
        p.setDataPosition(0);
        final MediaItem testRemoteItem = MediaUtils.fromParcelable(
                (ParcelImpl) p.readParcelable(MediaItem.class.getClassLoader()));

        assertFalse(testRemoteItem instanceof UriMediaItem);
        assertEquals(testUriItem.getStartPosition(), testRemoteItem.getStartPosition());
        assertEquals(testUriItem.getEndPosition(), testRemoteItem.getEndPosition());
        MediaTestUtils.assertMediaMetadataEquals(
                testUriItem.getMetadata(), testRemoteItem.getMetadata());
    }

    @Test
    public void testSubclass_acrossProcessWithParcelUtils() {
        final UriMediaItem testUriItem = createUriMediaItem();

        // Mocks the binder call across the processes by using writeParcelable/readParcelable
        // which only happens between processes. Code snippets are copied from
        // VersionedParcelIntegTest#parcelCopy.
        try {
            final Parcel p = Parcel.obtain();
            p.writeParcelable(ParcelUtils.toParcelable(testUriItem), 0);
            p.setDataPosition(0);
            final MediaItem testRemoteItem = ParcelUtils.fromParcelable(
                    (ParcelImpl) p.readParcelable(MediaItem.class.getClassLoader()));
            fail("Write to parcel should fail for subclass of MediaItem");
        } catch (Exception e) {
        }
    }

    private UriMediaItem createUriMediaItem() {
        final MediaMetadata testMetadata = new MediaMetadata.Builder()
                .putString("MediaItemTest", "MediaItemTest").build();
        return new UriMediaItem.Builder(mContext, Uri.parse("test://test"))
                        .setMetadata(testMetadata)
                        .setStartPosition(1)
                        .setEndPosition(1000)
                        .build();
    }
}
