/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.media.audiotestharness.client.grpc;

import static org.junit.Assert.assertNotNull;

import io.grpc.ManagedChannelBuilder;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Executors;

@RunWith(JUnitParamsRunner.class)
public class GrpcAudioTestHarnessClientTests {

    /** Tests for the {@link GrpcAudioTestHarnessClient.Builder} */
    @Test(expected = NullPointerException.class)
    public void setAddress_throwsNullPointerException_nullHostname() throws Exception {
        GrpcAudioTestHarnessClient.builder().setAddress(/* hostname= */ null, /* port= */ 8080);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setAddress_throwsIllegalArgumentException_badPort() throws Exception {
        GrpcAudioTestHarnessClient.builder().setAddress("localhost", /* port= */ -123);
    }

    @Test(expected = IllegalStateException.class)
    public void build_throwsIllegalStateException_hostNotSet() throws Exception {
        GrpcAudioTestHarnessClient.builder().build();
    }

    @Test
    @Parameters(method = "getBuildParameters")
    public void build_returnsNonNullInstance_validParameters(
            GrpcAudioTestHarnessClient.Builder builder) throws Exception {
        assertNotNull(builder.build());
    }

    public static Object[] getBuildParameters() {
        return new Object[][] {
            {GrpcAudioTestHarnessClient.builder().setAddress("localhost", /* port= */ 8080)},
            {
                GrpcAudioTestHarnessClient.builder()
                        .setAddress("service.google.com", 49152)
                        .setExecutor(Executors.newSingleThreadExecutor())
            },
            {
                GrpcAudioTestHarnessClient.builder()
                        .setManagedChannel(
                                ManagedChannelBuilder.forAddress("localhost", /* port= */ 8080)
                                        .usePlaintext()
                                        .build())
            }
        };
    }
}
