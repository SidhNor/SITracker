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

package com.andrada.sitracker.ui.phone;

import android.app.Fragment;
import android.os.Bundle;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.tasks.ExportAuthorsTask;
import com.andrada.sitracker.ui.BaseActivity;
import com.andrada.sitracker.ui.fragment.AuthorsFragment;
import com.andrada.sitracker.ui.fragment.AuthorsFragment_;
import com.andrada.sitracker.ui.fragment.DirectoryChooserFragment;
import com.andrada.sitracker.ui.widget.DrawShadowFrameLayout;
import com.andrada.sitracker.util.AnalyticsHelper;
import com.andrada.sitracker.util.UIUtils;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;

@EActivity(R.layout.activity_generic_list)
@OptionsMenu(R.menu.main_menu)
public class MyAuthorsActivity extends BaseActivity implements
        DirectoryChooserFragment.OnFragmentInteractionListener {

    DirectoryChooserFragment mDialog;

    @ViewById(R.id.main_content)
    DrawShadowFrameLayout mDrawShadowFrameLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDialog = DirectoryChooserFragment.newInstance(getResources().getString(R.string.export_folder_name), null, true);
        getFragmentManager().beginTransaction()
                .add(R.id.fragment_holder, AuthorsFragment_.builder().build(), "myAuthors")
                .commit();
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        AuthorsFragment frag = (AuthorsFragment) getFragmentManager().findFragmentByTag("myAuthors");
        enableActionBarAutoHide(frag.getListView());
        registerHideableHeaderView(findViewById(R.id.headerbar));

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

    @OptionsItem(R.id.action_import)
    void menuImportSelected() {
        startActivity(com.andrada.sitracker.ui.ImportAuthorsActivity_.intent(this).get());
    }

    @OptionsItem(R.id.action_export)
    void menuExportSelected() {
        AnalyticsHelper.getInstance().sendView(Constants.GA_SCREEN_EXPORT_DIALOG);
        mDialog.show(getFragmentManager(), null);
    }


    @Override
    public void onSelectDirectory(String path) {
        ExportAuthorsTask task = new ExportAuthorsTask(getApplicationContext());
        task.execute(path);
        mDialog.dismiss();
    }

    @Override
    public void onCancelChooser() {
        mDialog.dismiss();
    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();
        setTopClearance();
    }

    private void setTopClearance() {
        Fragment frag = getFragmentManager().findFragmentByTag("myAuthors");
        if (frag != null) {
            // configure fragment's top clearance to take our overlaid controls (Action Bar) into account.
            int actionBarSize = UIUtils.calculateActionBarSize(this);
            mDrawShadowFrameLayout.setShadowTopOffset(actionBarSize);
            ((AuthorsFragment) frag).setContentTopClearance(actionBarSize);
            setProgressBarTopWhenActionBarShown(actionBarSize);
        }
    }
}
