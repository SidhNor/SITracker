/*
 * Copyright 2014 Gleb Godonoga.
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
import android.view.View;
import android.view.ViewGroup;

import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.beans.SearchedAuthor;
import com.andrada.sitracker.db.dao.AuthorDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.ui.components.CollectionViewCallbacks;
import com.andrada.sitracker.ui.components.SearchAuthorItemView;
import com.andrada.sitracker.ui.components.SearchAuthorItemView_;
import com.andrada.sitracker.util.SamlibPageHelper;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.OrmLiteDao;
import org.androidannotations.annotations.RootContext;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.andrada.sitracker.util.LogUtils.LOGW;

@EBean
public class SearchResultsAdapter implements CollectionViewCallbacks {

    @RootContext
    Context context;

    @OrmLiteDao(helper = SiDBHelper.class, model = Author.class)
    AuthorDao authorDao;

    private List<SearchedAuthor> mData = new ArrayList<SearchedAuthor>();

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

    public int getCount() {
        return mData.size();
    }

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

    public List<SearchedAuthor> getData() {
        return new ArrayList<SearchedAuthor>(mData);
    }


    @Override
    public View newCollectionHeaderView(Context context, ViewGroup parent) {
        return null;
    }

    @Override
    public void bindCollectionHeaderView(Context context, View view, int groupId, String groupLabel) {
        //We don't have headers
    }

    @Override
    public View newCollectionItemView(Context context, int groupId, ViewGroup parent) {
        return SearchAuthorItemView_.build(context);
    }

    @Override
    public void bindCollectionItemView(Context context, View view, int groupId, int indexInGroup, int dataIndex, Object tag) {
        SearchAuthorItemView authView = (SearchAuthorItemView) view;
        if (dataIndex < getCount()) {
            final SearchedAuthor auth = (SearchedAuthor) getItem(dataIndex);
            authView.bind(auth, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCallbacks.onAuthorSelected(auth);
                }
            });
        }
    }

    public interface Callbacks {
        public void onAuthorSelected(SearchedAuthor author);
    }
}
