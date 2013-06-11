package com.andrada.sitracker.tasks.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.andrada.sitracker.contracts.AuthorMarkedAsReadListener;
import com.andrada.sitracker.contracts.PublicationMarkedAsReadListener;
import com.andrada.sitracker.tasks.messages.AuthorMarkedAsReadMessage;
import com.andrada.sitracker.tasks.messages.PublicationMarkedAsReadMessage;

import org.androidannotations.annotations.EReceiver;

/**
 * Created by ggodonoga on 11/06/13.
 */
@EReceiver
public class MarkedAsReadReceiver extends BroadcastReceiver {

    private AuthorMarkedAsReadListener mAuthorListener = null;
    private PublicationMarkedAsReadListener mPubListener;

    public MarkedAsReadReceiver(AuthorMarkedAsReadListener authListener, PublicationMarkedAsReadListener pubListener) {
        mAuthorListener = authListener;
        mPubListener = pubListener;
    }

    public MarkedAsReadReceiver() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //See if there is something we can notify
        String action = intent.getAction();
        long id;
        if (action.equals(AuthorMarkedAsReadMessage.getMessageName())) {
            id = intent.getLongExtra("authorId", -1);
            mAuthorListener.onAuthorMarkedAsRead(id);
        } else if (action.equals(PublicationMarkedAsReadMessage.getMessageName())) {
            id = intent.getLongExtra("publicationId", -1);
            mPubListener.onPublicationMarkedAsRead(id);
        }

    }
}
