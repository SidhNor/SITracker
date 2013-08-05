package com.andrada.sitracker.tasks.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.andrada.sitracker.contracts.AuthorUpdateStatusListener;
import com.andrada.sitracker.tasks.messages.UpdateFailedIntentMessage;
import com.andrada.sitracker.tasks.messages.UpdateSuccessfulIntentMessage;

/**
 * Created by ggodonoga on 06/06/13.
 */

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
