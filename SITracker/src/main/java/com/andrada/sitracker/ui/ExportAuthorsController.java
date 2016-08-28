/*
 * Copyright 2016 Gleb Godonoga.
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

import android.app.Activity;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.analytics.ExportAuthorsEvent;
import com.andrada.sitracker.analytics.FBAEvent;
import com.andrada.sitracker.tasks.ExportAuthorsTask;
import com.andrada.sitracker.ui.fragment.DirectoryChooserFragment;
import com.andrada.sitracker.analytics.AnalyticsManager;

public class ExportAuthorsController implements DirectoryChooserFragment.OnFragmentInteractionListener {

    private DirectoryChooserFragment mDialog;
    private Activity mActivity;

    public ExportAuthorsController(Activity context) {
        this.mActivity = context;
        mDialog = DirectoryChooserFragment
                .newInstance(null, true, this);
    }


    public void showDialog() {
        if (!this.mActivity.isFinishing()) {
            mDialog.show(mActivity.getFragmentManager(), null);
        }
    }

    @Override
    public void onSelectDirectory(String path) {
        ExportAuthorsTask task = new ExportAuthorsTask(mActivity.getApplicationContext());
        AnalyticsManager.getInstance().logEvent(new ExportAuthorsEvent());
        task.execute(path);
        mDialog.dismiss();
    }

    @Override
    public void onCancelChooser() {
        mDialog.dismiss();
    }
}
