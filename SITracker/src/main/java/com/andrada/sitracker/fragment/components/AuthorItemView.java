/*
 * Copyright 2013 Gleb Godonoga.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrada.sitracker.fragment.components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.IsNewItemTappedListener;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.ui.widget.CheckedRelativeLayout;
import com.andrada.sitracker.util.DateFormatterUtil;
import com.andrada.sitracker.util.UIUtils;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

/**
 * Created by Gleb on 04.06.13.
 */

@EViewGroup(R.layout.authors_list_item)
public class AuthorItemView extends CheckedRelativeLayout {

    @ViewById
    TextView author_title;

    @ViewById
    TextView author_update_date;

    @ViewById
    ImageButton author_updated;

    private boolean mIsNew = false;

    private IsNewItemTappedListener mListener;

    private final int REGULAR_BACKGROUND = R.drawable.authors_list_item_selector_normal;
    private final int NEW_BACKGROUND = R.drawable.authors_list_item_selector_new;

    private int currentBackground = REGULAR_BACKGROUND;

    public AuthorItemView(Context context) {
        super(context);
        this.setBackgroundResource(currentBackground);
        setOldNewBackgrounds();
    }

    @AfterViews
    void afterViews() {
        this.delegatedTouchViews.put(
                ViewConfig.wholeRight(),
                author_updated);
    }

    public void setListener(IsNewItemTappedListener listener) {
        mListener = listener;
    }

    @SuppressLint("NewApi")
    public void bind(Author author, boolean isSelected) {
        if (UIUtils.hasHoneycomb()) {
            this.setActivated(isSelected);
        } else {
            this.setChecked(isSelected);
        }
        mIsNew = author.isUpdated();
        author_updated.setTag(author);
        author_title.setText(author.getName());
        author_update_date.setText(
                DateFormatterUtil.getFriendlyDateRelativeToToday(author.getUpdateDate(),
                        getResources().getConfiguration().locale));
        setOldNewBackgrounds();
    }

    @Override
    protected void onDelegatedTouchViewClicked(View view) {
        if (mListener != null && view.getId() == R.id.author_updated && mIsNew) {
            mIsNew = false;
            setOldNewBackgrounds();
            mListener.onIsNewItemTapped(view);
        }
    }

    @Override
    protected void onDelegatedTouchViewDown(View view) {
        if (mIsNew) {
            author_updated.setImageResource(R.drawable.star_selected_focused);
        }
    }

    @Override
    protected void onDelegatedTouchViewCancel(View view) {
        //If we are not new, just ignore everything
        if (mIsNew) {
            author_updated.setImageResource(R.drawable.star_selected);
        }
    }

    private void setOldNewBackgrounds() {
        if (mIsNew && currentBackground != NEW_BACKGROUND) {
            this.setBackgroundResource(NEW_BACKGROUND);
            author_updated.setImageResource(R.drawable.star_selected);
            currentBackground = NEW_BACKGROUND;
        } else if (!mIsNew && currentBackground != REGULAR_BACKGROUND) {
            this.setBackgroundResource(REGULAR_BACKGROUND);
            author_updated.setImageResource(R.drawable.star_unselected);
            currentBackground = REGULAR_BACKGROUND;
        }
    }

}
