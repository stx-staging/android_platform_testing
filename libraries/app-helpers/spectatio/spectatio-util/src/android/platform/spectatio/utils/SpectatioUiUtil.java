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

package android.platform.spectatio.utils;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SpectatioUiUtil {
    private static final String LOG_TAG = SpectatioUiUtil.class.getSimpleName();

    private static SpectatioUiUtil sSpectatioUiUtil = null;

    private static final int SHORT_UI_RESPONSE_WAIT_MS = 1000;
    private static final int LONG_UI_RESPONSE_WAIT_MS = 5000;

    private UiDevice mDevice;

    private enum SwipeDirection {
        TOP_TO_BOTTOM,
        BOTTOM_TO_TOP,
        LEFT_TO_RIGHT,
        RIGHT_TO_LEFT
    }

    private SpectatioUiUtil(UiDevice mDevice) {
        this.mDevice = mDevice;
    }

    public static SpectatioUiUtil getInstance(UiDevice mDevice) {
        if (sSpectatioUiUtil == null) {
            sSpectatioUiUtil = new SpectatioUiUtil(mDevice);
        }
        return sSpectatioUiUtil;
    }

    public void pressHome() {
        mDevice.pressHome();
    }

    public void wakeUp() {
        try {
            mDevice.wakeUp();
        } catch (RemoteException ex) {
            throw new RuntimeException("Device Wake Up Failed.", ex);
        }
    }

    public void waitForIdle() {
        mDevice.waitForIdle();
    }

    public void wait1Second() {
        waitNSeconds(SHORT_UI_RESPONSE_WAIT_MS);
    }

    public void wait5Seconds() {
        waitNSeconds(LONG_UI_RESPONSE_WAIT_MS);
    }

    public void waitNSeconds(int waitTime) {
        SystemClock.sleep(waitTime);
    }

    /**
     * Executes a shell command on device, and return the standard output in string.
     *
     * @param command the command to run
     * @return the standard output of the command, or empty string if failed without throwing an
     *     IOException
     */
    public String executeShellCommand(String command) {
        try {
            return mDevice.executeShellCommand(command);
        } catch (IOException e) {
            // ignore
            Log.e(
                    LOG_TAG,
                    String.format(
                            "The shell command failed to run: %s exception: %s",
                            command, e.getMessage()));
            return "";
        }
    }

    /** Find and return the UI Object that matches the given selector */
    public UiObject2 findUiObject(BySelector selector) {
        if (selector == null) {
            return null;
        }
        UiObject2 uiObject = mDevice.wait(Until.findObject(selector), LONG_UI_RESPONSE_WAIT_MS);
        return uiObject;
    }

    public boolean hasPackageInForeground(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        return mDevice.hasObject(By.pkg(packageName).depth(0));
    }

    public void swipeUp() {
        // Swipe Up From bottom of screen to the top in one step
        swipe(SwipeDirection.BOTTOM_TO_TOP, /*numOfSteps*/ 1);
    }

    public void swipeDown() {
        // Swipe Down From top of screen to the bottom in one step
        swipe(SwipeDirection.TOP_TO_BOTTOM, /*numOfSteps*/ 1);
    }

    private void swipe(SwipeDirection swipeDirection, int numOfSteps) {
        Rect bounds = getScreenBounds();

        List<Point> swipePoints = getPointsToSwipe(bounds, swipeDirection);

        Point startPoint = swipePoints.get(0);
        Point finishPoint = swipePoints.get(1);

        // Swipe from start pont to finish point in given number of steps
        mDevice.swipe(startPoint.x, startPoint.y, finishPoint.x, finishPoint.y, numOfSteps);
    }

    private List<Point> getPointsToSwipe(Rect bounds, SwipeDirection swipeDirection) {
        Point boundsCenter = new Point(bounds.centerX(), bounds.centerY());

        int xStart;
        int yStart;
        int xFinish;
        int yFinish;
        // Set as 5 for default
        // TODO: Make padding value dynamic based on the screen of device under test
        int pad = 5;

        switch (swipeDirection) {
                // Scroll left = swipe from left to right.
            case LEFT_TO_RIGHT:
                xStart = bounds.left + pad; // Pad the edges
                xFinish = bounds.right - pad; // Pad the edges
                yStart = boundsCenter.y;
                yFinish = boundsCenter.y;
                break;
                // Scroll right = swipe from right to left.
            case RIGHT_TO_LEFT:
                xStart = bounds.right - pad; // Pad the edges
                xFinish = bounds.left + pad; // Pad the edges
                yStart = boundsCenter.y;
                yFinish = boundsCenter.y;
                break;
                // Scroll up = swipe from top to bottom.
            case TOP_TO_BOTTOM:
                xStart = boundsCenter.x;
                xFinish = boundsCenter.x;
                yStart = bounds.top + pad; // Pad the edges
                yFinish = bounds.bottom - pad; // Pad the edges
                break;
                // Scroll down = swipe to bottom to top.
            case BOTTOM_TO_TOP:
            default:
                xStart = boundsCenter.x;
                xFinish = boundsCenter.x;
                yStart = bounds.bottom - pad; // Pad the edges
                yFinish = bounds.top + pad; // Pad the edges
                break;
        }

        List<Point> swipePoints = new ArrayList<Point>();
        // Start Point
        swipePoints.add(new Point(xStart, yStart));
        // Finish Point
        swipePoints.add(new Point(xFinish, yFinish));

        return swipePoints;
    }

    private Rect getScreenBounds() {
        Point dimensions = mDevice.getDisplaySizeDp();
        return new Rect(0, 0, dimensions.x, dimensions.y);
    }
}
