package com.andrada.sitracker.db.manager;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.beans.Publication;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

public class SiDBHelper extends OrmLiteSqliteOpenHelper {

	private static final String DATABASE_NAME = "siinformer.db";
	private static final int DATABASE_VERSION = 2;

	private Dao<Publication, Integer> publicationDao;
	private Dao<Author, Integer> authorDao;
	
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


	}
	
	public Dao<Author, Integer> getAuthorDao() throws SQLException {
		if (authorDao == null) {
			authorDao = getDao(Author.class);
		}
		return authorDao;
	}

	public Dao<Publication, Integer> getPublicationDao() throws SQLException {
		if (publicationDao == null) {
			publicationDao = getDao(Publication.class);
		}
		return publicationDao;
	}

    @Override
    public void close(){
        super.close();
        publicationDao = null;
        authorDao = null;
    }

}
