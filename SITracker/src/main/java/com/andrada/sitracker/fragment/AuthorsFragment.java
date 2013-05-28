package com.andrada.sitracker.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.andrada.sitracker.R;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.manager.SiSQLiteHelper;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AuthorsFragment extends ListFragment {
	OnAuthorSelectedListener mCallback;

	public interface OnAuthorSelectedListener {
		public void onAuthorSelected(long id);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

    @Override
	public void onStart() {
		super.onStart();

		// When in two-pane layout, set the listview to highlight the selected
		// list item
		// (We do this during onStart because at the point the listview is
		// available.)
		if (getFragmentManager().findFragmentById(R.id.fragment_publications) != null) {
			getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			updateView();
			// This makes sure that the container activity has implemented
			// the callback interface. If not, it throws an exception.
			mCallback = (OnAuthorSelectedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnAuthorSelectedListener");
		}
	}

	public void updateView() {
		try {
			SiSQLiteHelper helper = new SiSQLiteHelper(getActivity());
			List<Author> authors = helper.getAuthorDao().queryForAll();
			setListAdapter(new AuthorsAdapter(authors, getActivity()));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void updateViewAtPosition(int position) {
		updateView();
		getListView().setItemChecked(position, true);
		getListView().setSelection(position);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// Notify the parent activity of selected item
		mCallback.onAuthorSelected(id);

		// Set the item as checked to be highlighted when in two-pane layout
		getListView().setItemChecked(position, true);
	}
	
	private class AuthorsAdapter extends BaseAdapter {
		List<Author> authors;
		private LayoutInflater inflater;
		
		public AuthorsAdapter(List<Author> authors, Context context) {
			this.authors = new ArrayList<Author>();
			this.authors.addAll(authors);
			this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return authors.size();
		}

		@Override
		public Object getItem(int position) {
			return authors.get(position);
		}

		@Override
		public long getItemId(int position) {
			return authors.get(position).getId();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			int layout = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
                    android.R.layout.simple_list_item_activated_1 : android.R.layout.simple_list_item_1;
			TextView view = (TextView) inflater.inflate(layout, parent,false);
            if (view != null) {
                view.setText(authors.get(position).getName());
            }
            return view;
		}
		
	}
}
