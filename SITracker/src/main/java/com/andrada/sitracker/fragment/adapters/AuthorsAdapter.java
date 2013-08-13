/*
 * Copyright 2013 Gleb Godonoga.
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

package com.andrada.sitracker.fragment.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;

import com.andrada.sitracker.contracts.IsNewItemTappedListener;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.dao.AuthorDaoImpl;
import com.andrada.sitracker.events.AuthorMarkedAsReadEvent;
import com.andrada.sitracker.fragment.components.AuthorItemView;
import com.andrada.sitracker.fragment.components.AuthorItemView_;
import com.google.analytics.tracking.android.EasyTracker;
import com.j256.ormlite.android.support.extras.OrmliteCursorAdapter;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import java.sql.SQLException;
import java.util.Collection;

import de.greenrobot.event.EventBus;

/**
 * Created by ggodonoga on 05/06/13.
 */

@EBean
public class AuthorsAdapter extends OrmliteCursorAdapter<Author> implements IsNewItemTappedListener {

    @RootContext
    Context context;
    private int mSelectedItem = 0;

    private long mSelectedAuthorId = 0;

    public AuthorsAdapter(Context context) {
        super(context, null, null);
    }

    @Override
    public int getCount() {
        if (mCursor != null) {
            return mCursor.getCount();
        }
        return 0;
    }

    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        AuthorItemView authorsItemView = AuthorItemView_.build(context);
        authorsItemView.setListener(this);
        return authorsItemView;
    }

    public void bindView(View itemView, Context context, Author item) {
        AuthorItemView authorsItemView = (AuthorItemView) itemView;
        authorsItemView.bind(item, mSelectedAuthorId == item.getId());
    }

    @Override
    public void onIsNewItemTapped(View starButton) {
        Author auth = (Author) starButton.getTag();
        if (auth != null) {
            auth.markRead();
            EventBus.getDefault().post(new AuthorMarkedAsReadEvent(auth.getId()));
        }
    }

    public void removeAuthors(Collection<Author> authorsToRemove, AuthorDaoImpl authorDao) {
        try {
            authorDao.delete(authorsToRemove);
            authorDao.notifyContentChange();
        } catch (SQLException e) {
            EasyTracker.getTracker().sendException("Author Remove thread", e, false);
        }

        boolean removingCurrentlySelected = false;
        for (Author anAuthorToRemove : authorsToRemove) {
            if (anAuthorToRemove.getId() == mSelectedAuthorId) {
                removingCurrentlySelected = true;
            }
        }
        if (removingCurrentlySelected) {
            //Try select the first one
            setSelectedItem(getFirstAuthorId());
        } else {
            setSelectedItem(mSelectedAuthorId);
        }
    }

    public long getFirstAuthorId() {
        if (mCursor != null && mCursor.getCount() > 0) {
            return this.getItemId(0);
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
        if (mCursor != null) {
            if (mSelectedItem < mCursor.getCount() && mSelectedItem >= 0)
                return this.getItem(mSelectedItem);
        }
        return null;
    }

    private int getItemPositionByAuthorId(long authorId) {
        if (mCursor != null) {
            for (int i = 0; i < mCursor.getCount(); i++) {
                if (this.getItemId(i) == authorId) {
                    return i;
                }
            }
        }
        return -1;
    }
}
