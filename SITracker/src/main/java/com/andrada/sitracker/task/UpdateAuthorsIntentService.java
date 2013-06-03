package com.andrada.sitracker.task;


import android.app.IntentService;
import android.content.Intent;

/**
 * Created by Gleb on 03.06.13.
 */
public class UpdateAuthorsIntentService extends IntentService  {

    public UpdateAuthorsIntentService() {
        super("UpdateAuthorsIntentService");
    }

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
}
