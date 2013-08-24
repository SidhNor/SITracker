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

package com.andrada.sitracker.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.andrada.sitracker.BuildConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * RelativeLayout base class that provides multiple tappable areas with custom
 * touch delegates of 8 possible configurations.
 * <p>Extended touch regions are highlighted in different colors when compiled in debug mode</p>
 * See {@link TouchDelegateRelativeLayout.ViewConfig}
 * for more details on possible configs
 */
public class TouchDelegateRelativeLayout extends RelativeLayout {

    private static final int TOUCH_ADDITION = 20;

    private TouchDelegateGroup mTouchDelegateGroup;

    private int mTouchAddition;

    private int mPreviousWidth = -1;
    private int mPreviousHeight = -1;

    /**
     * This is a mandatory field
     * Should contain all the views and their respective configs for delegated touch input
     */
    protected final HashMap<ViewConfig, View> delegatedTouchViews = new HashMap<ViewConfig, View>();

    public static boolean TapRegionHighlighted = false;

    private static final int[] HIGHLIGHT_COLOR_ARRAY = {
            Color.argb(50, 255, 0, 0),
            Color.argb(50, 0, 255, 0),
            Color.argb(50, 0, 0, 255),
            Color.argb(50, 0, 255, 255),
            Color.argb(50, 255, 0, 255),
            Color.argb(50, 255, 255, 0),
    };

    /**
     * Static class that provides configuration to
     * {@link TouchDelegateRelativeLayout}
     * describing expandable directions of touch delegate views
     */
    public static class ViewConfig {
        private static final int VIEW_CONFIG_START = 0x0;
        private static final int VIEW_CONFIG_END = 0x1;

        public int getHPosition() {
            return hPosition;
        }

        public int getHExpanding() {
            return hExpanding;
        }

        public int getVPosition() {
            return vPosition;
        }

        public int getVExpanding() {
            return vExpanding;
        }

        private int hPosition;
        private int hExpanding;
        private int vPosition;
        private int vExpanding;

        private ViewConfig(int horizontalPosition, int horizontalExpanding, int verticalPosition, int verticalExpanding) {
            //Positions can't expand both ways in MATCH_PARENT
            if ((horizontalExpanding != ViewGroup.LayoutParams.MATCH_PARENT &&
                    horizontalExpanding != ViewGroup.LayoutParams.WRAP_CONTENT) ||
                    (verticalExpanding != ViewGroup.LayoutParams.MATCH_PARENT &&
                            verticalExpanding != ViewGroup.LayoutParams.WRAP_CONTENT)) {
                throw new IllegalArgumentException(
                        "Expanding parameters can have only android.view.ViewGroup.LayoutParams.MATCH_PARENT " +
                                "or android.view.ViewGroup.LayoutParams.WRAP_CONTENT values");
            }
            if ((horizontalPosition != VIEW_CONFIG_START &&
                    horizontalPosition != VIEW_CONFIG_END) ||
                    (verticalPosition != VIEW_CONFIG_START &&
                            verticalPosition != VIEW_CONFIG_END)) {
                throw new IllegalArgumentException(
                        "Expanding parameters can have only android.view.ViewGroup.LayoutParams.MATCH_PARENT " +
                                "or android.view.ViewGroup.LayoutParams.WRAP_CONTENT values");
            }

            if (horizontalExpanding == ViewGroup.LayoutParams.MATCH_PARENT &&
                    verticalExpanding == ViewGroup.LayoutParams.MATCH_PARENT) {
                throw new IllegalArgumentException("You cannot configure both expanding directions to MATCH_PARENT");
            }
            this.hPosition = horizontalPosition;
            this.vPosition = verticalPosition;
            this.hExpanding = horizontalExpanding;
            this.vExpanding = verticalExpanding;
        }

