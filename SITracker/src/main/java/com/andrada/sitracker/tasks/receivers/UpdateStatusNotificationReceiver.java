package com.andrada.sitracker.tasks.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * Created by ggodonoga on 22/07/13.
 */

public class UpdateStatusNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //See if there is something we can notify
        //TODO post notification here
        Toast.makeText(context, "Update has been run on SIInformer", Toast.LENGTH_SHORT).show();
    }
}
