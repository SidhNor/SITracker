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

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.andrada.sitracker.R;
import com.andrada.sitracker.events.AuthorSelectedEvent;
import com.andrada.sitracker.ui.fragment.AuthorsFragment;
import com.andrada.sitracker.ui.fragment.AuthorsFragment_;
import com.andrada.sitracker.ui.fragment.PublicationsFragment_;
import com.andrada.sitracker.util.ActivityFragmentNavigator;
import com.andrada.sitracker.util.UIUtils;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsMenu;

import de.greenrobot.event.EventBus;

@EActivity(R.layout.activity_si_main)
@OptionsMenu(R.menu.main_menu)
public class SiMainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int priority = 1;
        EventBus.getDefault().register(this, priority);

        //Bootstrap app with initial fragment
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        AuthorsFragment authFrag = AuthorsFragment_.builder().build();
        mCurrentNavigationElement = authFrag;
        transaction.replace(R.id.fragment_holder, authFrag);
        transaction.setCustomAnimations(0, 0);
        transaction.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void onEvent(AuthorSelectedEvent event) {
        //If we received this event here, that means that nobody handle it - switch fragment then
        Fragment frag = PublicationsFragment_.builder().activeAuthorId(event.authorId).authorName(event.authorName).build();
        //getActionBarUtil().autoShowOrHideActionBar(true);
        ActivityFragmentNavigator.switchMainFragmentToChildFragment(this, frag);
    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();
        setContentTopClearance();
    }

    public void setContentTopClearance() {
        if (mCurrentNavigationElement != null) {
            // configure fragment's top clearance to take our overlaid controls (Action Bar) into account.
            int actionBarSize = UIUtils.calculateActionBarSize(this);
            setContentTopClearance(actionBarSize);
            mCurrentNavigationElement.setContentTopClearance(actionBarSize);
            setProgressBarTopWhenActionBarShown(actionBarSize);
        }
    }
}
