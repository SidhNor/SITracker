/*
 * Copyright 2013 Gleb Godonoga.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrada.sitracker.ui.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.andrada.sitracker.R;
import com.andrada.sitracker.events.ProgressBarToggleEvent;
import com.andrada.sitracker.tasks.AddAuthorTask;
import com.andrada.sitracker.util.ClipboardHelper;

import de.greenrobot.event.EventBus;

public class AddAuthorDialog extends SherlockDialogFragment implements
        android.content.DialogInterface.OnClickListener {

    EditText mAuthorEditText;
    private AlertDialog mDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = (LayoutInflater) getActivity()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_add_author, null);

        assert layout != null;
        mAuthorEditText = (EditText) layout.findViewById(R.id.et_add_author);
        CharSequence clipboardChars = ClipboardHelper.getClipboardText(getActivity().getApplicationContext());

        if (clipboardChars != null && clipboardChars.length() > 0) {
            mAuthorEditText.setText(clipboardChars);
        }

        mAuthorEditText.requestFocus();
        mDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.action_add).setView(layout)
                .setPositiveButton(R.string.action_add_ok, this)
                .setNegativeButton(android.R.string.cancel, this).create();
        mDialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        // Retrieve the "Yes" button and override it to validate the input
        mDialog.setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button yes = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if (yes != null) {
                    yes.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            doPositiveClick();
                            mDialog.dismiss();
                        }
                    });
                }
            }
        });

        return mDialog;
    }

    private void doPositiveClick() {
        EventBus.getDefault().post(new ProgressBarToggleEvent(true));
        new AddAuthorTask(getSherlockActivity()).execute(mAuthorEditText.getText().toString());
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == Dialog.BUTTON_NEGATIVE) {
            dialog.dismiss();
        }
    }
}
