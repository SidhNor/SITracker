/*
 * Copyright 2014 Gleb Godonoga.
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

package com.andrada.sitracker.ui;

import android.os.Bundle;

import com.andrada.sitracker.R;

import org.androidannotations.annotations.EActivity;

@EActivity(R.layout.activity_new_pubs)
public class NewPublicationsActivity extends BaseActivity {
    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_NEW_PUBS;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Ignore animations
        overridePendingTransition(0, 0);
    }
}
