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

import android.Manifest;

import com.andrada.sitracker.R;

public enum Permissions {

    READ_PERMISSION(Manifest.permission.READ_EXTERNAL_STORAGE, 102, R.string.read_storage_permissions),
    WRITE_PERMISSION(Manifest.permission.WRITE_EXTERNAL_STORAGE, 103, R.string.write_storage_permissions);

    public final String permissionCode;
    public final int requestCode;
    public final int explanationMessageResourceId;

    Permissions(String permissionCode, int requestCode, int explanationMessageResourceId) {
        this.permissionCode = permissionCode;
        this.requestCode = requestCode;
        this.explanationMessageResourceId = explanationMessageResourceId;

    }
}