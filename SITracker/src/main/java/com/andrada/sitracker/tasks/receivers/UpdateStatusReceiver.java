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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.andrada.sitracker.contracts.AuthorUpdateStatusListener;
import com.andrada.sitracker.tasks.messages.UpdateFailedIntentMessage;
import com.andrada.sitracker.tasks.messages.UpdateSuccessfulIntentMessage;

public class UpdateStatusReceiver extends BroadcastReceiver {

    private AuthorUpdateStatusListener mListener = null;

    public UpdateStatusReceiver(AuthorUpdateStatusListener listener) {
        mListener = listener;
    }

    public UpdateStatusReceiver() {
        super();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //See if there is something we can notify
        if (mListener != null) {
            String action = intent.getAction();
            if (action.equals(UpdateSuccessfulIntentMessage.getMessageName())) {
                mListener.onAuthorsUpdated();
            } else if (action.equals(UpdateFailedIntentMessage.getMessageName())) {
                mListener.onAuthorsUpdateFailed();
            }
            this.abortBroadcast();
        }
    }
}
