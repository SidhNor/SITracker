package com.andrada.sitracker.db.beans;

import com.andrada.sitracker.db.dao.AuthorDaoImpl;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

@DatabaseTable(daoClass = AuthorDaoImpl.class)
public class Author {
	@DatabaseField(generatedId = true)
	int id;
    @DatabaseField(canBeNull = false)
	String name;
	@DatabaseField(unique = true)
	String url;
    @DatabaseField(canBeNull = false)
    Date updateDate;
    @DatabaseField(defaultValue = "false", canBeNull = false)
    Boolean updated;

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

    public Boolean isUpdated() {
        return updated;
    }

    public void setUpdated(Boolean updated) {
        this.updated = updated;
    }


}
