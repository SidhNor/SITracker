package com.andrada.sitracker.fragment.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.components.AuthorItemView;
import com.andrada.sitracker.components.AuthorItemView_;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.util.DateFormatterUtil;
import com.j256.ormlite.dao.Dao;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.OrmLiteDao;
import org.androidannotations.annotations.RootContext;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ggodonoga on 05/06/13.
 */

@EBean
public class AuthorsAdapter extends BaseAdapter {

    List<Author> authors;

    @OrmLiteDao(helper = SiDBHelper.class, model = Author.class)
    Dao<Author, Integer> authorDao;

    @RootContext
    Context context;

    @AfterInject
    void initAdapter() {
        try {
            authors = authorDao.queryForAll();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void reloadAuthors() {
        try {
            authors = authorDao.queryForAll();
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

        AuthorItemView authorsItemView;
        if (convertView == null) {
            authorsItemView = AuthorItemView_.build(context);
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
}
