package com.andrada.sitracker.task;


import android.app.IntentService;
import android.content.Intent;

import com.andrada.sitracker.db.manager.SiDBHelper;
import com.j256.ormlite.android.apptools.OpenHelperManager;

import org.androidannotations.annotations.EService;

/**
 * Created by Gleb on 03.06.13.
 */

@EService
public class UpdateAuthorsIntentService extends IntentService  {

    public UpdateAuthorsIntentService() {
        super("UpdateAuthorsIntentService");
    }

    private volatile SiDBHelper helper;
    private volatile boolean created = false;
    private volatile boolean destroyed = false;
    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns, IntentService
     * stops the service, as appropriate.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        //Check for updates
        //Update DB for new items.
    }

    @Override
         public void onCreate() {
        if (helper == null) {
            helper = OpenHelperManager.getHelper(this, SiDBHelper.class);
        }
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        OpenHelperManager.releaseHelper();
        helper = null;
    }

}
