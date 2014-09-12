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

package com.andrada.sitracker.db.beans;

import com.andrada.sitracker.db.dao.AuthorDaoImpl;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;

@DatabaseTable(daoClass = AuthorDaoImpl.class, tableName = "authors")
public class Author implements Serializable {

    private static final long serialVersionUID = -4329046928579678402L;

    @DatabaseField(generatedId = true, useGetSet = true, columnName = "_id")
    long id;
    @DatabaseField(canBeNull = false, useGetSet = true)
    String name;
    @DatabaseField(unique = true, useGetSet = true)
    String url;
    @DatabaseField(unique = true, useGetSet = true)
    String urlId;
    @DatabaseField(canBeNull = false, useGetSet = true)
    Date updateDate;
    @DatabaseField(canBeNull = true, useGetSet = true)
    String authorImageUrl;
    @DatabaseField(canBeNull = true, useGetSet = true)
    String authorDescription;
    @DatabaseField(defaultValue = "false", canBeNull = false)
    Boolean isNew;

    @ForeignCollectionField(eager = false)
    transient ForeignCollection<Publication> publications;

    public Author() {
        updateDate = new Date();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
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

    public String getUrlId() {
        return urlId;
    }

    public void setUrlId(String urlId) {
        this.urlId = urlId;
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

    public String getAuthorImageUrl() {
        return authorImageUrl;
    }

    public void setAuthorImageUrl(String authorImageUrl) {
        this.authorImageUrl = authorImageUrl;
    }

    public String getAuthorDescription() {
        return authorDescription;
    }

    public void setAuthorDescription(String authorDescription) {
        this.authorDescription = authorDescription;
    }

    public Boolean getNew() {
        return isNew;
    }

    public void setNew(Boolean aNew) {
        isNew = aNew;
    }

    public void markRead() {
        this.setNew(false);
        for (Publication pub : this.publications) {
            pub.setNew(false);
            pub.setOldSize(0);
            try {
                this.publications.update(pub);
            } catch (SQLException e) {
                //surface error
                //Eat exception
            }
        }
    }
}
