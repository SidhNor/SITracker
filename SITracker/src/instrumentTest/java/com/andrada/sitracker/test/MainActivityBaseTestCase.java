package com.andrada.sitracker.test;

import android.test.ActivityInstrumentationTestCase2;

import com.andrada.sitracker.MainActivity_;

/**
 * Created by ggodonoga on 06/08/13.
 */
public class MainActivityBaseTestCase extends ActivityInstrumentationTestCase2<MainActivity_> {

    protected MainActivity_ mMainActivity;

    public MainActivityBaseTestCase() {
        super(MainActivity_.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mMainActivity = getActivity();
    }
}