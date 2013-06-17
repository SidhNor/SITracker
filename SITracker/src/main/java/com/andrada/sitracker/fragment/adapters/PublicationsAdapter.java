package com.andrada.sitracker.fragment.adapters;

import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;

import com.andrada.sitracker.contracts.IsNewItemTappedListener;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.dao.PublicationDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.fragment.components.PublicationCategoryItemView;
import com.andrada.sitracker.fragment.components.PublicationCategoryItemView_;
import com.andrada.sitracker.fragment.components.PublicationItemView;
import com.andrada.sitracker.fragment.components.PublicationItemView_;
import com.andrada.sitracker.tasks.messages.PublicationMarkedAsReadMessage;
import com.j256.ormlite.dao.Dao;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.OrmLiteDao;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ggodonoga on 05/06/13.
 */

@EBean
public class PublicationsAdapter extends BaseExpandableListAdapter implements IsNewItemTappedListener {

    List<String> mCategories = new ArrayList<String>();
    List<List<Publication>> mChildren = new ArrayList<List<Publication>>();

    @OrmLiteDao(helper = SiDBHelper.class, model = Publication.class)
    PublicationDao publicationsDao;

    @RootContext
    Context context;

    ListView listView = null;

    public void reloadPublicationsForAuthorId(long id) {
        try {
            createChildList(publicationsDao.getPublicationsForAuthorId(id));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void createChildList(List<Publication> items) {
        mCategories.clear();
        mChildren.clear();

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
        notifyDataSetChanged();
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        List<Publication> items = mChildren.get(groupPosition);
        return items.get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        List<Publication> items = mChildren.get(groupPosition);
        return items.get(childPosition).getId();
    }

    @Override
    public View getChildView(int groupPosition, int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        PublicationItemView publicationItemView;
        if (convertView == null) {
            publicationItemView = PublicationItemView_.build(context);
            publicationItemView.setListener(this);
        } else {
            publicationItemView = (PublicationItemView) convertView;
        }
        Boolean isLast = mChildren.get(groupPosition).size() -1 == childPosition;
        publicationItemView.bind((Publication)getChild(groupPosition, childPosition), isLast);

        return publicationItemView;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        if (listView == null) {
            listView = (ListView) parent;
        }
        PublicationCategoryItemView publicationCategoryView;
        if (convertView == null) {
            publicationCategoryView = PublicationCategoryItemView_.build(context);
        } else {
            publicationCategoryView = (PublicationCategoryItemView) convertView;
        }
        publicationCategoryView.bind(mCategories.get(groupPosition));
        return publicationCategoryView;
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
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    public void markPublicationsAsReadForAuthor(long id) {
        try {
            publicationsDao.markAsReadForAuthorId(id);
        } catch (SQLException e) {
            //TODO handle exception
            e.printStackTrace();
        }
    }

    @Override
    public void onIsNewItemTapped(View checkBox) {
        if (listView != null) {
            final long packedPosition = listView.getAdapter().getItemId(listView.getPositionForView(checkBox));
            final long groupPos = ExpandableListView.getPackedPositionGroup(packedPosition);
            final long childPos = ExpandableListView.getPackedPositionChild(packedPosition);
            Publication pub =  null;//mChildren.get((int) groupPos).get((int) childPos);
            /*if (position != ListView.INVALID_POSITION &&
                    position < authors.size() &&
                    authors.get(position).isUpdated()) {
                try {
                    authorDao.markAsRead(authors.get(position));
                    LocalBroadcastManager.getInstance(context).sendBroadcast(new PublicationMarkedAsReadMessage(mChildren.get(position).get(position).getId()));
                } catch (SQLException e) {
                    //TODO write error
                    e.printStackTrace();
                }
            }*/
        }
    }
}