        /**
         * ViewConfig for a touch region that takes the whole height of the parent component
         * and is anchored to the left. It's width is equal to view width + 20dp
         *
         * @return ViewConfig object with the specified options
         */
        public static ViewConfig wholeLeft() {
            return new ViewConfig(
                    ViewConfig.VIEW_CONFIG_START,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewConfig.VIEW_CONFIG_END,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }


        /**
         * ViewConfig for a touch region that takes the whole height of the parent component
         * and is anchored to the right. It's width is equal to view width + 20dp
         *
         * @return ViewConfig object with the specified options
         */
        public static ViewConfig wholeRight() {
            return new ViewConfig(
                    ViewConfig.VIEW_CONFIG_END,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewConfig.VIEW_CONFIG_END,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }


        /**
         * ViewConfig for a touch region that takes the whole width of the parent component
         * and is anchored to the top. It's height is equal to view height + 20dp
         *
         * @return ViewConfig object with the specified options
         */
        public static ViewConfig wholeTop() {
            return new ViewConfig(
                    ViewConfig.VIEW_CONFIG_START,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewConfig.VIEW_CONFIG_START,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }


        /**
         * ViewConfig for a touch region that takes the whole width of the parent component
         * and is anchored to the bottom. It's height is equal to view height + 20dp
         *
         * @return ViewConfig object with the specified options
         */
        public static ViewConfig wholeBottom() {
            return new ViewConfig(
                    ViewConfig.VIEW_CONFIG_START,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewConfig.VIEW_CONFIG_END,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }


        /**
         * ViewConfig for a touch region is anchored to the top-left corner of the parent component.
         * It's width and height is increased by 20dp
         *
         * @return ViewConfig object with the specified options
         */
        public static ViewConfig topLeft() {
            return new ViewConfig(
                    ViewConfig.VIEW_CONFIG_START,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewConfig.VIEW_CONFIG_START,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }


        /**
         * ViewConfig for a touch region is anchored to the top-right corner of the parent component.
         * It's width and height is increased by 20dp
         *
         * @return ViewConfig object with the specified options
         */
        public static ViewConfig topRight() {
            return new ViewConfig(
                    ViewConfig.VIEW_CONFIG_END,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewConfig.VIEW_CONFIG_START,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        /**
         * ViewConfig for a touch region is anchored to the bottom-left corner of the parent component.
         * It's width and height is increased by 20dp
         *
         * @return ViewConfig object with the specified options
         */
        public static ViewConfig bottomLeft() {
            return new ViewConfig(
                    ViewConfig.VIEW_CONFIG_START,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewConfig.VIEW_CONFIG_END,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }


        /**
         * ViewConfig for a touch region is anchored to the bottom-right corner of the parent component.
         * It's width and height is increased by 20dp
         *
         * @return ViewConfig object with the specified options
         */
        public static ViewConfig bottomRight() {
            return new ViewConfig(
                    ViewConfig.VIEW_CONFIG_END,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewConfig.VIEW_CONFIG_END,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }


    private static class TouchDelegateRecord {
        public final Rect rect;
        public final int color;

        public TouchDelegateRecord(Rect _rect, int _color) {
            rect = _rect;
            color = _color;
        }
    }

    private final ArrayList<TouchDelegateRecord> mTouchDelegateRecords = new ArrayList<TouchDelegateRecord>();
    private final Paint mPaint = new Paint();

    public TouchDelegateRelativeLayout(Context context) {
        super(context);
        init(context);
    }

    public TouchDelegateRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TouchDelegateRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        mTouchDelegateGroup = new TouchDelegateGroup(this);
        if (BuildConfig.DEBUG && TapRegionHighlighted) {
            mPaint.setStyle(Paint.Style.FILL);
        }

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

            int j = 0;
            for (Map.Entry<ViewConfig, View> entry : delegatedTouchViews.entrySet()) {
                if (j == HIGHLIGHT_COLOR_ARRAY.length) {
                    j = 0;
                }
                entry.getValue().setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onDelegatedTouchViewClicked(v);
                    }
                });
                entry.getValue().setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        switch (motionEvent.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                onDelegatedTouchViewDown(view);
                                break;
                            case MotionEvent.ACTION_CANCEL:
                            case MotionEvent.ACTION_OUTSIDE:
                            case MotionEvent.ACTION_UP:
                                onDelegatedTouchViewCancel(view);
                                break;
                        }
                        return false;
                    }
                });
                addTouchDelegate(computeRectFor(width, height, entry.getValue(), entry.getKey()),
                        HIGHLIGHT_COLOR_ARRAY[j], entry.getValue());
                j++;
            }

            setTouchDelegate(mTouchDelegateGroup);
        }
    }

    private Rect computeRectFor(int parentWidth, int parentHeight, View childView, ViewConfig config) {
        int x = 0, y = 0, w = parentWidth, h = parentHeight;

        if (config.getHExpanding() == ViewGroup.LayoutParams.MATCH_PARENT) {
            //We can ignore horizontal position.we will use full width of the parent component
            x = 0;
            w = parentWidth;
            if (config.getVPosition() == ViewConfig.VIEW_CONFIG_START) {
                //Place at the top
                y = 0;
                h = childView.getHeight() + mTouchAddition;
            } else {
                //Place at the bottom
                y = parentHeight - childView.getHeight() - mTouchAddition;
                h = parentHeight;
            }
        } else if (config.getVExpanding() == ViewGroup.LayoutParams.MATCH_PARENT) {
            //We can ignore vertical position, we will use height of the parent component
            y = 0;
            h = parentHeight;
            if (config.getHPosition() == ViewConfig.VIEW_CONFIG_START) {
                //Place on the left
                x = 0;
                w = childView.getWidth() + mTouchAddition;
            } else {
                //Place at the right
                x = parentWidth - childView.getWidth() - mTouchAddition;
                w = parentWidth;
            }
        } else {
            /**
             * We are in a position when both expanding are WRAP_CONTENT
             * That means we need to care just about the view positioning.
             * We have 4 options here
             */
            if (config.getHPosition() == ViewConfig.VIEW_CONFIG_START) {
                x = 0;
                w = childView.getWidth() + mTouchAddition;
            } else {
                x = parentWidth - childView.getWidth() - mTouchAddition;
                w = parentWidth;
            }
            if (config.getVPosition() == ViewConfig.VIEW_CONFIG_START) {
                y = 0;
                h = childView.getHeight() + mTouchAddition;
            } else {
                y = parentHeight - childView.getHeight() - mTouchAddition;
                h = parentHeight;
            }
        }
        return new Rect(x, y, w, h);
    }

    private void addTouchDelegate(Rect rect, int color, View delegateView) {
        mTouchDelegateGroup.addTouchDelegate(new TouchDelegate(rect, delegateView));
        mTouchDelegateRecords.add(new TouchDelegateRecord(rect, color));
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (BuildConfig.DEBUG && TapRegionHighlighted) {
            for (TouchDelegateRecord record : mTouchDelegateRecords) {
                mPaint.setColor(record.color);
                canvas.drawRect(record.rect, mPaint);
            }
        }
        super.dispatchDraw(canvas);
    }

    /**
     * Callback when a touch delegate view is being clicked
     *
     * @param view that is being clicked on
     */
    protected void onDelegatedTouchViewClicked(View view) {

    }

    /**
     * Callback when a touch delegate view is being pressed
     *
     * @param view that is being pressed
     */
    protected void onDelegatedTouchViewDown(View view) {

    }

    /**
     * Callback when a touch delegate view receives a cancel event
     * Use only to reset the state of the view. Is not intended for low level touch manipulation
     *
     * @param view that is being pressed
     */
    protected void onDelegatedTouchViewCancel(View view) {

    }
}
