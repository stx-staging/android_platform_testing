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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.android.media.audiotestharness.proto.AudioDeviceOuterClass.AudioDevice;

import com.google.common.collect.ImmutableSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.sound.sampled.Mixer;

/** Tests for the {@link JavaAudioSystemService} class. */
@RunWith(JUnit4.class)
public class JavaSoundSystemServiceTests {

    @Rule public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock JavaAudioSystem mJavaAudioSystem;

    private JavaAudioSystemService mJavaAudioSystemService;

    @Before
    public void setUp() throws Exception {
        mJavaAudioSystemService = new JavaAudioSystemService(mJavaAudioSystem);
    }

    @Test
    public void getDevices_returnsEmptyList_noConfiguredDevices() {
        when(mJavaAudioSystem.getMixerInfo()).thenReturn(new Mixer.Info[0]);

        assertEquals(0, mJavaAudioSystemService.getDevices().size());
    }

    @Test
    public void getDevices_returnsProperDevices_twoConfiguredDevices() throws Exception {
        Mixer mixerOne = new TestMixer(/* name= */ "Mixer One", /* targetLineCount= */ 2);
        Mixer mixerTwo = new TestMixer(/* name= */ "Mixer Two", /* targetLineCount= */ 0);

        Mixer.Info[] mixerInfos = new Mixer.Info[2];
        mixerInfos[0] = mixerOne.getMixerInfo();
        mixerInfos[1] = mixerTwo.getMixerInfo();

        when(mJavaAudioSystem.getMixerInfo()).thenReturn(mixerInfos);
        when(mJavaAudioSystem.getMixer(mixerInfos[0])).thenReturn(mixerOne);
        when(mJavaAudioSystem.getMixer(mixerInfos[1])).thenReturn(mixerTwo);

        ImmutableSet<AudioDevice> expectedDevices =
                ImmutableSet.of(
                        AudioDevice.newBuilder()
                                .setName("Mixer One")
                                .addCapabilities(AudioDevice.Capability.CAPTURE)
                                .build(),
                        AudioDevice.newBuilder().setName("Mixer Two").build());
        ImmutableSet<AudioDevice> actualDevices = mJavaAudioSystemService.getDevices();

        assertEquals(expectedDevices, actualDevices);
    }
}
