package com.andrada.sitracker.ui.fragment.adapters;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.andrada.sitracker.db.beans.SearchedAuthor;

import org.androidannotations.annotations.EBean;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@EBean
public class SearchResultsAdapter extends BaseAdapter {

    private List<SearchedAuthor> mData = new ArrayList<SearchedAuthor>();

    public void swapData(@NotNull List<SearchedAuthor> data) {
        mData = data;
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
    public View getView(int position, View convertView, ViewGroup parenp) {
        return null;
    }
}
