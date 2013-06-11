package com.andrada.sitracker.tasks.filters;

import com.andrada.sitracker.tasks.messages.AuthorMarkedAsReadMessage;
import com.andrada.sitracker.tasks.messages.PublicationMarkedAsReadMessage;

/**
 * Created by ggodonoga on 11/06/13.
 */
public class MarkAsReadMessageFilter extends BaseIntentMessageFilter {

    public MarkAsReadMessageFilter() {
        super();
        this.addAction(AuthorMarkedAsReadMessage.getMessageName());
        this.addAction(PublicationMarkedAsReadMessage.getMessageName());
    }
}
