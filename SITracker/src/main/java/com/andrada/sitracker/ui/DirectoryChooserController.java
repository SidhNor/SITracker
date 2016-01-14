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

import com.andrada.sitracker.ui.fragment.DirectoryChooserFragment;

public class DirectoryChooserController implements DirectoryChooserFragment.OnFragmentInteractionListener {

    public static final int RESULT_CODE_DIR_SELECTED = 1;

    public static final String TAG = "DirectoryChooserFrag";

    private DirectoryChooserFragment mDialog;
    private Activity mActivity;
    private DirectoryChooserResultListener mListener;

    public DirectoryChooserController(Activity activity, String initialDir, boolean isDirectoryChooser) {
        this.mActivity = activity;
        mDialog = (DirectoryChooserFragment) mActivity.getFragmentManager().findFragmentByTag(TAG);
        if (mDialog == null) {
            mDialog = DirectoryChooserFragment
                    .newInstance("", initialDir, isDirectoryChooser, this);
        }
        mDialog.setListener(this);
    }

    public void setListener(DirectoryChooserResultListener listener) {
        this.mListener = listener;
    }

    public void showDialog(DirectoryChooserResultListener listener) {
        this.mListener = listener;
        if (!this.mActivity.isFinishing()) {
            mDialog.show(mActivity.getFragmentManager(), TAG);
        }
    }


    @Override
    public void onSelectDirectory(String path) {
        mDialog.dismiss();
        if (mListener != null) {
            mListener.onDirectoryChooserResult(RESULT_CODE_DIR_SELECTED, path);
        }
    }

    @Override
    public void onCancelChooser() {
        mDialog.dismiss();
        if (mListener != null) {
            mListener.onDirectoryChooserResult(Activity.RESULT_CANCELED, null);
        }
    }


    public interface DirectoryChooserResultListener {
        void onDirectoryChooserResult(int resultCode, String dirOrFileChosen);
    }

}
