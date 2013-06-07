package com.andrada.sitracker.db.dao;

import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.beans.Publication;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by ggodonoga on 07/06/13.
 */
public interface PublicationDao extends Dao<Publication, Integer> {
    List<Publication> getPublicationsForAuthor(Author author) throws SQLException;
    List<Publication> getPublicationsForAuthorId(long authorId) throws SQLException;
    List<Publication> getNewPublicationsForAuthor(Author author) throws SQLException;
    List<Publication> getNewPublicationsForAuthorId(long authorId) throws SQLException;
    long getNewPublicationsCountForAuthor(Author author) throws SQLException;
    long getNewPublicationsCountForAuthorId(long authorId) throws SQLException;
}
