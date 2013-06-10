package com.andrada.sitracker.db.beans;

import com.andrada.sitracker.db.dao.PublicationDaoImpl;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

@DatabaseTable(daoClass = PublicationDaoImpl.class)
public class Publication {
	@DatabaseField(generatedId = true, useGetSet = true)
	int id;
    @DatabaseField(canBeNull = false, useGetSet = true)
	String name;
    @DatabaseField(useGetSet = true)
	int size;
    @DatabaseField(useGetSet = true)
    int oldSize;
	@DatabaseField(useGetSet = true)
	String category;
    @DatabaseField(canBeNull = false, index = true)
	long authorID;
	@DatabaseField(useGetSet = true)
	String date;
	@DatabaseField(useGetSet = true)
	String description;
    @DatabaseField(useGetSet = true)
    String commentUrl;
    @DatabaseField(canBeNull = false, unique = true, useGetSet = true)
	String url;
    @DatabaseField(useGetSet = true)
    String rating;
    @DatabaseField(useGetSet = true)
    int commentsCount;
    @DatabaseField(defaultValue = "false", canBeNull = false)
    Boolean isNew;
    @DatabaseField(canBeNull = false, useGetSet = true)
    Date updateDate;


    public Publication() {
        updateDate = new Date();
    }

    @Override
    public boolean equals(Object object)
    {
        boolean sameSame = false;

        if (object != null && object instanceof Publication)
        {
            sameSame = this.getUrl().equals(((Publication) object).getUrl());
        }

        return sameSame;
    }

    public int hashCode() {
        return getUrl().hashCode();
    }


    //region Getters setters
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

    public int getOldSize() {
        return oldSize;
    }

    public void setOldSize(int oldSize) {
        this.oldSize = oldSize;
    }

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public long getAuthorID() {
		return authorID;
	}

	public void setAuthorID(long authorID) {
		this.authorID = authorID;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

    public String getCommentUrl() {
        return commentUrl;
    }

    public void setCommentUrl(String commentUrl) {
        this.commentUrl = commentUrl;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public int getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(int commentsCount) {
        this.commentsCount = commentsCount;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public Boolean getNew() {
        return isNew;
    }

    public void setNew(Boolean aNew) {
        isNew = aNew;
    }
    //endregion

}
