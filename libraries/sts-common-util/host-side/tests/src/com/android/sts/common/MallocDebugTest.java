/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.sts.common;

import static com.google.common.truth.Truth.assertWithMessage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.android.tradefed.testtype.DeviceJUnit4ClassRunner;
import com.android.tradefed.testtype.junit4.BaseHostJUnit4Test;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/** Unit tests for {@link MallocDebug}. */
@RunWith(DeviceJUnit4ClassRunner.class)
public class MallocDebugTest extends BaseHostJUnit4Test {
    private static String logcatWithErrors = null;
    private static String logcatWithoutErrors = null;

    @BeforeClass
    public static void setUpClass() throws IOException {
        try (BufferedReader reader1 =
                        new BufferedReader(
                                new InputStreamReader(
                                        MallocDebugTest.class
                                                .getClassLoader()
                                                .getResourceAsStream("malloc_debug_logcat.txt")));
                BufferedReader reader2 =
                        new BufferedReader(
                                new InputStreamReader(
                                        MallocDebugTest.class
                                                .getClassLoader()
                                                .getResourceAsStream("logcat.txt")))) {
            StringBuffer input1 = new StringBuffer();
            String tmp1;
            while ((tmp1 = reader1.readLine()) != null) {
                input1.append(tmp1 + "\n");
            }
            logcatWithErrors = new String(input1.toString());

            StringBuffer input2 = new StringBuffer();
            String tmp2;
            while ((tmp2 = reader2.readLine()) != null) {
                input2.append(tmp2 + "\n");
            }
            logcatWithoutErrors = new String(input2.toString());
        }
    }

    @Before
    public void setUp() throws Exception {
        assertWithMessage("libc.debug.malloc.options not empty before test")
                .that(getDevice().getProperty("libc.debug.malloc.options"))
                .isNull();
        assertWithMessage("libc.debug.malloc.programs not empty before test")
                .that(getDevice().getProperty("libc.debug.malloc.programs"))
                .isNull();
    }

    @After
    public void tearDown() throws Exception {
        assertWithMessage("libc.debug.malloc.options not empty after test")
                .that(getDevice().getProperty("libc.debug.malloc.options"))
                .isNull();
        assertWithMessage("libc.debug.malloc.programs not empty after test")
                .that(getDevice().getProperty("libc.debug.malloc.programs"))
                .isNull();
    }

    @Test(expected = Test.None.class /* no exception expected */)
    public void testMallocDebugNoErrors() throws Exception {
        MallocDebug.assertNoMallocDebugErrors(logcatWithoutErrors);
    }

    @Test(expected = AssertionError.class)
    public void testMallocDebugWithErrors() throws Exception {
        MallocDebug.assertNoMallocDebugErrors(logcatWithErrors);
    }

    @Test(expected = IllegalStateException.class)
    public void testMallocDebugAutocloseableNonRoot() throws Exception {
        assertTrue(getDevice().disableAdbRoot());
        try (AutoCloseable mallocDebug =
                MallocDebug.withLibcMallocDebugOnNewProcess(
                        getDevice(), "backtrace guard", "native-poc")) {
            // empty
        }
    }

    @Test
    public void testMallocDebugAutocloseableRoot() throws Exception {
        assertTrue("must test with rootable device", getDevice().enableAdbRoot());
        try (AutoCloseable mallocDebug =
                MallocDebug.withLibcMallocDebugOnNewProcess(
                        getDevice(), "backtrace guard", "native-poc")) {
            // empty
        }
    }

    @Test
    public void testMallocDebugAutocloseableNonRootCleanup() throws Exception {
        assertTrue("must test with rootable device", getDevice().enableAdbRoot());
        try (AutoCloseable mallocDebug =
                MallocDebug.withLibcMallocDebugOnNewProcess(
                        getDevice(), "backtrace guard", "native-poc")) {
            assertTrue("could not disable root", getDevice().disableAdbRoot());
        }
        assertFalse(
                "device should not be root after autoclose if the body unrooted",
                getDevice().isAdbRoot());
    }

    @Test
    public void testMallocDebugAutoseablePriorValueNoException() throws Exception {
        assertTrue("must test with rootable device", getDevice().enableAdbRoot());
        final String oldValue = "TEST_VALUE_OLD";
        final String newValue = "TEST_VALUE_NEW";
        assertTrue(
                "could not set libc.debug.malloc.options",
                getDevice().setProperty("libc.debug.malloc.options", oldValue));
        assertWithMessage("test property was not properly set on device")
                .that(getDevice().getProperty("libc.debug.malloc.options"))
                .isEqualTo(oldValue);
        try (AutoCloseable mallocDebug =
                MallocDebug.withLibcMallocDebugOnNewProcess(getDevice(), newValue, "native-poc")) {
            assertWithMessage("new property was not set during malloc debug body")
                    .that(getDevice().getProperty("libc.debug.malloc.options"))
                    .isEqualTo(newValue);
        }
        String afterValue = getDevice().getProperty("libc.debug.malloc.options");
        assertTrue(
                "could not clear libc.debug.malloc.options",
                getDevice().setProperty("libc.debug.malloc.options", ""));
        assertWithMessage("prior property was not restored after test")
                .that(afterValue)
                .isEqualTo(oldValue);
    }
}
