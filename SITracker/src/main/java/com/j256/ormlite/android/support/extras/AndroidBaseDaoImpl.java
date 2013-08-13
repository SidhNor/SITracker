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

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.Loader;

import com.j256.ormlite.android.AndroidCompiledStatement;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.stmt.PreparedDelete;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.StatementBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;
import com.j256.ormlite.table.DatabaseTableConfig;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class AndroidBaseDaoImpl<T, ID> extends BaseDaoImpl<T, ID> {

    public AndroidBaseDaoImpl(Class<T> dataClass) throws SQLException {
        super(dataClass);
    }

    public AndroidBaseDaoImpl(ConnectionSource connectionSource, Class<T> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    public AndroidBaseDaoImpl(ConnectionSource connectionSource, DatabaseTableConfig<T> tableConfig) throws SQLException {
        super(connectionSource, tableConfig);
    }

    public Cursor getCursor(PreparedQuery<T> query) throws SQLException {
        DatabaseConnection readOnlyConn = connectionSource.getReadOnlyConnection();
        AndroidCompiledStatement stmt = (AndroidCompiledStatement) query.compile(readOnlyConn, StatementBuilder.StatementType.SELECT);
        return stmt.getCursor();
    }

    public OrmliteCursorLoader<T> getSQLCursorLoader(Context context, PreparedQuery<T> query) throws SQLException {
        OrmliteCursorLoader<T> loader = new OrmliteCursorLoader<T>(context, this, query);
        synchronized (mLoaders) {
            mLoaders.add(new WeakReference<Loader<?>>(loader));
        }
        return loader;
    }

    protected List<WeakReference<Loader<?>>> mLoaders = Collections.synchronizedList(new ArrayList<WeakReference<Loader<?>>>()); // new

    public void notifyContentChange() {
        synchronized (mLoaders) {
            for (Iterator<WeakReference<Loader<?>>> itr = mLoaders.iterator(); itr.hasNext(); ) {
                WeakReference<Loader<?>> weakRef = itr.next();
                Loader<?> loader = weakRef.get();
                if (loader == null) {
                    itr.remove();
                } else {
                    loader.onContentChanged();
                }
            }
        }
    }

    @Override
    public int create(T arg0) throws SQLException {
        int result = super.create(arg0);
        if (result > 0) {
            notifyContentChange();
        }
        return result;
    }

    @Override
    public int updateRaw(String arg0, String... arg1) throws SQLException {
        int result = super.updateRaw(arg0, arg1);
        if (result > 0) {
            notifyContentChange();
        }
        return result;
    }

    @Override
    public int delete(PreparedDelete<T> preparedDelete) throws SQLException {
        int result = super.delete(preparedDelete);
        if (result > 0) {
            notifyContentChange();
        }
        return result;
    }
}
