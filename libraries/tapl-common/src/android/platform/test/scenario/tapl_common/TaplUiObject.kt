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
package android.platform.test.scenario.tapl_common

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

/**
 * Ui object with diagnostic metadata and flake-free gestures.
 * @param [uiObject] UI Automator object
 * @param [name] Name of the object for diags
 */
class TaplUiObject constructor(val uiObject: UiObject2, private val name: String) {

    /** Wait for the object to become clickable and enabled, then clicks the object. */
    fun click() {
        Gestures.click(uiObject, name)
    }

    /**
     * Waits for a child UI object with a given resource id. Fails if the object is not visible.
     *
     * @param [resourceId] Resource id.
     * @param [childObjectName] Name of the object for diags.
     * @return The found UI object.
     */
    fun waitForChildObject(childResourceId: String, childObjectName: String): TaplUiObject {
        val selector = By.res(uiObject.applicationPackage, childResourceId)
        val childObject =
            uiObject.wait(Until.findObject(selector), TaplUiDevice.WAIT_TIME.toMillis())
                ?: throw AssertionError(
                    "UI object '$childObjectName' is not found in '$name'; selector: $selector."
                )
        return TaplUiObject(childObject, childObjectName)
    }
}