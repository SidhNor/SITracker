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

package com.andrada.sitracker.db.manager;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.dao.AuthorDao;
import com.andrada.sitracker.db.dao.PublicationDao;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

public class SiDBHelper extends OrmLiteSqliteOpenHelper {

    private static final String DATABASE_NAME = "siinformer.db";
    private static final int DATABASE_VERSION = 8;

    private PublicationDao publicationDao;
    private AuthorDao authorDao;

    public SiDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, Author.class);
            TableUtils.createTable(connectionSource, Publication.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource,
                          int oldVersion, int newVersion) {
        try {
            while (++oldVersion <= newVersion) {
                switch (oldVersion) {
                    case 2: {
                        getPublicationDao().executeRaw("ALTER TABLE 'publications' ADD COLUMN oldSize INTEGER;");
                        break;
                    }
                    case 3: {
                        getPublicationDao().executeRaw("CREATE INDEX 'fk_author_publication' ON 'publication' ('authorID' ASC)");
                        break;
                    }
                    case 6: {
                        getAuthorDao().executeRaw(
                                "ALTER TABLE authors RENAME TO tmp_authors;");
                        TableUtils.createTableIfNotExists(connectionSource, Author.class);
                        getAuthorDao().executeRaw(
                                "INSERT INTO authors(_id, name, url, updateDate) " +
                                        "SELECT id, name, url, updateDate " +
                                        "FROM tmp_authors;"
                        );
                        //Look at all author publications and update accordingly
                        getAuthorDao().executeRaw(
                                "UPDATE authors SET isNew = 1 " +
                                        "WHERE _id IN " +
                                        "(SELECT DISTINCT(author_id) FROM " +
                                        "publications WHERE publications.isNew = 1)"
                        );
                        getAuthorDao().executeRaw("DROP TABLE tmp_authors;");
                        break;
                    }
                    case 7: {
                        getPublicationDao().executeRaw("ALTER TABLE 'publications' ADD COLUMN imageUrl TEXT;");
                        break;
                    }
                    case 8: {
                        getAuthorDao().executeRaw("ALTER TABLE 'authors' ADD COLUMN authorImageUrl TEXT;");
                        getAuthorDao().executeRaw("ALTER TABLE 'authors' ADD COLUMN authorDescription TEXT;");
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public AuthorDao getAuthorDao() throws SQLException {
        if (authorDao == null) {
            authorDao = getDao(Author.class);
        }
        return authorDao;
    }

    public PublicationDao getPublicationDao() throws SQLException {
        if (publicationDao == null) {
            publicationDao = getDao(Publication.class);
        }
        return publicationDao;
    }

    @Override
    public void close() {
        super.close();
        publicationDao = null;
        authorDao = null;
    }

}
