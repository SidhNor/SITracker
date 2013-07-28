package com.andrada.sitracker.fragment.components;

import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.IsNewItemTappedListener;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.util.DateFormatterUtil;
import com.andrada.sitracker.util.TouchDelegateGroup;

import org.androidannotations.annotations.AfterViews;
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
    ImageButton item_updated;

    @ViewById
    TextView item_description;

    @ViewById
    View publication_item_divider;

    @ViewById
    TextView itemSize;

    private boolean mIsNew = false;

    private IsNewItemTappedListener mListener;

    private static final int TOUCH_ADDITION = 20;

    private TouchDelegateGroup mTouchDelegateGroup;

    private int mTouchAddition;

    private int mPreviousWidth = -1;
    private int mPreviousHeight = -1;

    public PublicationItemView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        mTouchDelegateGroup = new TouchDelegateGroup(this);

        final float density = context.getResources().getDisplayMetrics().density;
        mTouchAddition = (int) (density * TOUCH_ADDITION + 0.5f);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        final int width = r - l;
        final int height = b - t;

        if (width != mPreviousWidth || height != mPreviousHeight) {

            mPreviousWidth = width;
            mPreviousHeight = height;

            mTouchDelegateGroup.clearTouchDelegates();

            addTouchDelegate(
                    new Rect(width - item_updated.getWidth() - mTouchAddition, 0, width, height),
                    item_updated);

            setTouchDelegate(mTouchDelegateGroup);
        }
    }


    private void addTouchDelegate(Rect rect, View delegateView) {
        mTouchDelegateGroup.addTouchDelegate(new TouchDelegate(rect, delegateView));
    }

    @AfterViews
    void afterViews() {
        item_updated.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                publicationDismissUpdates(v);
            }
        });
        item_updated.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mIsNew) {
                            item_updated.setImageResource(R.drawable.star_selected_focused);
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_OUTSIDE:
                    case MotionEvent.ACTION_UP:
                        //If we are not new, just ignore everything
                        if (mIsNew) {
                            item_updated.setImageResource(mIsNew ? R.drawable.star_selected : R.drawable.star_unselected);
                        }
                        break;
                }
                return false;
            }
        });
    }

    void publicationDismissUpdates(View checkBox) {
        if (mListener != null) {
            mIsNew = false;
            item_updated.setImageResource(R.drawable.star_unselected);
            mListener.onIsNewItemTapped(checkBox);
        }
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
}
