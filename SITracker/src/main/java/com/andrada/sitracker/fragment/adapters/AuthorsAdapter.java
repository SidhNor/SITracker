package com.andrada.sitracker.fragment.adapters;

import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.IsNewItemTappedListener;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.dao.AuthorDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.fragment.components.AuthorItemView;
import com.andrada.sitracker.fragment.components.AuthorItemView_;
import com.andrada.sitracker.tasks.messages.AuthorMarkedAsReadMessage;

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

    @AfterInject
    void initAdapter() {
        reloadAuthors();
    }

    public void reloadAuthors() {
        try {
            authors = authorDao.queryForAll();
            mNewAuthors = authorDao.getNewAuthorsCount();
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
        if (authors.get(position).isUpdated()) {
            authorsItemView.setBackgroundResource(R.drawable.authors_list_item_selector_new);
        } else {
            authorsItemView.setBackgroundResource(R.drawable.authors_list_item_selector_normal);
        }
        authorsItemView.bind(authors.get(position));

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
                    //TODO write error
                    e.printStackTrace();
                }
            }
        }
    }

    public void ensureAuthorIsStillNew(long publicationId) {

    }
}
