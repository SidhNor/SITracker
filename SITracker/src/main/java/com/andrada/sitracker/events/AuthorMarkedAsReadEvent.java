package com.andrada.sitracker.events;

/**
 * Created by ggodonoga on 11/06/13.
 */
public class AuthorMarkedAsReadEvent {

    public final long authorId;

    public AuthorMarkedAsReadEvent(long authorId) {
        this.authorId = authorId;
    }
}
