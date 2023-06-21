/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.platform.test.flag.junit;

import static org.junit.Assume.assumeTrue;

import android.platform.test.annotations.RequiresFlagsOff;
import android.platform.test.annotations.RequiresFlagsOn;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A test rule that checks flags condition and skips tests with "assumption failed" if the condition
 * can not be met.
 */
public final class CheckFlagsRule implements TestRule {
    private final IFlagsValueProvider mFlagsValueProvider;

    public CheckFlagsRule(IFlagsValueProvider flagsValueProvider) {
        mFlagsValueProvider = flagsValueProvider;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                AnnotationsRetriever.FlagAnnotations flagAnnotations =
                        AnnotationsRetriever.getFlagAnnotations(description);
                RequiresFlagsOn requiresFlagsOn = flagAnnotations.mRequiresFlagsOn;
                RequiresFlagsOff requiresFlagsOff = flagAnnotations.mRequiresFlagsOff;
                mFlagsValueProvider.setUp();
                try {
                    if (requiresFlagsOn != null) {
                        for (String flag : requiresFlagsOn.value()) {
                            assumeTrue(
                                    String.format("Flag %s requires to be on, but is off", flag),
                                    mFlagsValueProvider.getBoolean(flag));
                        }
                    }
                    if (requiresFlagsOff != null) {
                        for (String flag : requiresFlagsOff.value()) {
                            assumeTrue(
                                    String.format("Flag %s requires to be off, but is on", flag),
                                    !mFlagsValueProvider.getBoolean(flag));
                        }
                    }
                } finally {
                    mFlagsValueProvider.tearDownBeforeTest();
                }
                base.evaluate();
            }
        };
    }
}
