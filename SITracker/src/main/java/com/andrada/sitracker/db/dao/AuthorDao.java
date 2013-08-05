package com.andrada.sitracker.db.dao;

import com.andrada.sitracker.db.beans.Author;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by ggodonoga on 07/06/13.
 */
public interface AuthorDao extends Dao<Author, Integer> {
    int getNewAuthorsCount() throws SQLException;

    void markAsRead(Author author) throws SQLException;

    List<Author> getAllAuthorsSorted() throws SQLException;
}
