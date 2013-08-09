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

package com.j256.ormlite.android.support.extras;

import java.sql.SQLException;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.View;

import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.stmt.PreparedQuery;

public abstract class OrmliteCursorAdapter<T> extends CursorAdapter {
    protected PreparedQuery<T> mQuery;

    public OrmliteCursorAdapter(Context context, Cursor c, PreparedQuery<T> query) {
        super(context, c, false);
        mQuery = query;
    }

    @Override
    public void bindView(View itemView, Context context, Cursor cursor) {
        try {
            T item = mQuery.mapRow(new AndroidDatabaseResults(cursor, null));
            bindView(itemView, context, item);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public void setQuery(PreparedQuery<T> query) {
        mQuery = query;
    }

    abstract public void bindView(View itemView, Context context, T item);
}
