package com.andrada.sitracker.fragment.components;

import android.content.Context;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.util.DateFormatterUtil;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

/**
 * Created by Gleb on 04.06.13.
 */

@EViewGroup(R.layout.authors_list_item)
public class AuthorItemView extends RelativeLayout {

    @ViewById
    TextView author_title;

    @ViewById
    TextView author_update_date;

    @ViewById
    CheckBox author_updated;

    public AuthorItemView(Context context) {
        super(context);
    }

    public void bind(Author author) {
        author_title.setText(author.getName());
        author_updated.setChecked(author.isUpdated());
        author_update_date.setText(DateFormatterUtil.getFriendlyDateRelativeToToday(author.getUpdateDate()));
    }

}
