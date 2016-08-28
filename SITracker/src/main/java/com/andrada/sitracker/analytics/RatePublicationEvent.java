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

import com.andrada.sitracker.Constants;
import com.google.firebase.analytics.FirebaseAnalytics;

public class RatePublicationEvent extends FBAEvent {

    public RatePublicationEvent(String publicationUrl, int stars) {
        super(Constants.GA_EVENT_PUB_RATED);
        getParamMap().put(FirebaseAnalytics.Param.ITEM_CATEGORY, "publication");
        getParamMap().put(FirebaseAnalytics.Param.ITEM_ID, publicationUrl);
        getParamMap().put(FirebaseAnalytics.Param.VALUE, String.valueOf(stars));
    }
}
