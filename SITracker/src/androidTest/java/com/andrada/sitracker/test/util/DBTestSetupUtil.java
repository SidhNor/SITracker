package com.andrada.sitracker.test.util;

import android.content.Context;

import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.dao.AuthorDao;
import com.andrada.sitracker.db.dao.PublicationDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.j256.ormlite.android.apptools.OpenHelperManager;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * Created by ggodonoga on 06/08/13.
 */
public class DBTestSetupUtil {

    public static final String[] AUTHOR_NAMES = {
            "Test Author 1", "Test Author 2", "Test Author 3", "Test Author 4"
    };

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public static final String[] AUTHOR_UPDATE_DATES = {
            "2013-08-12 21:42", "2013-08-12 14:17", "2011-08-12 14:17", "2013-07-12 14:10"
    };

    public static final int AUTHORS_COUNT = AUTHOR_NAMES.length;

    /**
     * Clear all db up
     *
     * @param context Android Application context
     * @throws SQLException
     */
    public static void clearDb(Context context) throws SQLException {
        SiDBHelper helper = OpenHelperManager.getHelper(context, SiDBHelper.class);
        helper.getAuthorDao().delete(helper.getAuthorDao().queryForAll());
        OpenHelperManager.releaseHelper();
    }

    /**
     * Populate DB with authors
     *
     * @param context Android Application context
     * @throws SQLException
     */
    public static void populateDBWithAuthors(Context context) throws SQLException, ParseException {
        SiDBHelper helper = OpenHelperManager.getHelper(context, SiDBHelper.class);
        final Random rand = new Random();
        AuthorDao authorsDao = helper.getAuthorDao();
        PublicationDao publicationDao = helper.getPublicationDao();

        for (int i = 0; i < AUTHORS_COUNT; i++) {
            Author author = new Author();
            author.setName(AUTHOR_NAMES[i]);
            author.setUpdateDate(dateFormat.parse(AUTHOR_UPDATE_DATES[i]));
            author.setUrl(AUTHOR_NAMES[i]);
            authorsDao.create(author);
            for (int j = 0; j < 5; j++) {
                Publication item = new Publication();
                item.setAuthor(author);
                item.setUpdateDate(new Date());
                item.setUrl(AUTHOR_NAMES[i] + "/" + j);
                item.setName(AUTHOR_NAMES[i] + " Publication " + j);
                item.setSize(rand.nextInt());
                item.setCategory("Category " + j % 2);
                //Group 11 - Description
                item.setDescription("Description " + (i + j));
                publicationDao.create(item);
            }
        }

        OpenHelperManager.releaseHelper();
    }
}
