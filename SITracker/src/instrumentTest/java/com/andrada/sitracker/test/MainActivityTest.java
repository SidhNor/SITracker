package com.andrada.sitracker.test;

import android.support.v4.widget.SlidingPaneLayout;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.ProgressBar;

import com.andrada.sitracker.MainActivity_;
import com.andrada.sitracker.R;
import com.andrada.sitracker.events.ProgressBarToggleEvent;

import de.greenrobot.event.EventBus;

import static android.test.ViewAsserts.assertOnScreen;

/**
 * Created by ggodonoga on 05/08/13.
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity_> {

    private MainActivity_ mMainActivity;
    private ProgressBar mProgressBar;

    SlidingPaneLayout slidingPane;

    public MainActivityTest() {
        super(MainActivity_.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mMainActivity = getActivity();
        mProgressBar = (ProgressBar) mMainActivity.findViewById(R.id.globalProgress);
        slidingPane = (SlidingPaneLayout) mMainActivity.findViewById(R.id.fragment_container);
    }

    // Methods whose names are prefixed with test will automatically be run
    public void testProgressBarPresent() {
        assertOnScreen(mMainActivity.getWindow().getDecorView(), mProgressBar);
    }


    public void testProgressBarVisibilityGone() {
        assertTrue(mProgressBar.getVisibility() == View.GONE);
    }


    public void testProgressBarRespondsToOnProgressBarToggleEvents() {

        EventBus.getDefault().post(new ProgressBarToggleEvent(true));
        getInstrumentation().waitForIdleSync();

        assertTrue(mProgressBar.getVisibility() == View.VISIBLE);

        EventBus.getDefault().post(new ProgressBarToggleEvent(false));

        getInstrumentation().waitForIdleSync();

        assertTrue(mProgressBar.getVisibility() == View.GONE);
    }


    public void testSlidingPaneIsUp() {
        assertOnScreen(mMainActivity.getWindow().getDecorView(), slidingPane);
    }

    public void testSlidingPaneIsInitiallyOpened() {
        assertTrue(slidingPane.isOpen());
    }
}
