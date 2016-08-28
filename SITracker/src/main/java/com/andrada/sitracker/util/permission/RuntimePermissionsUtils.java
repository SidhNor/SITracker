/*
 * Copyright 2016 Gleb Godonoga.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrada.sitracker.util.permission;

import android.content.pm.PackageManager;
import android.support.v13.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

public class RuntimePermissionsUtils {

    public RuntimePermissionsUtils() {
    }

    /**
     * Returns a boolean that describes the status of permission provided
     * within the {@code permission}.
     *
     * @param permission the permission name to be checked and asked for.
     * @return <b>true</b> if permission request started, <b>false</b> otherwise.
     */
    public boolean requestPermissionIfNeed(Permissions permission, RuntimePermissionsInteraction runtimePermissionsInteraction) {
        if (isPermissionGranted(permission, runtimePermissionsInteraction)) {
            return false;
        }

        if (shouldRequestPermissionRationale(permission, runtimePermissionsInteraction)) {
            runtimePermissionsInteraction.showExplanationDialog(permission);
        } else {
            requestPermission(permission, runtimePermissionsInteraction);
        }
        return true;
    }

    public boolean shouldRequestPermissionRationale(Permissions permission, RuntimePermissionsInteraction runtimePermissionsInteraction) {
        return ActivityCompat.shouldShowRequestPermissionRationale(runtimePermissionsInteraction.getActivity(), permission.permissionCode);
    }

    public void requestPermission(Permissions permission, RuntimePermissionsInteraction runtimePermissionsInteraction) {
        ActivityCompat.requestPermissions(runtimePermissionsInteraction.getActivity(), new String[]{permission.permissionCode}, permission.requestCode);
    }

    public boolean isPermissionGranted(Permissions permission, RuntimePermissionsInteraction runtimePermissionsInteraction) {
        return ContextCompat.checkSelfPermission(runtimePermissionsInteraction.getActivity(), permission.permissionCode) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean handleOnRequestPermissionsResult(int requestCode, String[] resultCode, int[] data, RuntimePermissionsInteraction runtimePermissionsInteraction) {
        if (requestCode == Permissions.WRITE_PERMISSION.requestCode) {
            if (data.length > 0 && data[0] == PackageManager.PERMISSION_GRANTED) {
                runtimePermissionsInteraction.permissionGranted();
            } else {
                runtimePermissionsInteraction.permissionRevoked();
            }
        }
        return false;
    }

}