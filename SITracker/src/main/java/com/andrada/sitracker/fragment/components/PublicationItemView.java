package com.andrada.sitracker.fragment.components;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.IsNewItemTappedListener;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.util.DateFormatterUtil;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

/**
 * Created by ggodonoga on 05/06/13.
 */
@EViewGroup(R.layout.publications_item)
public class PublicationItemView extends RelativeLayout {

    @ViewById
    TextView item_title;

    @ViewById
    TextView item_update_date;

    @ViewById
    CheckBox item_updated;

    @ViewById
    TextView item_description;

    @ViewById
    View publication_item_divider;

    @ViewById
    TextView itemSize;

    private IsNewItemTappedListener mListener;


    public PublicationItemView(Context context) {
        super(context);
    }


    @Click(R.id.item_updated)
    void publicationDismissUpdates(View checkBox) {
        if (mListener != null) {
            mListener.onIsNewItemTapped(checkBox);
        }
    }

    public void setListener(IsNewItemTappedListener listener) {
        mListener = listener;
    }

    public void bind(Publication publication, Boolean isLast) {
        item_title.setText(publication.getName());
        item_updated.setChecked(publication.getNew());
        item_update_date.setText(DateFormatterUtil.getFriendlyDateRelativeToToday(publication.getUpdateDate()));
        item_description.setText(publication.getDescription());

        StringBuilder builder = new StringBuilder();
        int oldSize = publication.getOldSize();
        int newSize = publication.getSize();
        if (oldSize == 0 || oldSize == newSize) {
            builder.append(newSize);
        } else {
            builder.append(oldSize);
            builder.append('+');
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
}
