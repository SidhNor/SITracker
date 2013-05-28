package com.andrada.sitracker.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import com.andrada.sitracker.R;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.manager.SiSQLiteHelper;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PublicationsFragment extends Fragment{

	public static final String ARG_ID = "author_id";
	ExpandableListView mListView;
	
	long mCurrentId = -1;
	private SiSQLiteHelper helper;

	    @Override
	    public View onCreateView(LayoutInflater inflater, ViewGroup container,
	        Bundle savedInstanceState) {

	        // If activity recreated (such as from screen rotate), restore
	        // the previous article selection set by onSaveInstanceState().
	        // This is primarily necessary when in the two-pane layout.
	        if (savedInstanceState != null) {
	            mCurrentId = savedInstanceState.getLong(ARG_ID);
	        }

	        // Inflate the layout for this fragment
	        View view = inflater.inflate(R.layout.fragment_publications, container, false);
	        mListView = (ExpandableListView) view.findViewById(R.id.publication_list);
	        return view;
	    }
	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	    	super.onCreate(savedInstanceState);
	    	setRetainInstance(true);
	    	Bundle  bundle = getArguments();
			if (bundle  != null) {
	            mCurrentId = bundle.getLong(ARG_ID);
	        }
	    }
	    @Override
	    public void onAttach(Activity activity) {
	    	super.onAttach(activity);
	    	helper = new SiSQLiteHelper(activity);

	    }
	    @Override
	    public void onStart() {
	    	super.onStart();
            if (mCurrentId >= 0)
	    	    updatePublicationsView(mCurrentId, getActivity());
	    }

	public void updatePublicationsView(long id, Context context) {
        mCurrentId = id;
		List<Publication> items = new ArrayList<Publication>();
		try {
			items = helper.getPublicationDao().queryBuilder().where().eq("authorID", id).query();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		PublicationsAdapter mAdapter = new PublicationsAdapter(items, context);
		mListView.setAdapter(mAdapter);
	}
	
	private class PublicationsAdapter extends BaseExpandableListAdapter {
		List<String> mCategories = new ArrayList<String>();
		List<List<Publication>> mChildren = new ArrayList<List<Publication>>();
		
		private final Context context;
		
		public PublicationsAdapter(List<Publication> items, Context context) {
			this.context = context;
			createChildList(items);
		}
				
		private void createChildList(List<Publication> items) {
			for (Publication publication : items) {
				if(!mCategories.contains(publication.getCategory())){
					mCategories.add(publication.getCategory());
				}
			}
			
			for (String category : mCategories) {
				List<Publication> categoryList = new ArrayList<Publication>();
				for (Publication publication : items) {
					if (publication.getCategory().equals(category)) {
						categoryList.add(publication);
					}
				}
				mChildren.add(categoryList);
			}
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			List<Publication> items = mChildren.get(groupPosition);
			return items.get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			// TODO Auto-generated method stub
			List<Publication> items = mChildren.get(groupPosition);
			return items.get(childPosition).getId();
		}

		@Override
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			View view = convertView;
            if (view == null) {
			   view = LayoutInflater.from(context).inflate(R.layout.publications_item, null);
			}
			TextView title = (TextView) view.findViewById(R.id.item_title);
            Publication child = (Publication)getChild(groupPosition, childPosition);
			title.setText(child.getName());
            TextView description = (TextView) view.findViewById(R.id.item_description);
            description.setText(child.getDescription());
			return view;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return mChildren.get(groupPosition).size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return mCategories.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return mCategories.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			View view = convertView;
			  if (view == null) {
			   view = LayoutInflater.from(context).inflate(R.layout.publications_category, null);
			  }

			  TextView categoryTitle = (TextView) view.findViewById(R.id.category_title);
			  categoryTitle.setText(mCategories.get(groupPosition));
			return view;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return false;
		}
		
	}

}
