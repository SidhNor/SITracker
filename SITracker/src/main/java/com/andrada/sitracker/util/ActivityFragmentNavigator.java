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

package com.andrada.sitracker.util;

import android.app.Fragment;
import android.app.FragmentTransaction;

import com.andrada.sitracker.R;
import com.andrada.sitracker.ui.BaseActivity;

public class ActivityFragmentNavigator {

    public static void switchMainFragmentInMainActivity(BaseActivity activity, Fragment fragment) {
        FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_holder, fragment);
        transaction.setCustomAnimations(0, 0);
        transaction.addToBackStack(fragment.getClass().getName());
        transaction.commit();
    }

    public static void switchMainFragmentToChildFragment(BaseActivity activity, Fragment fragment) {
        FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.replace(R.id.fragment_holder, fragment);
        transaction.addToBackStack(fragment.getClass().getName());
        transaction.commit();
    }
}

