package com.andrada.sitracker.tasks.messages;

/**
 * Created by ggodonoga on 11/06/13.
 */
public class UpdateSuccessfulIntentMessage extends BaseIntentMessage {

    public static final String SUCCESS_MESSAGE = "com.andrada.sitracker.UPDATE_SUCCESS_ACTION";

    public UpdateSuccessfulIntentMessage() {
        super();
        this.setAction(SUCCESS_MESSAGE);
    }

    public static String getMessageName() {
        return SUCCESS_MESSAGE;
    }
}
