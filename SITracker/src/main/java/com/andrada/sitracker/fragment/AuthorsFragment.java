package com.andrada.sitracker.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.andrada.sitracker.R;
import com.andrada.sitracker.fragment.adapters.AuthorsAdapter;
import com.andrada.sitracker.task.AddAuthorTask;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.InstanceState;

@EFragment
public class AuthorsFragment extends ListFragment implements AddAuthorTask.IAuthorTaskCallback {

	OnAuthorSelectedListener mCallback;

    @Bean
    AuthorsAdapter adapter;

    @InstanceState
    Boolean isInTwoPane = false;

    public interface OnAuthorSelectedListener {
		public void onAuthorSelected(long id);
        public void onAuthorAdded();
	}

    public void setInTwoPane(Boolean inTwoPane) {
        isInTwoPane = inTwoPane;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

	@Override
	public void onStart() {
		super.onStart();
        if (isInTwoPane) {
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            getListView().setSelector(R.drawable.authors_list_selector);
        }
        getListView().setBackgroundResource(R.drawable.authors_list_background);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			// This makes sure that the container activity has implemented
			// the callback interface. If not, it throws an exception.
			mCallback = (OnAuthorSelectedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnAuthorSelectedListener");
		}
	}

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }

    @AfterViews
    void bindAdapter() {
        setListAdapter(adapter);
    }

	public void updateView() {
		adapter.reloadAuthors();
	}

	public void updateViewAtPosition(int position) {
		updateView();
		getListView().setItemChecked(position, true);
		getListView().setSelection(position);
	}

    public void tryAddAuthor(String url) {
        new AddAuthorTask((Context)mCallback, this).execute(url);
    }

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// Notify the parent activity of selected item
		mCallback.onAuthorSelected(id);
		// Set the item as checked to be highlighted when in two-pane layout
		getListView().setItemChecked(position, true);
	}

    @Override
    public void deliverResults(String message) {
        //Stop progress bar
        if (message.length() == 0) {
            //This is success
            mCallback.onAuthorAdded();
        } else {
            Toast.makeText((Context)mCallback, message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void operationStart() {
        //Start progress bar
    }


}
