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

package com.andrada.sitracker.db.beans;

import com.andrada.sitracker.db.dao.AuthorDaoImpl;
import com.google.analytics.tracking.android.EasyTracker;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import java.sql.SQLException;
import java.util.Date;

@DatabaseTable(daoClass = AuthorDaoImpl.class, tableName = "authors")
public class Author {
    @DatabaseField(generatedId = true, useGetSet = true, columnName = "_id")
    int id;
    @DatabaseField(canBeNull = false, useGetSet = true)
    String name;
    @DatabaseField(unique = true, useGetSet = true)
    String url;
    @DatabaseField(canBeNull = false, useGetSet = true)
    Date updateDate;
    @DatabaseField(defaultValue = "false", canBeNull = false, columnName = "_id")
    Boolean isNew;

    @ForeignCollectionField(eager = false)
    ForeignCollection<Publication> publications;

    public Author() {
        updateDate = new Date();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public ForeignCollection<Publication> getPublications() {
        return publications;
    }

    public void setPublications(ForeignCollection<Publication> publications) {
        this.publications = publications;
    }

    public Boolean getNew() {
        return isNew;
    }

    public void setNew(Boolean aNew) {
        isNew = aNew;
    }

    /*
    public Boolean isUpdated() {
        //TODO REMOVE this - its killing performance
        boolean isUpdated = false;
        for (Publication pub : this.publications) {
            if (pub.isNew) {
                isUpdated = true;
                break;
            }
        }
        return isUpdated;
    }*/

    public void markRead() {
        this.setNew(false);
        for (Publication pub : this.publications) {
            pub.setNew(false);
            pub.setOldSize(0);
            try {
                this.publications.update(pub);
            } catch (SQLException e) {
                //surface error
                EasyTracker.getTracker().sendException("Author mark as read", e, false);
            }
        }
    }
}
