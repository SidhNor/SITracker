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

package com.andrada.sitracker.test;

import android.support.v7.app.ActionBar;
import android.test.ActivityInstrumentationTestCase2;

import com.andrada.sitracker.ui.HomeActivity_;

public class HomeActivityBaseTestCase extends ActivityInstrumentationTestCase2<HomeActivity_> {

    protected HomeActivity_ mMainActivity;
    protected ActionBar mActionBar;


    public HomeActivityBaseTestCase() {
        super(HomeActivity_.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mMainActivity = getActivity();
        mActionBar = mMainActivity.getSupportActionBar();
    }
}
