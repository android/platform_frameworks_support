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

package androidx.media2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;

import androidx.test.filters.SmallTest;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests {@link MediaSessionService2}.
 */
@SmallTest
public class MediaSessionService2Test extends MediaSession2TestBase {
    private SessionToken2 mToken;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        TestServiceRegistry.getInstance().setHandler(sHandler);
        mToken = new SessionToken2(mContext,
                new ComponentName(mContext, MockMediaSessionService2.class));
    }

    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
        TestServiceRegistry.getInstance().cleanUp();
    }

    @Test
    public void testOnGetSession() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final List<SessionToken2> tokens = new ArrayList<>();
        TestServiceRegistry.getInstance().setOnGetSessionHandler(
                new TestServiceRegistry.OnGetSessionHandler() {
                    @Override
                    public MediaSession2 onGetSession() {
                        MockPlayer player = new MockPlayer(1);
                        MediaSession2 session = new MediaSession2.Builder(mContext)
                                .setPlayer(player)
                                .setSessionCallback(sHandlerExecutor,
                                        new MediaSession2.SessionCallback() {})
                                .setId("testOnGetSession" + latch.getCount()).build();
                        tokens.add(session.getToken());
                        latch.countDown();
                        return session;
                    }
                });

        MediaController2 controller1 = createController(mToken);
        MediaController2 controller2 = createController(mToken);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertNotEquals(controller1.getConnectedSessionToken(),
                controller2.getConnectedSessionToken());
        assertEquals(tokens.get(0), controller1.getConnectedSessionToken());
        assertEquals(tokens.get(1), controller2.getConnectedSessionToken());
    }

    @Test
    public void testGetSessions() throws InterruptedException {
        createController(mToken);
        MediaSessionService2 service = TestServiceRegistry.getInstance().getServiceInstance();
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setId("testAddSession")
                .setSessionCallback(sHandlerExecutor, new MediaSession2.SessionCallback() { })
                .build()) {
            service.addSession(session);
            List<MediaSession2> sessions = service.getSessions();
            assertTrue(sessions.contains(session));
            assertEquals(2, sessions.size());

            service.removeSession(session);
            sessions = service.getSessions();
            assertFalse(sessions.contains(session));
        }
    }

    @Test
    public void testAddSessions_removedWhenClose() throws InterruptedException {
        createController(mToken);
        MediaSessionService2 service = TestServiceRegistry.getInstance().getServiceInstance();
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setId("testAddSession")
                .setSessionCallback(sHandlerExecutor, new MediaSession2.SessionCallback() { })
                .build()) {
            service.addSession(session);
            List<MediaSession2> sessions = service.getSessions();
            assertTrue(sessions.contains(session));
            assertEquals(2, sessions.size());

            session.close();
            sessions = service.getSessions();
            assertFalse(sessions.contains(session));
        }
    }
}
