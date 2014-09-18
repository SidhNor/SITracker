package com.andrada.sitracker.ui.components;

import android.content.Context;
import android.text.Html;
import android.text.SpannableString;
import android.widget.TextView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.db.beans.SearchedAuthor;
import com.andrada.sitracker.ui.widget.CheckedRelativeLayout;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.jetbrains.annotations.NotNull;

@EViewGroup(R.layout.search_author_list_item)
public class SearchAuthorItemView extends CheckedRelativeLayout {

    @ViewById
    TextView authorName;

    @ViewById
    TextView authorUrl;

    @ViewById
    TextView matchDescription;

    public SearchAuthorItemView(@NotNull Context context) {
        super(context);
    }

    public void bind(@NotNull SearchedAuthor author) {
        authorName.setText(author.getAuthorName());
        authorUrl.setText(author.getAuthorUrl());
        SpannableString spannableString = new SpannableString(Html.fromHtml(author.getContextDescription()));
        matchDescription.setText(spannableString);
    }
}
