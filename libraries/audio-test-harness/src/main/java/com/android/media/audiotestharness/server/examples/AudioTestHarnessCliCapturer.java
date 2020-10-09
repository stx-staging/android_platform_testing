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

package com.android.media.audiotestharness.server.examples;

import com.android.media.audiotestharness.common.Defaults;
import com.android.media.audiotestharness.server.javasound.JavaAudioCapturer;
import com.android.media.audiotestharness.server.javasound.JavaSoundUtility;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

/**
 * Simple Audio Capture Binary that makes use of the Audio Test Harness libraries to capture raw
 * audio samples to a file named {@link #FILENAME}.
 */
public class AudioTestHarnessCliCapturer {
    private static final Logger LOGGER =
            Logger.getLogger(AudioTestHarnessCliCapturer.class.getName());

    /** Output file for the recording. */
    private static final String FILENAME = "output.raw";

    /** Duration that the recording will go until stopping. */
    private static final Duration RECORD_TIME = Duration.of(10, ChronoUnit.SECONDS);

    public static void main(String[] args) throws Exception {
        Mixer mixer = AudioSystem.getMixer(AudioSystem.getMixerInfo()[8]);
        LOGGER.info(String.format("Found Mixer: %s", mixer.getMixerInfo().getName()));

        TargetDataLine targetDataLine =
                (TargetDataLine)
                        mixer.getLine(
                                new DataLine.Info(
                                        TargetDataLine.class,
                                        JavaSoundUtility.audioFormatFrom(Defaults.AUDIO_FORMAT)));
        targetDataLine.open(JavaSoundUtility.audioFormatFrom(Defaults.AUDIO_FORMAT));

        LOGGER.info("New AudioCapturer...");
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        JavaAudioCapturer capturer =
                JavaAudioCapturer.create(
                        Defaults.AUDIO_DEVICE,
                        Defaults.AUDIO_FORMAT,
                        targetDataLine,
                        executorService);
        capturer.attachOutput(new File(FILENAME));

        LOGGER.info("Opening AudioCapturer...");
        capturer.open();

        // Put main thread to sleep since the capturer operates on a separate thread provided by the
        // above executor service.
        LOGGER.info(String.format("Sleeping for %s...", RECORD_TIME));
        Thread.sleep(RECORD_TIME.toMillis());

        LOGGER.info("Completed Capture. Cleaning up...");

        capturer.close();
        executorService.shutdown();
    }
}
