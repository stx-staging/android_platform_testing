/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.server.wm.flicker.service.assertors.common

import com.android.server.wm.flicker.service.assertors.BaseAssertion
import com.android.server.wm.traces.common.ComponentMatcher

/**
 * Base class for tests that require a [componentMatcher] based on a window name
 */
abstract class ComponentBaseTest(windowName: String) : BaseAssertion() {
    protected val componentMatcher = ComponentMatcher.unflattenFromString(windowName)
}
