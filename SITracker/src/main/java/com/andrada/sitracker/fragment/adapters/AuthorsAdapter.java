package com.andrada.sitracker.fragment.adapters;

import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.andrada.sitracker.contracts.IsNewItemTappedListener;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.dao.AuthorDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.fragment.components.AuthorItemView;
import com.andrada.sitracker.fragment.components.AuthorItemView_;
import com.andrada.sitracker.tasks.messages.AuthorMarkedAsReadMessage;
import com.google.analytics.tracking.android.EasyTracker;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.OrmLiteDao;
import org.androidannotations.annotations.RootContext;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by ggodonoga on 05/06/13.
 */

@EBean
public class AuthorsAdapter extends BaseAdapter implements IsNewItemTappedListener {

    List<Author> authors;
    long mNewAuthors;
    ListView listView = null;

    @OrmLiteDao(helper = SiDBHelper.class, model = Author.class)
    AuthorDao authorDao;

    @RootContext
    Context context;
    private int mSelectedItem = 0;

    private long mSelectedAuthorId = 0;

    @AfterInject
    void initAdapter() {
        reloadAuthors();
    }

    public void reloadAuthors() {
        try {
            authors = authorDao.getAllAuthorsSorted();
            mNewAuthors = authorDao.getNewAuthorsCount();
            setSelectedItem(mSelectedAuthorId);
            notifyDataSetChanged();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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

        if (listView == null) {
            listView = (ListView) parent;
        }
        AuthorItemView authorsItemView;
        if (convertView == null) {
            authorsItemView = AuthorItemView_.build(context);
            authorsItemView.setListener(this);
        } else {
            authorsItemView = (AuthorItemView) convertView;
        }
        authorsItemView.bind(authors.get(position), position == mSelectedItem);
        return authorsItemView;
    }

    @Override
    public void onIsNewItemTapped(View checkBox) {
        if (listView != null) {
            final int position = listView.getPositionForView(checkBox);
            if (position != ListView.INVALID_POSITION &&
                    position < authors.size() &&
                    authors.get(position).isUpdated()) {
                try {
                    authorDao.markAsRead(authors.get(position));
                    LocalBroadcastManager.getInstance(context).sendBroadcast(new AuthorMarkedAsReadMessage(authors.get(position).getId()));
                } catch (SQLException e) {
                    //surface error
                    EasyTracker.getTracker().sendException("Author Mark as read thread", e, false);
                }
            }
        }
    }

    public void removeAuthors(List<Author> authorsToRemove) {
        try {
            authorDao.delete(authorsToRemove);
        } catch (SQLException e) {
            EasyTracker.getTracker().sendException("Author Remove thread", e, false);
        }

        boolean removingCurrentlySelected = false;
        for (Author anAuthorToRemove : authorsToRemove) {
            if (anAuthorToRemove.getId() == mSelectedAuthorId) {
                removingCurrentlySelected = true;
            }
            authors.remove(anAuthorToRemove);
        }
        if (removingCurrentlySelected) {
            //Try select the first one
            setSelectedItem(getFirstAuthorId());
        } else {
            setSelectedItem(mSelectedAuthorId);
        }
        notifyDataSetChanged();
    }

    public long getFirstAuthorId() {
        if (authors.size() > 0) {
            return authors.get(0).getId();
        }
        return -1;
    }

    public void setSelectedItem(long selectedItemId) {
        this.mSelectedAuthorId = selectedItemId;
        this.mSelectedItem = getItemPositionByAuthorId(selectedItemId);
    }

    public long getSelectedAuthorId() {
        return this.mSelectedAuthorId;
    }

    public Author getCurrentlySelectedAuthor() {
        if (authors.size() > 0)
            return authors.get(mSelectedItem);
        return null;
    }

    private int getItemPositionByAuthorId(long authorId) {
        for (int i = 0; i < authors.size(); i++) {
            if (authors.get(i).getId() == authorId) {
                return i;
            }
        }
        return -1;
    }
}
