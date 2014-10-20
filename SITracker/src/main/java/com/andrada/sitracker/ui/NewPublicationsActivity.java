package com.andrada.sitracker.ui;

import android.app.Activity;
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
