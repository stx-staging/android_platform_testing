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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.platform.test.annotations.RequiresFlagsOff;
import android.platform.test.annotations.RequiresFlagsOn;

import com.google.auto.value.AutoAnnotation;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.annotation.Annotation;

@RunWith(JUnit4.class)
public class AnnotationsRetrieverTest {
    static class TestClassHasNoAnnotation {}

    @RequiresFlagsOn({"flag1", "flag2"})
    static class TestClassHasRequiresFlagsOn {}

    @RequiresFlagsOff({"flag3", "flag4"})
    static class TestClassHasRequiresFlagsOff {}

    @RequiresFlagsOn({"flag1", "flag2"})
    @RequiresFlagsOff({"flag3", "flag4"})
    static class TestClassHasAllAnnotations {}

    private final RequiresFlagsOn mRequiresFlagsOn = createRequiresFlagsOn(new String[] {"flag5"});

    private final RequiresFlagsOff mRequiresFlagsOff =
            createRequiresFlagsOff(new String[] {"flag6"});

    @Test
    public void getFlagAnnotations_noAnnotation() {
        AnnotationsRetriever.FlagAnnotations flagAnnotations =
                getFlagAnnotations(TestClassHasNoAnnotation.class);

        assertNull(flagAnnotations.mRequiresFlagsOn);
        assertNull(flagAnnotations.mRequiresFlagsOff);
    }

    @Test
    public void getFlagAnnotations_oneAnnotationFromMethod() {
        AnnotationsRetriever.FlagAnnotations flagAnnotations1 =
                getFlagAnnotations(TestClassHasRequiresFlagsOn.class, mRequiresFlagsOn);
        AnnotationsRetriever.FlagAnnotations flagAnnotations2 =
                getFlagAnnotations(TestClassHasRequiresFlagsOff.class, mRequiresFlagsOff);

        assertNull(flagAnnotations1.mRequiresFlagsOff);
        assertEquals(
                flagAnnotations1.mRequiresFlagsOn, createRequiresFlagsOn(new String[] {"flag5"}));
        assertNull(flagAnnotations2.mRequiresFlagsOn);
        assertEquals(
                flagAnnotations2.mRequiresFlagsOff, createRequiresFlagsOff(new String[] {"flag6"}));
    }

    @Test
    public void getFlagAnnotations_oneAnnotationFromClass() {
        AnnotationsRetriever.FlagAnnotations flagAnnotations1 =
                getFlagAnnotations(TestClassHasRequiresFlagsOn.class);
        AnnotationsRetriever.FlagAnnotations flagAnnotations2 =
                getFlagAnnotations(TestClassHasRequiresFlagsOff.class);

        assertNull(flagAnnotations1.mRequiresFlagsOff);
        assertEquals(
                flagAnnotations1.mRequiresFlagsOn,
                createRequiresFlagsOn(new String[] {"flag1", "flag2"}));
        assertNull(flagAnnotations2.mRequiresFlagsOn);
        assertEquals(
                flagAnnotations2.mRequiresFlagsOff,
                createRequiresFlagsOff(new String[] {"flag3", "flag4"}));
    }

    @Test
    public void getFlagAnnotations_twoAnnotationsFromMethod() {
        AnnotationsRetriever.FlagAnnotations flagAnnotations =
                getFlagAnnotations(
                        TestClassHasAllAnnotations.class, mRequiresFlagsOn, mRequiresFlagsOff);

        assertEquals(
                flagAnnotations.mRequiresFlagsOn, createRequiresFlagsOn(new String[] {"flag5"}));
        assertEquals(
                flagAnnotations.mRequiresFlagsOff, createRequiresFlagsOff(new String[] {"flag6"}));
    }

    @Test
    public void getFlagAnnotations_twoAnnotationsFromClass() {
        AnnotationsRetriever.FlagAnnotations flagAnnotations =
                getFlagAnnotations(TestClassHasAllAnnotations.class);

        assertEquals(
                flagAnnotations.mRequiresFlagsOn,
                createRequiresFlagsOn(new String[] {"flag1", "flag2"}));
        assertEquals(
                flagAnnotations.mRequiresFlagsOff,
                createRequiresFlagsOff(new String[] {"flag3", "flag4"}));
    }

    @Test
    public void getFlagAnnotations_twoAnnotationsFromMethodAndClass() {
        AnnotationsRetriever.FlagAnnotations flagAnnotations =
                getFlagAnnotations(TestClassHasAllAnnotations.class, mRequiresFlagsOn);

        assertEquals(
                flagAnnotations.mRequiresFlagsOn, createRequiresFlagsOn(new String[] {"flag5"}));
        assertEquals(
                flagAnnotations.mRequiresFlagsOff,
                createRequiresFlagsOff(new String[] {"flag3", "flag4"}));
    }

    private AnnotationsRetriever.FlagAnnotations getFlagAnnotations(
            Class<?> testClass, Annotation... annotations) {
        Description description =
                Description.createTestDescription(testClass, "testMethod", annotations);
        return AnnotationsRetriever.getFlagAnnotations(description);
    }

    @AutoAnnotation
    private static RequiresFlagsOn createRequiresFlagsOn(String[] value) {
        return new AutoAnnotation_AnnotationsRetrieverTest_createRequiresFlagsOn(value);
    }

    @AutoAnnotation
    private static RequiresFlagsOff createRequiresFlagsOff(String[] value) {
        return new AutoAnnotation_AnnotationsRetrieverTest_createRequiresFlagsOff(value);
    }
}
