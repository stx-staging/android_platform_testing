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

package com.android.server.wm.flicker.assertiongenerator

import com.android.server.wm.flicker.assertiongenerator.common.AssertionObject

/**
 * Common class for [WindowManagerAssertionGeneratorTest] and [LayersAssertionGeneratorTest]
 */
class AssertionGeneratorTestConsts {
    companion object{
        // stub values for testing purposes
        // will be updated later to more meaningful values
        val expectedAppLaunchAssertions = arrayOf(
            AssertionObject(
                listOf(1, 2, 3),
                AssertionObject.Companion.AssertionFunction.IS_VISIBLE
            ),
            AssertionObject(
                listOf(4, 5, 6),
                AssertionObject.Companion.AssertionFunction.MOVES_RIGHT
            )
        )
    }
}