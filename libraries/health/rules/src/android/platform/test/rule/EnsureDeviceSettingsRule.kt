package android.platform.test.rule

import android.os.SystemProperties
import android.provider.Settings
import org.junit.runner.Description

/**
 * Making sure "Stay awake" setting from Developer settings is set so the screen doesn't turn off
 * while tests are running and that "Test Harness" mode is on.
 *
 * Setting value of "Stay awake" is bit-based with 4 bits responsible for different types of
 * charging. So the value is device-dependent but non-zero value means the settings is on.
 * See [Settings.Global.STAY_ON_WHILE_PLUGGED_IN] for more information.
 */
class EnsureDeviceSettingsRule : TestWatcher() {

    override fun starting(description: Description?) {
        val mobileHarnessModeEnabled = SystemProperties.getBoolean(TEST_HARNESS_PROP, false)
        if (!mobileHarnessModeEnabled) {
            throw AssertionError("'Test harness' mode is required but disabled. " +
                    "To enable it run `adb shell setprop $TEST_HARNESS_PROP 1`. " +
                    "It is also recommended to restart Nexus Launcher after doing this using " +
                    "`adb shell am force-stop $LAUNCHER_PACKAGE`")
        }

        val stayAwakeResult =
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.STAY_ON_WHILE_PLUGGED_IN
            )
        if (stayAwakeResult == 0) {
            throw AssertionError("'Stay awake' option in developer settings should be enabled")
        }
    }

    private companion object {
        const val TEST_HARNESS_PROP = "ro.test_harness"
        const val LAUNCHER_PACKAGE = "com.google.android.apps.nexuslauncher"

        const val TAG = "EnsureDeviceSettingsRule"
    }
}
