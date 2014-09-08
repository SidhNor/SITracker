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

package com.andrada.sitracker.util;


import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.os.ParcelFileDescriptor;

import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.dao.AuthorDao;
import com.andrada.sitracker.db.dao.PublicationDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.events.BackUpRestoredEvent;
import com.j256.ormlite.android.apptools.OpenHelperManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import de.greenrobot.event.EventBus;

import static com.andrada.sitracker.util.LogUtils.LOGD;
import static com.andrada.sitracker.util.LogUtils.makeLogTag;

public class SiBackupAgent extends BackupAgent {

    private static final String TAG = makeLogTag(SiBackupAgent.class);

    private final static String AUTHORS_BACKUP_KEY = "authorsbackupKey";

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
                         ParcelFileDescriptor newState) throws IOException {

        // Create buffer stream and data output stream for our data
        ByteArrayOutputStream bufStream = new ByteArrayOutputStream();
        ObjectOutputStream outWriter = new ObjectOutputStream(bufStream);

        try {
            SiDBHelper helper = new SiDBHelper(this.getApplicationContext());
            PublicationDao pubDao = helper.getPublicationDao();

            List<Publication> publications = pubDao.queryForAll();
            LOGD(TAG, "Backing up publications. Count: " + publications.size());

            // Write structured data
            outWriter.writeObject(publications);

            // Send the data to the Backup Manager via the BackupDataOutput
            byte[] buffer = bufStream.toByteArray();
            int len = buffer.length;
            data.writeEntityHeader(AUTHORS_BACKUP_KEY, len);
            data.writeEntityData(buffer, len);

            FileOutputStream outstream = new FileOutputStream(newState.getFileDescriptor());
            ObjectOutputStream out = new ObjectOutputStream(outstream);
            out.writeObject(publications);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode,
                          ParcelFileDescriptor newState) throws IOException {

        while (data.readNextHeader()) {
            String key = data.getKey();
            int dataSize = data.getDataSize();

            // If the key is ours (for saving top score). Note this key was used when
            // we wrote the backup entity header
            if (AUTHORS_BACKUP_KEY.equals(key)) {
                // Create an input stream for the BackupDataInput
                byte[] dataBuf = new byte[dataSize];
                data.readEntityData(dataBuf, 0, dataSize);
                ByteArrayInputStream baStream = new ByteArrayInputStream(dataBuf);
                ObjectInputStream in = null;

                try {
                    LOGD(TAG, "Starting reading backup data");
                    in = new ObjectInputStream(baStream);
                    Object possiblePubs = in.readObject();
                    if (possiblePubs instanceof List) {
                        final List<Publication> publications = (List<Publication>) possiblePubs;
                        SiDBHelper helper = OpenHelperManager.getHelper(this.getApplicationContext(), SiDBHelper.class);
                        final Map<Long, Author> authorsMap = new HashMap<Long, Author>();
                        for (Publication pub : publications) {
                            if (pub.getAuthor() != null && !authorsMap.containsKey(pub.getAuthor().getId())) {
                                authorsMap.put(pub.getAuthor().getId(), pub.getAuthor());
                            }
                        }
                        LOGD(TAG, "Backup data parsed");

                        final AuthorDao authorDao = helper.getAuthorDao();
                        final PublicationDao pubDao = helper.getPublicationDao();

                        //Write all authors and publications in a trasaction
                        authorDao.callBatchTasks(new Callable<Object>() {
                            @Override
                            public Object call() throws Exception {
                                //Write all the authors to db.
                                for (Author auth : authorsMap.values()) {
                                    authorDao.createOrUpdate(auth);
                                }
                                for (Publication pub : publications) {
                                    pubDao.createOrUpdate(pub);
                                }
                                return null;
                            }
                        });

                        LOGD(TAG, "Backup data persisted");

                        EventBus.getDefault().post(new BackUpRestoredEvent());
                        OpenHelperManager.releaseHelper();
                    }


                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (in != null) {
                        in.close();
                    }
                }

            } else {
                // We don't know this entity key. Skip it. (Shouldn't happen.)
                data.skipEntityData();
            }
        }

    }
}
