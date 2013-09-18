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

package com.andrada.sitracker.ui.components;

import android.content.Context;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.ui.widget.CheckedRelativeLayout;
import com.andrada.sitracker.util.DateFormatterUtil;
import com.andrada.sitracker.util.ImageLoader;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

@EViewGroup(R.layout.new_pub_list_item)
public class NewPubItemView extends CheckedRelativeLayout {

    @ViewById
    TextView item_title;

    @ViewById
    TextView item_update_date;

    @ViewById
    TextView category_title;

    @ViewById
    TextView author_title;

    @ViewById
    ImageButton item_updated;

    @ViewById
    ViewGroup downloadProgress;

    @ViewById
    ImageView publication_image;

    @ViewById
    TextView itemSize;

    @ViewById
    ViewGroup backgroundPane;

    Animation scaleFadeOutAnim;
    Animation fadeInAnim;

    private boolean mIsNew = false;

    public NewPubItemView(Context context) {
        super(context);
    }

    public void bind(Publication publication, ImageLoader loader) {
        mIsNew = publication.getNew();
        item_title.setText(publication.getName());
        category_title.setText(publication.getCategory());
        author_title.setText(publication.getAuthor().getName());
        item_updated.setImageResource(mIsNew ? R.drawable.star_selected : R.drawable.star_unselected);
        item_updated.setTag(publication);
        item_update_date.setText(
                DateFormatterUtil.getFriendlyDateRelativeToToday(publication.getUpdateDate(),
                        getResources().getConfiguration().locale));
        if (loader != null && publication.getImageUrl() != null) {
            publication_image.setVisibility(VISIBLE);
            loader.get(publication.getImageUrl(), publication_image);
        } else {
            publication_image.setVisibility(GONE);
        }

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
        }
    }
}
