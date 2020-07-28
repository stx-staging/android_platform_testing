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

package com.android.server.wm.flicker

import android.app.Instrumentation
import android.platform.helpers.AbstractStandardAppHelper
import com.android.launcher3.tapl.LauncherInstrumentation

/**
 * Class to take advantage of {@code IAppHelper} interface so the same test can be run against first
 * party and third party apps.
 */
open class StandardAppHelper @JvmOverloads constructor(
    instr: Instrumentation,
    protected val packageName: String,
    protected val appName: String
) : AbstractStandardAppHelper(instr) {
    private val mLauncher = LauncherInstrumentation(instr)

    override fun open() {
        mLauncher.pressHome().switchToAllApps().getAppIcon(appName).launch(packageName)
    }

    /** {@inheritDoc}  */
    override fun getPackage(): String {
        return packageName
    }

    /** {@inheritDoc}  */
    override fun getLauncherName(): String {
        return appName
    }

    /** {@inheritDoc}  */
    override fun dismissInitialDialogs() {}
}
