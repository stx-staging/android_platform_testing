/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.boothelper;

import static android.platform.helpers.LockscreenUtils.LockscreenType.PIN;
import static android.platform.helpers.LockscreenUtils.checkDeviceLock;

import android.platform.systemui_tapl.controller.LockscreenController;
import android.platform.systemui_tapl.ui.Root;

import org.junit.Test;

public class BootHelperTest {

    private static final String VALID_PIN = "1234";

    @Test
    public void setupLockScreenPin() throws Exception {
        LockscreenController.get().setLockscreenPin(VALID_PIN);
        LockscreenController.get().lockScreen();
    }

    @Test
    public void unlockScreenWithPin() throws Exception {
        checkDeviceLock(true);
        Root.get().getLockScreen().swipeUpToBouncer().unlockViaCode(PIN, VALID_PIN);
        Root.get().assertLauncherVisible();
    }
}
