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

package com.andrada.sitracker.db.dao;

import com.andrada.sitracker.db.beans.Author;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by ggodonoga on 07/06/13.
 */
public class AuthorDaoImpl extends BaseDaoImpl<Author, Integer>
        implements AuthorDao {

    public AuthorDaoImpl(ConnectionSource connectionSource)
            throws SQLException {
        super(connectionSource, Author.class);
    }

    @Override
    public int getNewAuthorsCount() throws SQLException {

        return (int) this.queryRawValue(
                "SELECT COUNT(DISTINCT authors._id) FROM authors, publications " +
                        "WHERE authors._id = publications.author_id AND publications.isNew = 1");
    }

    @Override
    public List<Author> getAllAuthorsSortedAZ() throws SQLException {
        return this.queryBuilder().orderBy("name", true).query();
    }

    @Override
    public List<Author> getAllAuthorsSortedNew() throws SQLException {
        return this.queryBuilder().orderBy("updateDate", false).query();
    }

    @Override
    public void markAsRead(Author author) throws SQLException {
        author.markRead();
        this.update(author);
    }
}
