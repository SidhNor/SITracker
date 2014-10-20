package com.andrada.sitracker.ui;

import android.os.Bundle;

import com.andrada.sitracker.R;

import org.androidannotations.annotations.EActivity;

@EActivity(R.layout.activity_explore)
public class ExploreAuthorsActivity extends BaseActivity {

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_EXPLORE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Ignore animations
        overridePendingTransition(0, 0);
    }
}
