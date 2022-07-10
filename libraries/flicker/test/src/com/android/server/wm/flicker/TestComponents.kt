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

@file:JvmName("CommonConstants")
package com.android.server.wm.flicker

import com.android.server.wm.traces.common.ComponentMatcher

internal object TestComponents {
    @JvmStatic
    val CHROME = ComponentMatcher("com.android.chrome", "com.google.android.apps.chrome.Main")
    @JvmStatic
    val CHROME_FIRST_RUN = ComponentMatcher(
        "com.android.chrome",
        "org.chromium.chrome.browser.firstrun.FirstRunActivity"
    )
    @JvmStatic
    val CHROME_SPLASH_SCREEN = ComponentMatcher("", "Splash Screen com.android.chrome")
    @JvmStatic
    val DOCKER_STACK_DIVIDER = ComponentMatcher("", "DockedStackDivider")
    @JvmStatic
    val IMAGINARY = ComponentMatcher("", "ImaginaryWindow")
    @JvmStatic
    val IME_ACTIVITY = ComponentMatcher(
        "com.android.server.wm.flicker.testapp",
        "com.android.server.wm.flicker.testapp.ImeActivity"
    )
    @JvmStatic
    val LAUNCHER = ComponentMatcher(
        "com.google.android.apps.nexuslauncher",
        "com.google.android.apps.nexuslauncher.NexusLauncherActivity"
    )
    @JvmStatic
    val PIP_OVERLAY = ComponentMatcher("", "pip-dismiss-overlay")

    @JvmStatic
    val SIMPLE_APP = ComponentMatcher(
        "com.android.server.wm.flicker.testapp",
        "com.android.server.wm.flicker.testapp.SimpleActivity"
    )
    private const val SHELL_PKG_NAME = "com.android.wm.shell.flicker.testapp"
    @JvmStatic
    val SHELL_SPLIT_SCREEN_PRIMARY = ComponentMatcher(
        SHELL_PKG_NAME,
        "$SHELL_PKG_NAME.SplitScreenActivity"
    )
    @JvmStatic
    val SHELL_SPLIT_SCREEN_SECONDARY = ComponentMatcher(
        SHELL_PKG_NAME,
        "$SHELL_PKG_NAME.SplitScreenSecondaryActivity"
    )

    @JvmStatic
    val SCREEN_DECOR_OVERLAY = ComponentMatcher("", "ScreenDecorOverlay")
    @JvmStatic
    val WALLPAPER = ComponentMatcher(
        "", "com.breel.wallpapers18.soundviz.wallpaper.variations.SoundVizWallpaperV2"
    )
}
