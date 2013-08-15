/*
 * Copyright 2013 Gleb Godonoga.
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

package com.andrada.sitracker.tasks.receivers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.tasks.messages.UpdateFailedIntentMessage;
import com.andrada.sitracker.tasks.messages.UpdateSuccessfulIntentMessage;
import com.andrada.sitracker.ui.HomeActivity_;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;


public class UpdateStatusNotificationReceiver extends BroadcastReceiver {

    private final static int UPDATE_SUCCESS_NOTIFICATION_ID = 1;
    private final static int UPDATE_FAIL_NOTIFICATION_ID = 2;

    @Override
    public void onReceive(Context context, Intent intent) {
        //See if there is something we can notify
        if (intent.getAction().equals(UpdateSuccessfulIntentMessage.SUCCESS_MESSAGE)) {
            int updatedAuthors = intent.getIntExtra(Constants.NUMBER_OF_UPDATED_AUTHORS, 0);
            if (updatedAuthors > 0) {
                //Notify that update successful
                sendNotification(updatedAuthors, context);
            }

        } else if (intent.getAction().equals(UpdateFailedIntentMessage.FAILED_MESSAGE)) {
            //Notify that update failed
            Tracker myTracker = EasyTracker.getTracker();      // Get a reference to tracker.
            myTracker.sendException(UpdateFailedIntentMessage.FAILED_MESSAGE, false);
        }

    }

    private void sendNotification(int number, Context context) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.notification)
                        .setContentTitle(context.getResources().getString(R.string.notification_title))
                        .setContentText(context.getResources().getQuantityString(R.plurals.authors_updated, number, number))
                        .setAutoCancel(true)
                        .setOnlyAlertOnce(true)
                        .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
                        .setNumber(number);
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(context, HomeActivity_.class);
        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(HomeActivity_.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(UPDATE_SUCCESS_NOTIFICATION_ID, mBuilder.build());
    }
}
