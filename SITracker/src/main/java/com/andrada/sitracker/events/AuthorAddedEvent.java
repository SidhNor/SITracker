package com.andrada.sitracker.events;

/**
 * Created by ggodonoga on 02/08/13.
 */
public class AuthorAddedEvent {

    public final String message;

    public AuthorAddedEvent(String message) {
        this.message = message;
    }
}
