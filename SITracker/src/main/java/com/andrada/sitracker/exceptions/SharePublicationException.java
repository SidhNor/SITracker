/*
 * Copyright 2014 Gleb Godonoga.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrada.sitracker.exceptions;

public class SharePublicationException extends Exception {
    private static final long serialVersionUID = 1926675132307831316L;

    private SharePublicationErrors mError = SharePublicationErrors.ERROR_UNKOWN;

    public enum SharePublicationErrors {
        COULD_NOT_PERSIST,
        STORAGE_NOT_ACCESSIBLE_FOR_PERSISTANCE,
        COULD_NOT_LOAD,
        ERROR_UNKOWN
    }

    public SharePublicationException(SharePublicationErrors error) {
        super();
        mError = error;
    }

    public SharePublicationErrors getError() {
        return mError;
    }

    public SharePublicationException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
