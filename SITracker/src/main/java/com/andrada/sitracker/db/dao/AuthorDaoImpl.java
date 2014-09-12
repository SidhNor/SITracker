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

package com.andrada.sitracker.db.dao;

import com.andrada.sitracker.db.beans.Author;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AuthorDaoImpl extends BaseDaoImpl<Author, Integer>
        implements AuthorDao {

    public AuthorDaoImpl(ConnectionSource connectionSource)
            throws SQLException {
        super(connectionSource, Author.class);
    }

    public List<String> getAuthorsUrls() throws SQLException {
        GenericRawResults results = this.queryRaw("SELECT url FROM authors");
        List<String> authorUrls = new ArrayList<String>();
        while (results.iterator().hasNext()) {
            authorUrls.add(((String[]) results.iterator().next())[0]);
        }
        return authorUrls;
    }

    @Override
    public List<String> getAuthorsUrlIds() throws SQLException {
        GenericRawResults results = this.queryRaw("SELECT urlId FROM authors");
        List<String> authorUrls = new ArrayList<String>();
        while (results.iterator().hasNext()) {
            authorUrls.add(((String[]) results.iterator().next())[0]);
        }
        return authorUrls;
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

    @Override
    public void removeAuthor(long id) throws SQLException {
        this.queryRaw("DELETE FROM publications WHERE author_id = ?", String.valueOf(id));
        DeleteBuilder<Author, Integer> delBuilder = this.deleteBuilder();
        delBuilder.where().eq("_id", id);
        delBuilder.delete();
    }
}
