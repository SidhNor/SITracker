package com.andrada.sitracker.tasks.filters;

import com.andrada.sitracker.tasks.messages.UpdateFailedIntentMessage;
import com.andrada.sitracker.tasks.messages.UpdateSuccessfulIntentMessage;

/**
 * Created by ggodonoga on 11/06/13.
 */
public class UpdateStatusMessageFilter extends BaseIntentMessageFilter {

    public UpdateStatusMessageFilter() {
        super();
        this.addAction(UpdateSuccessfulIntentMessage.getMessageName());
        this.addAction(UpdateFailedIntentMessage.getMessageName());
    }
}
