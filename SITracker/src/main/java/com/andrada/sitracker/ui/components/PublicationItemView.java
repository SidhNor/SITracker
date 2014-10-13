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

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.IsNewItemTappedListener;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.ui.widget.TouchDelegateRelativeLayout;
import com.andrada.sitracker.util.DateFormatterUtil;
import com.andrada.sitracker.util.SamlibPageHelper;
import com.andrada.sitracker.util.UIUtils;
import com.bumptech.glide.Glide;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.jetbrains.annotations.NotNull;

@EViewGroup(R.layout.publications_item)
public class PublicationItemView extends TouchDelegateRelativeLayout {

    @ViewById
    TextView item_title;

    @ViewById
    TextView item_update_date;

    @ViewById
    ImageButton item_updated;

    @ViewById
    ViewGroup downloadProgress;

    @ViewById
    TextView item_description;

    @ViewById
    ImageView publication_image;

    @ViewById(R.id.dim_verlay)
    View dimOverlay;

    @ViewById
    TextView itemSize;

    @ViewById
    ViewGroup backgroundPane;

    Animation scaleFadeOutAnim;
    Animation fadeInAnim;

    private boolean mIsNew = false;

    private IsNewItemTappedListener mListener;

    public PublicationItemView(@NotNull Context context) {
        super(context);

    }

    @AfterViews
    void afterViews() {
        this.delegatedTouchViews.put(
                ViewConfig.wholeRight(),
                item_updated);
        item_description.setMaxLines(3);
        scaleFadeOutAnim = AnimationUtils.loadAnimation(getContext(), R.anim.item_fade_scale_down);
        scaleFadeOutAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                backgroundPane.setVisibility(GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        fadeInAnim = AnimationUtils.loadAnimation(getContext(), R.anim.item_fade_in);
    }

    public void setListener(IsNewItemTappedListener listener) {
        mListener = listener;
    }

    public void bind(@NotNull Publication publication, boolean loadImages) {
        mIsNew = publication.getNew();
        item_title.setText(publication.getName());
        item_updated.setImageResource(mIsNew ? R.drawable.star_selected : R.drawable.star_unselected);
        item_updated.setTag(publication);
        item_update_date.setText(
                DateFormatterUtil.getFriendlyDateRelativeToToday(publication.getUpdateDate(),
                        getResources().getConfiguration().locale));

        if (this.getContext() != null && loadImages && publication.getImageUrl() != null) {
            publication_image.setVisibility(VISIBLE);
            Glide.with(this.getContext())
                    .load(publication.getImageUrl())
                    .fitCenter()
                    .placeholder(R.drawable.blank_book)
                    .crossFade()
                    .into(publication_image);
        } else {
            publication_image.setVisibility(GONE);
        }

        if (publication.getUpdatesIgnored()) {
            dimOverlay.setVisibility(VISIBLE);
        } else {
            dimOverlay.setVisibility(GONE);
        }

        UIUtils.setTextMaybeHtml(item_description,
                SamlibPageHelper.stripDescriptionOfImages(publication.getDescription()));


        StringBuilder builder = new StringBuilder();
        int oldSize = publication.getOldSize();
        int newSize = publication.getSize();
        if (oldSize == 0 || oldSize == newSize) {
            builder.append(newSize);
        } else {
            builder.append(oldSize);
            if ((newSize - oldSize) > 0) {
                builder.append('+');
            }
            builder.append(newSize - oldSize);
        }
        builder.append("kb");
        itemSize.setText(builder.toString());

        if (publication.getLoading()) {
            if (downloadProgress.getVisibility() == GONE) {
                downloadProgress.setVisibility(VISIBLE);
                downloadProgress.startAnimation(fadeInAnim);
                backgroundPane.startAnimation(scaleFadeOutAnim);
            }
        } else {
            downloadProgress.clearAnimation();
            backgroundPane.clearAnimation();
            downloadProgress.setVisibility(GONE);
            backgroundPane.setVisibility(VISIBLE);
        }

    }

    @Override
    protected void onDelegatedTouchViewClicked(@NotNull View view) {
        if (mListener != null && view.getId() == R.id.item_updated) {
            mIsNew = false;
            item_updated.setImageResource(R.drawable.star_unselected);
            mListener.onIsNewItemTapped(view);
        }
    }

    @Override
    protected void onDelegatedTouchViewDown(@NotNull View view) {
        if (mIsNew && view.getId() == R.id.item_updated) {
            item_updated.setImageResource(R.drawable.star_selected_focused);
        }
    }

    @Override
    protected void onDelegatedTouchViewCancel(@NotNull View view) {
        //If we are not new, just ignore everything
        if (mIsNew && view.getId() == R.id.item_updated) {
            item_updated.setImageResource(R.drawable.star_selected);
        }
    }
}
