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

import com.google.firebase.analytics.FirebaseAnalytics;

public class SearchAuthorEvent extends FBAEvent {

    public SearchAuthorEvent(String query, String type) {
        super(FirebaseAnalytics.Event.SEARCH);
        getParamMap().put(FirebaseAnalytics.Param.SEARCH_TERM, query);
        getParamMap().put("search_type", type);
    }
}
