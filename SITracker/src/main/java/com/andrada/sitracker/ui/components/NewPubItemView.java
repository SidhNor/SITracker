package com.andrada.sitracker.ui.components;

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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;


//TODO Make an abstract class that implements new item handling to reuse PublicationItemView
@EViewGroup(R.layout.newpub_list_item)
public class NewPubItemView extends TouchDelegateRelativeLayout {

    @ViewById
    TextView item_title;

    @ViewById
    TextView item_update_date;

    @ViewById
    ImageButton item_updated;

    @ViewById
    TextView author_name;

    @ViewById
    TextView item_description;

    @ViewById
    ImageView publication_image;

    @ViewById
    TextView itemSize;

    private boolean mIsNew = false;

    private IsNewItemTappedListener mListener;

    public NewPubItemView(Context context) {
        super(context);
    }

    public NewPubItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NewPubItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @AfterViews
    void afterViews() {
        this.delegatedTouchViews.put(
                TouchDelegateRelativeLayout.ViewConfig.wholeRight(),
                item_updated);
    }


    public void setListener(IsNewItemTappedListener listener) {
        mListener = listener;
    }

    public void bind(Publication publication, boolean loadImages) {
        mIsNew = publication.getNew();
        item_title.setText(publication.getName());
        author_name.setText(publication.getAuthor().getName());
        item_updated
                .setImageResource(mIsNew ? R.drawable.star_selected : R.drawable.star_unselected);
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
