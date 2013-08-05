package com.andrada.sitracker.tasks.filters;

import android.content.Intent;
import android.content.IntentFilter;

/**
 * Created by ggodonoga on 11/06/13.
 */
public abstract class BaseIntentMessageFilter extends IntentFilter {

    public BaseIntentMessageFilter() {
        super();
        addCategory(Intent.CATEGORY_DEFAULT);
    }

}
