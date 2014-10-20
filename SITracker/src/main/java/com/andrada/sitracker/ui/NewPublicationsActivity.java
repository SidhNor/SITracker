package com.andrada.sitracker.ui;

import android.app.Activity;

import com.andrada.sitracker.R;

import org.androidannotations.annotations.EActivity;

@EActivity(R.layout.activity_new_pubs)
public class NewPublicationsActivity extends BaseActivity {
    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_NEW_PUBS;
    }
}
