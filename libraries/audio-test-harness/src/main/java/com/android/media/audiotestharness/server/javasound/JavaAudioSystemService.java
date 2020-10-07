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

package com.android.media.audiotestharness.server.javasound;

import com.android.media.audiotestharness.proto.AudioDeviceOuterClass.AudioDevice;
import com.android.media.audiotestharness.proto.AudioFormatOuterClass.AudioFormat;
import com.android.media.audiotestharness.server.core.AudioCapturer;
import com.android.media.audiotestharness.server.core.AudioSystemService;

import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.sound.sampled.Mixer;

public class JavaAudioSystemService implements AudioSystemService {

    private final JavaAudioSystem mAudioSystem;

    /**
     * Cache of {@link com.android.media.audiotestharness.proto.AudioDeviceOuterClass.AudioDevice}
     * that were provided by the {@link #getDevices()} method to the {@link Mixer.Info} they
     * correspond to.
     *
     * <p>This cache is reset upon ever call to the {@link #getDevices()} method, and is used
     * primarily to simplify the {@link #createCapturerFor(AudioDevice, AudioFormat)} method
     * significantly.
     */
    private final Map<AudioDevice, Mixer.Info> mDeviceMixerMap;

    @Inject
    public JavaAudioSystemService(JavaAudioSystem javaAudioSystem) {
        this.mAudioSystem = javaAudioSystem;
        this.mDeviceMixerMap = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Makes use of the Java Sound API to get currently configured devices. Devices are marked as
     * having the {@link AudioDevice.Capability#CAPTURE} capability if they have a {@link
     * javax.sound.sampled.TargetDataLine} available, meaning that audio data can be read from them.
     * Currently only sets the {@link AudioDevice}'s name to the name provided by the {@link
     * Mixer.Info} corresponding to the device.
     */
    @Override
    public ImmutableSet<AudioDevice> getDevices() {
        mDeviceMixerMap.clear();
        ImmutableSet.Builder<AudioDevice> devices = ImmutableSet.builder();

        for (Mixer.Info mixerInfo : mAudioSystem.getMixerInfo()) {
            AudioDevice.Builder audioDeviceBuilder = AudioDevice.newBuilder();
            Mixer mixer = mAudioSystem.getMixer(mixerInfo);

            audioDeviceBuilder.setName(mixer.getMixerInfo().getName());

            if (mixer.getTargetLineInfo().length > 0) {
                audioDeviceBuilder.addCapabilities(AudioDevice.Capability.CAPTURE);
            }

            AudioDevice device = audioDeviceBuilder.build();

            devices.add(device);
            mDeviceMixerMap.put(device, mixerInfo);
        }

        return devices.build();
    }

    @Override
    public AudioCapturer createCapturerFor(AudioDevice device, AudioFormat audioFormat)
            throws IOException {
        return null;
    }
}
