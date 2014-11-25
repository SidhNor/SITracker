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

package com.andrada.sitracker.ui.fragment;

import android.support.v7.widget.RecyclerView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.util.NavDrawerManager;

import org.androidannotations.annotations.EFragment;

@EFragment(R.layout.fragment_explore_authors)
public class ExploreAuthorsFragment extends BaseListFragment
        implements NavDrawerManager.NavDrawerItemAware {


    @Override
    public void onResume() {
        super.onResume();
        getBaseActivity().getDrawerManager().pushNavigationalState(getString(R.string.navdrawer_item_explore), true);
    }

    @Override
    public int getSelfNavDrawerItem() {
        return NavDrawerManager.NAVDRAWER_ITEM_EXPLORE;
    }

    @Override
    public void setContentTopClearance(int top) {

    }

    @Override
    public RecyclerView getRecyclerView() {
        return null;
    }
}
