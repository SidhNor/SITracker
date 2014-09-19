package com.andrada.sitracker.ui.components;

import android.content.Context;
import android.text.Html;
import android.text.SpannableString;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.db.beans.SearchedAuthor;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.jetbrains.annotations.NotNull;

@EViewGroup(R.layout.search_author_list_item)
public class SearchAuthorItemView extends RelativeLayout {

    @ViewById
    TextView authorName;

    @ViewById
    TextView authorUrl;

    @ViewById
    TextView matchDescription;

    @ViewById
    TextView actionText;

    @ViewById
    ImageView actionImg;

    public SearchAuthorItemView(@NotNull Context context) {
        super(context);
    }

    public void bind(@NotNull SearchedAuthor author) {
        if (author.isAdded()) {
            actionText.setText(getContext().getString(R.string.already_in_library));
            actionImg.setImageResource(R.drawable.in_library);
        } else {
            actionText.setText(getContext().getString(R.string.tap_to_add_to_library));
            actionImg.setImageResource(R.drawable.not_in_library);
        }
        authorName.setText(author.getAuthorName());
        authorUrl.setText(author.getAuthorUrl().replace("http://", "").replace("/indextitle.shtml", ""));
        SpannableString spannableString = new SpannableString(Html.fromHtml(author.getContextDescription()));
        matchDescription.setText(spannableString);
    }
}
