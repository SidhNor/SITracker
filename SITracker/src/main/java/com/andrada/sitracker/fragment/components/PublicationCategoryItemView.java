package com.andrada.sitracker.fragment.components;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.andrada.sitracker.R;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

/**
 * Created by ggodonoga on 05/06/13.
 */
@EViewGroup(R.layout.publications_category)
public class PublicationCategoryItemView extends LinearLayout {

    @ViewById
    TextView category_title;

    public PublicationCategoryItemView(Context context) {
        super(context);
    }

    public void bind(String category) {
        category_title.setText(category);
    }
}
