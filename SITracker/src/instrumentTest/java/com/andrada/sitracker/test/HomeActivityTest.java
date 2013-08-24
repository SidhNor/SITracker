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

import android.support.v4.widget.SlidingPaneLayout;
import android.support.v7.app.ActionBar;
import android.widget.ProgressBar;

import com.andrada.sitracker.R;
import com.andrada.sitracker.events.ProgressBarToggleEvent;

import de.greenrobot.event.EventBus;

import static android.test.ViewAsserts.assertOnScreen;
import static org.fest.assertions.api.ANDROID.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created by ggodonoga on 05/08/13.
 */
public class HomeActivityTest extends HomeActivityBaseTestCase {

    private ProgressBar mProgressBar;
    private SlidingPaneLayout slidingPane;
    private String appName;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mProgressBar = (ProgressBar) mMainActivity.findViewById(R.id.globalProgress);
        slidingPane = (SlidingPaneLayout) mMainActivity.findViewById(R.id.fragment_container);

        appName = mMainActivity.getResources().getString(R.string.app_name);
    }

    public void testPreconditions() {
        assertThat(mMainActivity).isNotNull();
        assertThat(mProgressBar).isNotNull();
        assertThat(slidingPane).isNotNull();
        assertThat(mActionBar).isNotNull();
        assertThat(appName).isIn("SI Tracker", "СИ Трекер");
        assertOnScreen(mMainActivity.getWindow().getDecorView(), slidingPane);
        assertOnScreen(mMainActivity.getWindow().getDecorView(), mProgressBar);
    }

    public void testProgressBarVisibilityGone() {
        assertThat(mProgressBar).isGone();
    }


    public void testProgressBarRespondsToOnProgressBarToggleEvents() {

        EventBus.getDefault().post(new ProgressBarToggleEvent(true));
        getInstrumentation().waitForIdleSync();

        assertThat(mProgressBar).isVisible().isIndeterminate();

        EventBus.getDefault().post(new ProgressBarToggleEvent(false));

        getInstrumentation().waitForIdleSync();

        assertThat(mProgressBar).isGone();
    }

    public void testSlidingPaneIsInitiallyOpened() {
        assertThat(slidingPane.isOpen()).isTrue();
    }

    public void testActionBarHasAppTitle() {
        assertThat(mActionBar.getTitle()).isIn("SI Tracker", "СИ Трекер");
    }

    public void testActionBarHasDisabledHome() {
        assertThat(mActionBar.getDisplayOptions())
                .isNotEqualTo(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME);
    }

}
