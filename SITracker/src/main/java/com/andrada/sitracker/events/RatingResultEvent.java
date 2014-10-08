package com.andrada.sitracker.events;

public class RatingResultEvent {

    public final boolean ratingSubmissionResult;

    public RatingResultEvent(boolean result) {
        ratingSubmissionResult = result;
    }
}
