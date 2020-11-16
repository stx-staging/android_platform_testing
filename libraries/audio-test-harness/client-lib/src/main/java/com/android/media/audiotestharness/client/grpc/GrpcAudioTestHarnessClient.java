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

import com.android.media.audiotestharness.client.core.AudioCaptureStream;
import com.android.media.audiotestharness.client.core.AudioTestHarnessClient;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** {@link AudioTestHarnessClient} that uses gRPC as its communication method. */
public class GrpcAudioTestHarnessClient extends AudioTestHarnessClient {

    private static final int MIN_PORT = 0;
    private static final int MAX_PORT = 65535;
    private static final int DEFAULT_NUM_THREADS = 8;

    // TODO(b/168812333): Implement this class to contain the required Executor and ManagedChannel
    // for spinning up new connections to the AudioTestHarness gRPC Server.

    private ManagedChannel mManagedChannel;

    private GrpcAudioTestHarnessClient(ManagedChannel managedChannel) {
        this.mManagedChannel = managedChannel;
    }

    public static GrpcAudioTestHarnessClient.Builder builder() {
        return new Builder();
    }

    @Override
    public AudioCaptureStream startCapture() {
        return null;
    }

    @Override
    public void close() {}

    /**
     * Builder for {@link GrpcAudioTestHarnessClient}s that allows for the injection of certain
     * members for testing purposes.
     */
    public static class Builder {

        private String mHostname;
        private int mPort;
        private Executor mExecutor;
        private ManagedChannel mManagedChannel;

        private Builder() {}

        public Builder setAddress(String hostname, int port) {
            Preconditions.checkNotNull(hostname, "Hostname cannot be null");
            Preconditions.checkArgument(
                    port >= MIN_PORT && port <= MAX_PORT,
                    String.format("Port expected in range [%d, %d]", MIN_PORT, MAX_PORT));

            mHostname = hostname;
            mPort = port;

            return this;
        }

        public Builder setExecutor(Executor executor) {
            mExecutor = executor;
            return this;
        }

        @VisibleForTesting
        Builder setManagedChannel(ManagedChannel managedChannel) {
            mManagedChannel = managedChannel;
            return this;
        }

        public GrpcAudioTestHarnessClient build() {
            if (mManagedChannel == null) {
                Preconditions.checkState(mHostname != null, "Address must be set.");

                if (mExecutor == null) {
                    mExecutor = Executors.newFixedThreadPool(DEFAULT_NUM_THREADS);
                }

                mManagedChannel =
                        ManagedChannelBuilder.forAddress(mHostname, mPort)
                                .executor(mExecutor)
                                .usePlaintext()
                                .build();
            }

            return new GrpcAudioTestHarnessClient(mManagedChannel);
        }
    }
}
