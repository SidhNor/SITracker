package com.andrada.sitracker.ui;

import com.andrada.sitracker.R;

import org.androidannotations.annotations.EActivity;

@EActivity(R.layout.activity_explore)
public class ExploreAuthorsActivity extends BaseActivity {
    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_EXPLORE;
    }
}
