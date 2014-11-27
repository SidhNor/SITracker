package com.andrada.sitracker.ui.components;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.andrada.sitracker.R;
import com.andrada.sitracker.db.beans.Publication;

import org.androidannotations.annotations.EViewGroup;

@EViewGroup(R.layout.newpub_list_item)
public class NewPubItemView extends RelativeLayout {
    public NewPubItemView(Context context) {
        super(context);
    }

    public NewPubItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NewPubItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void bind(Publication pub) {

    }
}
