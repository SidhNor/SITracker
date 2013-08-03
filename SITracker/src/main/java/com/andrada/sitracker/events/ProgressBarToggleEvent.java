package com.andrada.sitracker.events;

/**
 * Created by ggodonoga on 02/08/13.
 */
public class ProgressBarToggleEvent {

    public final boolean showProgress;

    public ProgressBarToggleEvent(boolean showProgress) {
        this.showProgress = showProgress;
    }
}
