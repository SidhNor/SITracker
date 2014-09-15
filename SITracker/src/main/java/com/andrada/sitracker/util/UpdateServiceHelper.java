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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.contracts.SIPrefs_;
import com.andrada.sitracker.tasks.UpdateAuthorsTask_;

public class UpdateServiceHelper {

    public static boolean isServiceRunning(Context context) {
        Intent updateIntent = UpdateAuthorsTask_.intent(context.getApplicationContext()).get();
        return PendingIntent.getService(context.getApplicationContext(), 0, updateIntent, PendingIntent.FLAG_NO_CREATE) != null;
    }

    public static boolean scheduleUpdates(Context context) {
        SIPrefs_ prefs = new SIPrefs_(context);
        Intent updateIntent = UpdateAuthorsTask_.intent(context.getApplicationContext()).get();
        PendingIntent pendingInt = PendingIntent.getService(context.getApplicationContext(), 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        long updateInterval = Long.parseLong(prefs.updateInterval().get());
        AlarmManager alarmManager = ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE));
        if (prefs.updatesEnabled().get()) {
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                    //Delay update for 10 minutes to allow complete restore of backup if exists
                    System.currentTimeMillis() + Constants.STARTUP_UPDATE_DELAY,
                    updateInterval,
                    pendingInt);
        }

        return prefs.updatesEnabled().get();
    }

    public static void cancelUpdates(Context context) {
        Intent updateIntent = UpdateAuthorsTask_.intent(context.getApplicationContext()).get();
        AlarmManager alarmManager = ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE));
        alarmManager.cancel(PendingIntent.getService(context.getApplicationContext(), 0, updateIntent, PendingIntent.FLAG_NO_CREATE));
    }
}
