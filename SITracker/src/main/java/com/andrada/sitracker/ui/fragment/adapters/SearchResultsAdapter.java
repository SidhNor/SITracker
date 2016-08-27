/*
 * Copyright 2016 Gleb Godonoga.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrada.sitracker.ui.fragment.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.andrada.sitracker.db.beans.SearchedAuthor;
import com.andrada.sitracker.db.dao.AuthorDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.ui.components.SearchAuthorItemView;
import com.andrada.sitracker.ui.components.SearchAuthorItemView_;
import com.andrada.sitracker.util.SamlibPageHelper;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.ormlite.annotations.OrmLiteDao;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.andrada.sitracker.util.LogUtils.LOGW;

@EBean
public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {

    @RootContext
    Context context;

    @OrmLiteDao(helper = SiDBHelper.class)
    AuthorDao authorDao;

    private List<SearchedAuthor> mData = new ArrayList<>();

    private Callbacks mCallbacks = null;

    public void setCallbacks(Callbacks mCallbacks) {
        this.mCallbacks = mCallbacks;
    }

    public void swapData(@NotNull List<SearchedAuthor> data) {
        mData.clear();
        mData.addAll(data);
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
    }

    public SearchedAuthor getItem(int position) {
        return mData.get(position);
    }

    public int getItemPositionById(String id) {
        for (int i = 0; i < mData.size(); i++) {
            if (mData.get(i).getAuthorUrl().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    public List<SearchedAuthor> getData() {
        return new ArrayList<>(mData);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(SearchAuthorItemView_.build(context));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (position < getItemCount()) {
            final SearchedAuthor auth = getItem(position);
            holder.view.bind(auth, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCallbacks.onAuthorSelected(auth);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public interface Callbacks {
        void onAuthorSelected(SearchedAuthor author);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        SearchAuthorItemView view;

        public ViewHolder(SearchAuthorItemView itemView) {
            super(itemView);
            view = itemView;
        }
    }
}
