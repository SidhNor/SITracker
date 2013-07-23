package com.andrada.sitracker.tasks.messages;

/**
 * Created by ggodonoga on 11/06/13.
 */
public class UpdateFailedIntentMessage extends BaseIntentMessage {

    public static final String FAILED_MESSAGE = "com.andrada.sitracker.UPDATE_FAILED_ACTION";

    public UpdateFailedIntentMessage() {
        super();
        this.setAction(FAILED_MESSAGE);
    }

    public static String getMessageName() {
        return FAILED_MESSAGE;
    }

}
