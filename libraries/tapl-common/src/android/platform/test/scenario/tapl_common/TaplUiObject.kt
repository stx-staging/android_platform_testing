/** Copyright 2022 Google Inc. All Rights Reserved. */
package android.platform.test.scenario.tapl_common

import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiObject2Condition
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue

/**
 * Ui object with diagnostic metadata and flake-free gestures.
 * @param [uiObject] UI Automator object
 * @param [name] Name of the object for diags
 */
class TaplUiObject constructor(val uiObject: UiObject2, private val name: String) {
    private fun waitForObjectCondition(
        condition: UiObject2Condition<Boolean>,
        conditionName: String
    ) {
        assertTrue(
            "UI object '$name' is not $conditionName.",
            uiObject.wait(condition, ObjectFactory.WAIT_TIME.toMillis())
        )
    }

    private fun waitForObjectEnabled() {
        waitForObjectCondition(Until.enabled(true), "enabled")
    }

    private fun waitForObjectClickable() {
        waitForObjectCondition(Until.clickable(true), "clickable")
    }

    /** Wait for the object to become clickable and enabled, then clicks the object. */
    fun click() {
        try {
            waitForObjectEnabled()
            waitForObjectClickable()
            uiObject.click()
        } catch (e: StaleObjectException) {
            throw AssertionError(
                "UI object '$name' has disappeared from the screen during the click gesture.",
                e
            )
        }
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
            uiObject.wait(Until.findObject(selector), ObjectFactory.WAIT_TIME.toMillis())
                ?: throw AssertionError(
                    "UI object '$childObjectName' is not found in '$name'; selector: $selector."
                )
        return TaplUiObject(childObject, childObjectName)
    }
}
