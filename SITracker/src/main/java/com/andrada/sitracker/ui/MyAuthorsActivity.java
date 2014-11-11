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

package com.andrada.sitracker.ui;

import android.os.Bundle;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.tasks.ExportAuthorsTask;
import com.andrada.sitracker.ui.fragment.AuthorsFragment;
import com.andrada.sitracker.ui.fragment.AuthorsFragment_;
import com.andrada.sitracker.ui.fragment.DirectoryChooserFragment;
import com.andrada.sitracker.util.AnalyticsHelper;
import com.andrada.sitracker.util.NavDrawerManager;
import com.andrada.sitracker.util.UIUtils;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;

@EActivity(R.layout.activity_si_main)
@OptionsMenu(R.menu.main_menu)
public class MyAuthorsActivity extends BaseActivity implements
        DirectoryChooserFragment.OnFragmentInteractionListener {

    DirectoryChooserFragment mDialog;

    AuthorsFragment authorsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDialog = DirectoryChooserFragment.newInstance(getResources().getString(R.string.export_folder_name), null, true);
        authorsFragment = AuthorsFragment_.builder().build();
        getFragmentManager().beginTransaction()
                .add(R.id.fragment_holder, authorsFragment, "myAuthors")
                .commit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        AuthorsFragment frag = (AuthorsFragment) getFragmentManager().findFragmentByTag("myAuthors");
        getActionBarUtil().enableActionBarAutoHide(frag.getListView());
        getActionBarUtil().registerHideableHeaderView(findViewById(R.id.headerbar));

    }

    @Override
    public int getSelfNavDrawerItem() {
        // we only have a nav drawer if we are in top-level Explore mode.
        return NavDrawerManager.NAVDRAWER_ITEM_MY_AUTHORS;
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
        if (authorsFragment != null) {
            // configure fragment's top clearance to take our overlaid controls (Action Bar) into account.
            int actionBarSize = UIUtils.calculateActionBarSize(this);
            authorsFragment.setContentTopClearance(actionBarSize);
            setProgressBarTopWhenActionBarShown(actionBarSize);
        }
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        if (authorsFragment != null) {
            return authorsFragment.canCollectionViewScrollUp();
        }
        return super.canSwipeRefreshChildScrollUp();
    }
}
