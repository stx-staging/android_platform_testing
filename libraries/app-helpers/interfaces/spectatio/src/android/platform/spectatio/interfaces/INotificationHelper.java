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

package android.platform.spectatio.interfaces;

import android.platform.helpers.IAppHelper;

public interface INotificationHelper extends IAppHelper {
    void openNotificationTray();

    void closeNotificationTray();

    boolean hasNotificationWithTitle(String title);

    /**
     * Dismiss the notification with given title.
     *
     * <p>Precede calls to this method with {@code hasNotificationWithTitle} in order to verify that
     * the notification exists. If the notification doesn't exist, this method throws an exception.
     *
     * @param title : Title of the notification to be dismissed
     */
    void dismissNotificationWithTitle(String title);

    void clearAllNotifications();
}
