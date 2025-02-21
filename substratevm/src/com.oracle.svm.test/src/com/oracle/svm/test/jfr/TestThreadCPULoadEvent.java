/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2023, 2023, BELLSOFT. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.oracle.svm.test.jfr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;

public class TestThreadCPULoadEvent extends JfrRecordingTest {
    private static final int DURATION_MS = 1000;
    private static final String THREAD_NAME_1 = "Thread-1";
    private static final String THREAD_NAME_2 = "Thread-2";

    @Test
    public void test() throws Throwable {
        String[] events = new String[]{"jdk.ThreadCPULoad"};
        Recording recording = startRecording(events);

        Thread thread1 = createAndStartBusyWaitThread(THREAD_NAME_1, 0.1);
        Thread thread2 = createAndStartBusyWaitThread(THREAD_NAME_2, 1.0);

        thread1.join();
        thread2.join();

        stopRecording(recording, TestThreadCPULoadEvent::validateEvents);
    }

    private static void validateEvents(List<RecordedEvent> events) {
        assertEquals(2, events.size());
        Map<String, Float> userTimes = new HashMap<>();
        Map<String, Float> cpuTimes = new HashMap<>();

        for (RecordedEvent e : events) {
            String threadName = e.getThread().getJavaName();
            float userTime = e.<Float> getValue("user");
            float systemTime = e.<Float> getValue("system");
            assertTrue("User time is outside 0..1 range", 0.0 <= userTime && userTime <= 1.0);
            assertTrue("System time is outside 0..1 range", 0.0 <= systemTime && systemTime <= 1.0);

            userTimes.put(threadName, userTime);
            cpuTimes.put(threadName, userTime + systemTime);
        }

        assertTrue(userTimes.get(THREAD_NAME_1) < userTimes.get(THREAD_NAME_2));
        assertTrue(cpuTimes.get(THREAD_NAME_1) < cpuTimes.get(THREAD_NAME_2));
    }

    private static Thread createAndStartBusyWaitThread(String name, double busyPercent) {
        Thread thread = new Thread(() -> {
            assert busyPercent >= 0 && busyPercent <= 1;
            long busyMs = (long) (DURATION_MS * busyPercent);
            long idleMs = DURATION_MS - busyMs;

            busyWait(busyMs);
            sleep(idleMs);
        });
        thread.setName(name);
        thread.start();
        return thread;
    }

    private static void busyWait(long delay) {
        long end = System.currentTimeMillis() + delay;
        while (end > System.currentTimeMillis()) {
            /* Nothing to do. */
        }
    }

    private static void sleep(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ignored) {
        }
    }
}
