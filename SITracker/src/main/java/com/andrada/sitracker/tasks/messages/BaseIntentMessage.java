package com.andrada.sitracker.tasks.messages;

import android.content.Intent;

/**
 * Created by ggodonoga on 11/06/13.
 */
public abstract class BaseIntentMessage extends Intent {

    public BaseIntentMessage() {
        super();
        this.setAction(this.getClass().getCanonicalName());
        this.addCategory(Intent.CATEGORY_DEFAULT);
    }
}
