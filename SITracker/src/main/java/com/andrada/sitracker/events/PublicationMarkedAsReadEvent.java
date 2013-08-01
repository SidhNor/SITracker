package com.andrada.sitracker.events;

/**
 * Created by ggodonoga on 11/06/13.
 */
public class PublicationMarkedAsReadEvent {

    public final long publicationId;

    public PublicationMarkedAsReadEvent(long publicationId) {
        this.publicationId = publicationId;
    }
}
