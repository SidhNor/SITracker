/*
 * Copyright 2014 Gleb Godonoga.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import android.widget.BaseAdapter;

import com.andrada.sitracker.contracts.IsNewItemTappedListener;
import com.andrada.sitracker.contracts.SIPrefs_;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.dao.AuthorDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.events.AuthorMarkedAsReadEvent;
import com.andrada.sitracker.events.AuthorSelectedEvent;
import com.andrada.sitracker.ui.components.AuthorItemView;
import com.andrada.sitracker.ui.components.AuthorItemView_;
import com.andrada.sitracker.util.AnalyticsHelper;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.OrmLiteDao;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import de.greenrobot.event.EventBus;

@EBean
public class AuthorsAdapter extends BaseAdapter implements IsNewItemTappedListener {

    List<Author> authors = new ArrayList<Author>();
    long mNewAuthors;

    @OrmLiteDao(helper = SiDBHelper.class, model = Author.class)
    AuthorDao authorDao;

    @Pref
    SIPrefs_ prefs;

    @RootContext
    Context context;
    private int mSelectedItem = 0;

    private long mSelectedAuthorId = 0;

    @AfterInject
    void initAdapter() {
        reloadAuthors();
    }

    /**
     * Reloads authors in background posting change set notification to UI Thread
     */
    @Background
    public void reloadAuthors() {
        try {
            int sortType = Integer.parseInt(prefs.authorsSortType().get());
            if (sortType == 0) {
                authors = authorDao.getAllAuthorsSortedAZ();
            } else {
                authors = authorDao.getAllAuthorsSortedNew();
            }
            mNewAuthors = authorDao.getNewAuthorsCount();
            setSelectedItem(mSelectedAuthorId);
            postDataSetChanged();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @UiThread
    protected void postDataSetChanged() {
        notifyDataSetChanged();
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

    @Nullable
    @Override
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        AuthorItemView authorsItemView;
        if (convertView == null) {
            authorsItemView = AuthorItemView_.build(context);
            authorsItemView.setListener(this);
        } else {
            authorsItemView = (AuthorItemView) convertView;
        }
        if (position < authors.size()) {
            authorsItemView.bind(authors.get(position), position == mSelectedItem);
        }
        return authorsItemView;
    }

    @Override
    @Background
    public void onIsNewItemTapped(@NotNull View starButton) {
        Author auth = (Author) starButton.getTag();
        dismissAuthor(auth);
    }

    @Background
    public void markAuthorsRead(@NotNull List<Long> authorsToMarkAsRead) {
        for (long authId : authorsToMarkAsRead) {
            dismissAuthor(this.getAuthorById(authId));
        }
    }

    /**
     * Should be called on background thread only
     *
     * @param auth Author to mark as read
     */
    private void dismissAuthor(@Nullable Author auth) {
        if (auth != null) {
            auth.markRead();
            try {
                authorDao.update(auth);
                EventBus.getDefault().post(new AuthorMarkedAsReadEvent(auth));
            } catch (SQLException e) {
                AnalyticsHelper.getInstance().sendException("Author mark as read: ", e);
            }
        }
    }

    @Background
    public void removeAuthors(@NotNull final List<Long> authorsToRemove) {
        try {
            authorDao.callBatchTasks(new Callable<Object>() {
                @Nullable
                @Override
                public Object call() throws Exception {
                    for (Long anAuthorsToRemove : authorsToRemove) {
                        authorDao.removeAuthor(anAuthorsToRemove);
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            AnalyticsHelper.getInstance().sendException("Author Remove thread: ", e);
        }

        boolean removingCurrentlySelected = false;
        for (Long anAuthorToRemove : authorsToRemove) {
            if (anAuthorToRemove == mSelectedAuthorId) {
                removingCurrentlySelected = true;
            }
            authors.remove(getAuthorById(anAuthorToRemove));
        }
        if (removingCurrentlySelected) {
            //Try select the first one
            setSelectedItem(getFirstAuthorId());
        } else {
            setSelectedItem(mSelectedAuthorId);
        }
        postDataSetChanged();
    }

    public long getFirstAuthorId() {
        if (authors.size() > 0) {
            return authors.get(0).getId();
        }
        return -1;
    }

    public void setSelectedItem(long selectedItemId) {
        int potentialSelectedItem = getItemPositionByAuthorId(selectedItemId);
        long potentialAuthorId = selectedItemId;
        if (potentialSelectedItem == -1) {
            potentialSelectedItem = 0;
            potentialAuthorId = getFirstAuthorId();
        }
        this.mSelectedAuthorId = potentialAuthorId;
        EventBus.getDefault().post(new AuthorSelectedEvent(mSelectedAuthorId, potentialSelectedItem == 0));
        this.mSelectedItem = potentialSelectedItem;
    }

    public long getSelectedAuthorId() {
        return this.mSelectedAuthorId;
    }

    @Nullable
    public Author getCurrentlySelectedAuthor() {
        if (mSelectedItem < authors.size() && mSelectedItem >= 0)
            return authors.get(mSelectedItem);
        return null;
    }

    @Nullable
    private Author getAuthorById(long authorId) {
        for (Author author : authors) {
            if (author.getId() == authorId) {
                return author;
            }
        }
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
