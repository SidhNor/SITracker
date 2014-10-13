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

package com.andrada.sitracker.db.dao;

import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.beans.Publication;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;

import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;

import static com.andrada.sitracker.util.LogUtils.LOGE;

public class PublicationDaoImpl extends BaseDaoImpl<Publication, Long>
        implements PublicationDao {


    public PublicationDaoImpl(ConnectionSource connectionSource)
            throws SQLException {
        super(connectionSource, Publication.class);
    }

    @Override
    public Publication getPublicationForId(long id) {
        Publication pub = null;
        try {
            pub = this.queryForId(id);
        } catch (SQLException e) {
            LOGE("SITracker", "Failed to retrieve publication by url and id");
        }
        return pub;
    }

    @NotNull
    @Override
    public List<Publication> getPublicationsForAuthor(@NotNull Author author) throws SQLException {
        return getPublicationsForAuthorId(author.getId());
    }

    @NotNull
    @Override
    public List<Publication> getPublicationsForAuthorId(long authorId) throws SQLException {
        return this.queryBuilder().where().eq("author_id", authorId).query();
    }

    @NotNull
    @Override
    public List<Publication> getNewPublications() throws SQLException {
        return this.queryBuilder()
                .orderBy("updateDate", false)
                .where()
                .eq("isNew", true).query();
    }


    @NotNull
    @Override
    public List<Publication> getNewPublicationsForAuthor(@NotNull Author author) throws SQLException {
        return getNewPublicationsForAuthorId(author.getId());
    }

    @NotNull
    @Override
    public List<Publication> getNewPublicationsForAuthorId(long authorId) throws SQLException {
        return this.queryBuilder().where()
                .eq("author_id", authorId)
                .and()
                .eq("isNew", true).query();
    }

    @Override
    public long getNewPublicationsCountForAuthor(@NotNull Author author) throws SQLException {
        return getNewPublicationsCountForAuthorId(author.getId());
    }

    @Override
    public long getNewPublicationsCountForAuthorId(long authorId) throws SQLException {
        return this.queryBuilder().where()
                .eq("author_id", authorId)
                .and()
                .eq("isNew", true).countOf();
    }

    @NotNull
    @Override
    public List<Publication> getSortedPublicationsForAuthorId(long authorId) throws SQLException {
        return this.queryBuilder()
                .orderBy("isNew", false)
                .orderBy("updateDate", false)
                .orderBy("category", true)
                .where().eq("author_id", authorId)
                .query();
    }

    @Override
    public boolean markPublicationRead(@NotNull Publication pub) throws SQLException {
        long authId = pub.getAuthor().getId();
        pub.setNew(false);
        pub.setOldSize(0);
        this.update(pub);
        int newPubCount = (int) getNewPublicationsCountForAuthorId(authId);
        if (newPubCount == 0) {
            this.executeRaw("UPDATE authors SET isNew=0 WHERE _id = " + authId);
        }
        return newPubCount == 0;
    }
}
