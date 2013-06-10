package com.andrada.sitracker.db.dao;

import com.andrada.sitracker.db.beans.Author;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by ggodonoga on 07/06/13.
 */
public interface AuthorDao extends Dao<Author, Integer> {
    long getNewAuthorsCount() throws SQLException;
    List<Author> getNewAuthors() throws SQLException;
    void markAsRead(Author author) throws SQLException;
}
