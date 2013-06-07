package com.andrada.sitracker.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.AuthorUpdateProgressListener;
import com.andrada.sitracker.fragment.adapters.AuthorsAdapter;
import com.andrada.sitracker.fragment.dialog.AddAuthorDialog;
import com.andrada.sitracker.task.AddAuthorTask;
import com.andrada.sitracker.task.UpdateAuthorsIntentService_;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;

@EFragment(R.layout.fragmet_authors)
@OptionsMenu(R.menu.authors_menu)
public class AuthorsFragment extends SherlockFragment implements AddAuthorTask.IAuthorTaskCallback,
        AuthorUpdateProgressListener, AddAuthorDialog.OnAuthorLinkSuppliedListener {

    public interface OnAuthorSelectedListener {
        public void onAuthorSelected(long id);
    }

	OnAuthorSelectedListener mCallback;

    @ViewById
    ListView list;

    @ViewById
    View refreshProgressBar;

    @Bean
    AuthorsAdapter adapter;

    @InstanceState
    Boolean isInTwoPane = false;

    private boolean mIsUpdating = false;

    //region Fragment lifecycle overrides

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

	@Override
	public void onStart() {
		super.onStart();
        if (isInTwoPane) {
            list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }
        list.setSelector(R.drawable.authors_list_selector);
        list.setBackgroundResource(R.drawable.authors_list_background);

        getSherlockActivity().invalidateOptionsMenu();
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

    //endregion

    //region Menu item tap handlers
    @OptionsItem(R.id.action_add)
    void menuAddSelected() {
        AddAuthorDialog authorDialog = new AddAuthorDialog();
        authorDialog.setOnAuthorLinkSuppliedListener(this);
        authorDialog.show(getActivity().getSupportFragmentManager(),
                Constants.DIALOG_ADD_AUTHOR);
    }

    @OptionsItem(R.id.action_refresh)
    void menuRefreshSelected() {
        UpdateAuthorsIntentService_.intent(getActivity()).start();
        toggleUpdatingState();
    }
    //endregion

    @AfterViews
    void bindAdapter() {
        list.setAdapter(adapter);
    }

    protected void updateAuthors() {
        int tempPosition = list.getSelectedItemPosition();
        adapter.reloadAuthors();
        list.setItemChecked(tempPosition, true);
        list.setSelection(tempPosition);
	}

    protected void tryAddAuthor(String url) {
        new AddAuthorTask((Context)mCallback, this).execute(url);
    }

	@ItemClick
	public void listItemClicked(int position) {
		// Notify the parent activity of selected item
        Object obj = list.getItemAtPosition(position);
        long id = list.getItemIdAtPosition(position);
		mCallback.onAuthorSelected(id);

		// Set the item as checked to be highlighted when in two-pane layout
        list.setItemChecked(position, true);
	}

    private void toggleUpdatingState() {

        ActionBar bar = ((SherlockFragmentActivity)getActivity()).getSupportActionBar();
        bar.setDisplayShowHomeEnabled(mIsUpdating);
        bar.setDisplayShowTitleEnabled(mIsUpdating);
        bar.setDisplayShowCustomEnabled(!mIsUpdating);

        if (mIsUpdating) {
            refreshProgressBar.setVisibility(View.GONE);
        } else {
            View mLogoView = LayoutInflater.from(getActivity()).inflate(R.layout.updating_actionbar_layout, null);
            bar.setCustomView(mLogoView, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            refreshProgressBar.setVisibility(View.VISIBLE);
        }

        mIsUpdating = !mIsUpdating;
        getSherlockActivity().invalidateOptionsMenu();
    }

    //region Public methods
    public boolean isUpdating() {
        return mIsUpdating;
    }


    public void setInTwoPane(Boolean inTwoPane) {
        isInTwoPane = inTwoPane;
    }

    //endregion


    //region AddAuthorTask.IAuthorTaskCallback callbacks
    @Override
    public void onAuthorAddStarted() {
        //Add a temporary item to authors
        //Start progress bar
    }

    @Override
    public void onAuthorAddCompleted(String message) {
        //Stop progress bar
        if (message.length() == 0) {
            //This is success
            updateAuthors();
        } else {
            Toast.makeText((Context)mCallback, message, Toast.LENGTH_SHORT).show();
        }
    }

    //endregion


    //region AuthorUpdateProgressListener callbacks
    @Override
    public void onAuthorsUpdated() {
        toggleUpdatingState();
        this.updateAuthors();
    }
    //endregion


    //region AddAuthorDialog.OnAuthorLinkSuppliedListener callbacks
    @Override
    public void onLinkSupplied(String url) {
        tryAddAuthor(url);
    }
    //endregion


}
