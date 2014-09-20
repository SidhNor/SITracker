package com.andrada.sitracker.ui.fragment.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.beans.SearchedAuthor;
import com.andrada.sitracker.db.dao.AuthorDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.ui.components.SearchAuthorItemView;
import com.andrada.sitracker.ui.components.SearchAuthorItemView_;
import com.andrada.sitracker.util.SamlibPageHelper;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.OrmLiteDao;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.andrada.sitracker.util.LogUtils.LOGW;

@EBean
public class SearchResultsAdapter extends BaseAdapter {

    @RootContext
    Context context;

    @OrmLiteDao(helper = SiDBHelper.class, model = Author.class)
    AuthorDao authorDao;

    private List<SearchedAuthor> mData = new ArrayList<SearchedAuthor>();

    public void swapData(@NotNull List<SearchedAuthor> data) {
        mData.clear();
        mData.addAll(data);
        notifyDataSetChanged();
        checkAuthExistance();
    }

    @Background
    protected void checkAuthExistance() {
        for (SearchedAuthor auth : mData) {
            String urlId = SamlibPageHelper.getUrlIdFromCompleteUrl(auth.getAuthorUrl());
            try {
                auth.setAdded(authorDao.hasAuthor(urlId));
            } catch (SQLException e) {
                LOGW("SiTracker", "Could not check if author exists");
            }
        }
        postDataSetChanged();
    }

    @UiThread
    protected void postDataSetChanged() {
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    public SearchedAuthor getItemById(String id) {
        for (SearchedAuthor auth : mData) {
            if (auth.getAuthorUrl().equals(id)) {
                return auth;
            }
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return mData.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SearchAuthorItemView authorsItemView;
        if (convertView == null) {
            authorsItemView = SearchAuthorItemView_.build(context);
        } else {
            authorsItemView = (SearchAuthorItemView) convertView;
        }
        if (position < mData.size()) {
            authorsItemView.bind(mData.get(position));
        }
        return authorsItemView;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

}
