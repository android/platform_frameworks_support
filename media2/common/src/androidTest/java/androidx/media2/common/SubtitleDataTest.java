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

package androidx.media2.common;

import static junit.framework.Assert.assertEquals;

import android.media.MediaFormat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests {@link SubtitleData}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class SubtitleDataTest {

    @Test
    public void testConstructor() {
        byte[] testData = {4, 3, 2, 1};
        final MediaFormat format = new MediaFormat();
        SubtitleData data = new SubtitleData(123, 456, testData);
        assertEquals(123, data.getStartTimeUs());
        assertEquals(456, data.getDurationUs());
        assertEquals(testData, data.getData());
    }
}
