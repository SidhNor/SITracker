package com.andrada.sitracker.events;

/**
 * Created by ggodonoga on 01/08/13.
 */
public class AuthorSelectedEvent {
    public final long authorId;

    public AuthorSelectedEvent(long authorId) {
        this.authorId = authorId;
    }
}
