package com.andrada.sitracker.fragment.components;

import android.content.Context;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.IsNewItemTappedListener;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.util.DateFormatterUtil;
import com.andrada.sitracker.util.TouchDelegateRelativeLayout;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

/**
 * Created by ggodonoga on 05/06/13.
 */
@EViewGroup(R.layout.publications_item)
public class PublicationItemView extends TouchDelegateRelativeLayout {

    @ViewById
    TextView item_title;

    @ViewById
    TextView item_update_date;

    @ViewById
    ImageButton item_updated;

    @ViewById
    TextView item_description;

    @ViewById
    View publication_item_divider;

    @ViewById
    TextView itemSize;

    private boolean mIsNew = false;

    private IsNewItemTappedListener mListener;

    public PublicationItemView(Context context) {
        super(context);
    }

    @AfterViews
    void afterViews() {
        this.delegatedTouchViews.put(
                ViewConfig.wholeRight(),
                item_updated);
    }

    public void setListener(IsNewItemTappedListener listener) {
        mListener = listener;
    }

    public void bind(Publication publication, Boolean isLast) {
        mIsNew = publication.getNew();
        item_title.setText(publication.getName());
        item_updated.setImageResource(mIsNew ? R.drawable.star_selected : R.drawable.star_unselected);
        item_updated.setTag(publication);
        item_update_date.setText(DateFormatterUtil.getFriendlyDateRelativeToToday(publication.getUpdateDate()));
        item_description.setText(publication.getDescription());

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

        if (isLast) {
            publication_item_divider.setVisibility(View.GONE);
        } else {
            publication_item_divider.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDelegatedTouchViewClicked(View view) {
        if (mListener != null && view.getId() == R.id.item_updated) {
            mIsNew = false;
            item_updated.setImageResource(R.drawable.star_unselected);
            mListener.onIsNewItemTapped(view);
        }
    }

    @Override
    protected void onDelegatedTouchViewDown(View view) {
        if (mIsNew && view.getId() == R.id.item_updated) {
            item_updated.setImageResource(R.drawable.star_selected_focused);
        }
    }

    @Override
    protected void onDelegatedTouchViewCancel(View view) {
        //If we are not new, just ignore everything
        if (mIsNew && view.getId() == R.id.item_updated) {
            item_updated.setImageResource(mIsNew ? R.drawable.star_selected : R.drawable.star_unselected);
        }
    }
}
