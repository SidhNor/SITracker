/*
 * Copyright 2014 Gleb Godonoga.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrada.sitracker.ui.debug.actions;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.andrada.sitracker.R;
import com.andrada.sitracker.ui.SiMainActivity_;
import com.andrada.sitracker.ui.debug.DebugAction;

import java.util.ArrayList;
import java.util.List;

public class ShowAuthorsUpdatedNotificationALotAction implements DebugAction {

    @Override
    public void run(Context context, Callback callback) {
        List<String> updatedAuthorNames = new ArrayList<String>();
        for (int i = 0; i < 14; i++) {
            updatedAuthorNames.add("Автор Тест Тестович" + (i + 1));
        }
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        int maxLines = 4;
        if (updatedAuthorNames.size() < maxLines) {
            maxLines = updatedAuthorNames.size();
        }
        for (int i = 0; i < maxLines; i++) {
            inboxStyle.addLine(updatedAuthorNames.get(i));
        }
        if (updatedAuthorNames.size() > 4) {
            inboxStyle.setSummaryText(context.getString(R.string.notification_more_summary, updatedAuthorNames.size() - 4));
        }
        Bitmap icon = BitmapFactory.decodeResource(context.getResources(),
                R.mipmap.ic_launcher);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.notification)
                        .setColor(context.getResources().getColor(R.color.theme_primary))
                        .setPriority(Notification.PRIORITY_LOW)
                        .setContentTitle(context.getResources().getString(R.string.notification_title))
                        .setContentText(context.getResources().getQuantityString(R.plurals.authors_updated, updatedAuthorNames.size(), updatedAuthorNames.size()))
                        .setAutoCancel(true)
                        .setOnlyAlertOnce(true)
                        .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
                        .setNumber(14)
                        .setStyle(inboxStyle);


        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(context, SiMainActivity_.class);
        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(SiMainActivity_.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, mBuilder.build());
    }

    @Override
    public String getLabel() {
        return "Show 14 authors updated notification";
    }
}
