package com.andrada.sitracker.tasks.messages;

/**
 * Created by ggodonoga on 11/06/13.
 */
public class PublicationMarkedAsReadMessage extends BaseIntentMessage {

    public PublicationMarkedAsReadMessage(long publicationId) {
        super();
        putExtra("publicationId", publicationId);
    }
    public static String getMessageName() {
        return PublicationMarkedAsReadMessage.class.getCanonicalName();
    }
}
