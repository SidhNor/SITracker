package com.andrada.sitracker.ui.phone;

import android.app.ActionBar;
import android.os.Bundle;

import com.andrada.sitracker.R;
import com.andrada.sitracker.ui.BaseActivity;
import com.andrada.sitracker.ui.widget.DrawShadowFrameLayout;

public class MyAuthorsActivity extends BaseActivity {

    private DrawShadowFrameLayout mDrawShadowFrameLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_authors_narrow);

        overridePendingTransition(0, 0);

        mDrawShadowFrameLayout = (DrawShadowFrameLayout) findViewById(R.id.main_content);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        ActionBar ab = getActionBar();
        // unless Nav Drawer opens
        ab.setTitle("Authors");
        ab.setHomeButtonEnabled(true);
        ab.setDisplayHomeAsUpEnabled(false);
    }

    @Override
    protected void onActionBarAutoShowOrHide(boolean shown) {
        super.onActionBarAutoShowOrHide(shown);
        mDrawShadowFrameLayout.setShadowVisible(shown, shown);
    }

    @Override
    protected int getSelfNavDrawerItem() {
        // we only have a nav drawer if we are in top-level Explore mode.
        return NAVDRAWER_ITEM_MY_AUTHORS;
    }
}
