package com.andrada.sitracker.fragment.components;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.IsNewItemTappedListener;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.util.DateFormatterUtil;
import com.andrada.sitracker.util.TouchDelegateGroup;

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

    private static final int TOUCH_ADDITION = 20;

    private TouchDelegateGroup mTouchDelegateGroup;

    private int mTouchAddition;

    private int mPreviousWidth = -1;
    private int mPreviousHeight = -1;

    private IsNewItemTappedListener mListener;

    public AuthorItemView(Context context) {
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
                    new Rect(width - author_updated.getWidth() - mTouchAddition, 0, width, height),
                    author_updated);

            setTouchDelegate(mTouchDelegateGroup);
        }
    }


    private void addTouchDelegate(Rect rect, View delegateView) {
        mTouchDelegateGroup.addTouchDelegate(new TouchDelegate(rect, delegateView));
    }

    @AfterViews
    void afterViews() {
        author_updated.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                authorDismissUpdates(v);
            }
        });
        author_updated.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mIsNew) {
                            author_updated.setImageResource(R.drawable.star_selected_focused);
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_OUTSIDE:
                    case MotionEvent.ACTION_UP:
                        //If we are not new, just ignore everything
                        if (mIsNew) {
                            author_updated.setImageResource(mIsNew ? R.drawable.star_selected : R.drawable.star_unselected);
                        }
                        break;
                }
                return false;
            }
        });
    }

    void authorDismissUpdates(View checkBox) {
        if (mListener != null) {
            mIsNew = false;
            author_updated.setImageResource(R.drawable.star_unselected);
            mListener.onIsNewItemTapped(checkBox);
        }
    }

    public void setListener(IsNewItemTappedListener listener) {
        mListener = listener;
    }

    public void bind(Author author, boolean isSelected) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            this.setActivated(isSelected);
        } else {
            this.setChecked(isSelected);
        }
        mIsNew = author.isUpdated();
        author_title.setText(author.getName());
        author_updated.setImageResource(mIsNew ? R.drawable.star_selected : R.drawable.star_unselected);
        author_update_date.setText(DateFormatterUtil.getFriendlyDateRelativeToToday(author.getUpdateDate()));
    }

}
