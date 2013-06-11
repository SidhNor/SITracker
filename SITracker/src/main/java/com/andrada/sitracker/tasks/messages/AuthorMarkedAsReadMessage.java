package com.andrada.sitracker.tasks.messages;

/**
 * Created by ggodonoga on 11/06/13.
 */
public class AuthorMarkedAsReadMessage extends BaseIntentMessage {

    public AuthorMarkedAsReadMessage(long authorId) {
        super();
        putExtra("authorId", authorId);
    }
    public static String getMessageName() {
        return AuthorMarkedAsReadMessage.class.getCanonicalName();
    }
}
