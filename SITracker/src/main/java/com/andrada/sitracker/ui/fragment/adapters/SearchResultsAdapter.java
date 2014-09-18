package com.andrada.sitracker.ui.fragment.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.andrada.sitracker.db.beans.SearchedAuthor;
import com.andrada.sitracker.ui.components.SearchAuthorItemView;
import com.andrada.sitracker.ui.components.SearchAuthorItemView_;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@EBean
public class SearchResultsAdapter extends BaseAdapter {

    @RootContext
    Context context;

    private List<SearchedAuthor> mData = new ArrayList<SearchedAuthor>();

    public void swapData(@NotNull List<SearchedAuthor> data) {
        mData.clear();
        mData.addAll(data);
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
}
