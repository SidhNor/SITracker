/*
 * Copyright 2014 Gleb Godonoga.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrada.sitracker.ui.components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.IsNewItemTappedListener;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.ui.widget.CheckedRelativeLayout;
import com.andrada.sitracker.ui.widget.LetterTileProvider;
import com.andrada.sitracker.util.DateFormatterUtil;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.jetbrains.annotations.NotNull;

@EViewGroup(R.layout.authors_list_item)
public class AuthorItemView extends CheckedRelativeLayout {

    @ViewById
    TextView author_title;
    @ViewById
    TextView author_update_date;
    @ViewById
    ImageButton author_updated;
    @ViewById
    ImageView author_image;
    private boolean mIsNew = false;
    private boolean mPreviousNewState = false;
    private IsNewItemTappedListener mListener;

    private final int tileSize;
    private final LetterTileProvider tileProvider;

    public AuthorItemView(@NotNull Context context) {
        super(context);
        this.setBackgroundResource(R.drawable.authors_list_item_selector_normal);
        tileSize = getResources().getDimensionPixelSize(R.dimen.avatar_image_height);
        tileProvider = new LetterTileProvider(getContext());
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
    public void bind(@NotNull Author author, boolean isSelected) {

        final Bitmap letterTile = tileProvider.getLetterTile(author.getName(), author.getUrlId(), tileSize, tileSize);

        if (author.getAuthorImageUrl() != null && getContext() != null) {
            Glide.with(getContext())
                    .load(author.getAuthorImageUrl())
                    .placeholder(R.drawable.avatar_placeholder_gray)
                    .listener(new RequestListener<String, GlideDrawable>() {
                        @Override
                        public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                            author_image.setImageBitmap(letterTile);
                            return true;
                        }

                        @Override
                        public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(author_image);
        } else {
            author_image.setImageBitmap(letterTile);
        }


        //TODO Activate only in case its a table
        //UIUtils.isTablet(getContext());
        this.setActivated(isSelected);

        mIsNew = author.getNew();
        author_updated.setTag(author);
        author_title.setText(author.getName());
        author_update_date.setText(
                DateFormatterUtil.getFriendlyDateRelativeToToday(author.getUpdateDate(),
                        getResources().getConfiguration().locale));
        setOldNewStates();
    }

    @Override
    protected void onDelegatedTouchViewClicked(@NotNull View view) {
        if (mListener != null && view.getId() == R.id.author_updated && mIsNew) {
            mIsNew = false;
            setOldNewStates();
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

    private void setOldNewStates() {
        //TODO if new - make author title bold and update date text color in theme primary
        if (mIsNew && !mPreviousNewState) {
            author_title.setTypeface(null, Typeface.BOLD);
            author_update_date.setTypeface(null, Typeface.BOLD);
            author_update_date.setTextColor(getResources().getColor(R.color.accent_blue));
            author_updated.setImageResource(R.drawable.star_selected);
            mPreviousNewState = true;
        } else if (!mIsNew && mPreviousNewState) {
            author_title.setTypeface(null, Typeface.NORMAL);
            author_update_date.setTypeface(null, Typeface.NORMAL);
            author_update_date.setTextColor(getResources().getColor(R.color.body_text_1));
            author_updated.setImageResource(R.drawable.star_unselected);
            mPreviousNewState = false;
        }
    }

}
