package com.andrada.sitracker.fragment.dialog;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.content.DialogInterface.OnShowListener;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import com.andrada.sitracker.R;
import com.andrada.sitracker.task.AddAuthorTask;
import com.andrada.sitracker.task.AddAuthorTask.ITaskCallback;

public class AddAuthorDialog extends DialogFragment implements android.content.DialogInterface.OnClickListener, ITaskCallback {
	EditText mAuthorEditText;
	AlertDialog mDialog;
	OnAuthorAddedListener mAddedListner;

	public interface OnAuthorAddedListener {
		public void onAuthorAdded();
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	public void setOnAuthorAddedListener(OnAuthorAddedListener listener){
		mAddedListner = listener;
	}

    @Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LayoutInflater inflater = (LayoutInflater) getActivity()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.dialog_add_author, null);

        assert layout != null;
        mAuthorEditText = (EditText) layout.findViewById(R.id.et_add_author);
        ClipboardManager clipboard = (ClipboardManager)
                getActivity().getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
        CharSequence clipboardChars = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (clipboard.hasPrimaryClip()) {
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                clipboardChars = item.getText();
            }
        } else {
            clipboardChars = clipboard.getText();
        }
        if (clipboardChars != null && clipboardChars.length() > 0) {
            mAuthorEditText.setText(clipboardChars);
        }

		mAuthorEditText.requestFocus();
		mDialog = new AlertDialog.Builder(getActivity())
				.setTitle(R.string.action_add).setView(layout)
				.setPositiveButton(R.string.action_add, this)
				.setNegativeButton(android.R.string.cancel, this).create();
		mDialog.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

		// Retrieve the "Yes" button and override it to validate the input
		mDialog.setOnShowListener(new OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				Button yes = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);
				yes.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						doPositiveClick();
					}

				});
			}
		});

		return mDialog;
	}
	
	private void doPositiveClick() {
		new AddAuthorTask(getActivity(), this).execute(mAuthorEditText.getText().toString());
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

	@Override
	public void deliverResults() {
		mAddedListner.onAuthorAdded();
		dismiss();
	}

}
