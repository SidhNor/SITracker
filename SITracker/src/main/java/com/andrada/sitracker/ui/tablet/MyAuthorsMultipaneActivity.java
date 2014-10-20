package com.andrada.sitracker.ui.tablet;

import android.os.Bundle;

import com.andrada.sitracker.R;
import com.andrada.sitracker.ui.BaseActivity;

import org.androidannotations.annotations.EActivity;

@EActivity(R.layout.activity_authors_wide)
public class MyAuthorsMultipaneActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Ignore animations
        overridePendingTransition(0, 0);
    }

    @Override
    protected int getSelfNavDrawerItem() {
        // we only have a nav drawer if we are in top-level Explore mode.
        return NAVDRAWER_ITEM_MY_AUTHORS;
    }
}
