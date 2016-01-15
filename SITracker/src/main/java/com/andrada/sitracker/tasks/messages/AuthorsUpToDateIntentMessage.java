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

package com.andrada.sitracker.tasks.messages;

import org.jetbrains.annotations.NotNull;

public class AuthorsUpToDateIntentMessage extends BaseIntentMessage {

    public static final String UP_TO_DATE_MESSAGE = "com.andrada.sitracker.UP_TO_DATE_ACTION";

    public AuthorsUpToDateIntentMessage() {
        super();
        this.setAction(UP_TO_DATE_MESSAGE);
    }

    @NotNull
    public static String getMessageName() {
        return UP_TO_DATE_MESSAGE;
    }

}
