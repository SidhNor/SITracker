/*
 * Copyright 2013 Gleb Godonoga.
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

package com.andrada.sitracker.tasks.messages;

public class UpdateFailedIntentMessage extends BaseIntentMessage {

    public static final String FAILED_MESSAGE = "com.andrada.sitracker.UPDATE_FAILED_ACTION";

    public UpdateFailedIntentMessage() {
        super();
        this.setAction(FAILED_MESSAGE);
    }

    public static String getMessageName() {
        return FAILED_MESSAGE;
    }

}
