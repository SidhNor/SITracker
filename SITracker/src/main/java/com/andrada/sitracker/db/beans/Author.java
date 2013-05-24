package com.andrada.sitracker.db.beans;

import com.j256.ormlite.field.DatabaseField;

public class Author {
	@DatabaseField(generatedId = true)
	int id;
	@DatabaseField
	String name;
	@DatabaseField
	String url;

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

}
