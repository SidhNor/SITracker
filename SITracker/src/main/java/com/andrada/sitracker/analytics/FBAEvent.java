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

package com.andrada.sitracker.analytics;

import java.util.HashMap;
import java.util.Map;

public class FBAEvent {

    protected String name;
    private final Map<String, String> paramMap;

    public FBAEvent() {
        this(new HashMap<String, String>());
    }

    public FBAEvent(String eventName) {
        this(new HashMap<String, String>());
        name = eventName;
    }

    public FBAEvent(Map<String, String> paramMap) {
        this.paramMap = paramMap;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getParamMap() {
        return paramMap;
    }

}
