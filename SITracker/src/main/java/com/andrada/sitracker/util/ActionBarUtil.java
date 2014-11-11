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

package com.andrada.sitracker.util;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.ListView;

import com.andrada.sitracker.R;

import java.util.ArrayList;

public class ActionBarUtil {

    private static final int HEADER_HIDE_ANIM_DURATION = 300;
    private Context mContext;
    private ActionBarShowHideListener mListener;
    // When set, these components will be shown/hidden in sync with the action bar
    // to implement the "quick recall" effect (the Action Bar and the header views disappear
    // when you scroll down a list, and reappear quickly when you scroll up).
    private ArrayList<View> mHideableHeaderViews = new ArrayList<View>();
    // variables that control the Action Bar auto hide behavior (aka "quick recall")
    private boolean mActionBarAutoHideEnabled = false;
    private int mActionBarAutoHideSensivity = 0;
    private int mActionBarAutoHideMinY = 0;
    private int mActionBarAutoHideSignal = 0;
    private boolean mActionBarShown = true;
    private RecyclerView mCurrentScrollingView;

    public ActionBarUtil(ActionBarActivity context, ActionBarShowHideListener listener) {
        mContext = context;
        mListener = listener;
    }

    public void registerHideableHeaderView(View hideableHeaderView) {
        if (!mHideableHeaderViews.contains(hideableHeaderView)) {
            mHideableHeaderViews.add(hideableHeaderView);
        }
    }

    public void deregisterHideableHeaderView(View hideableHeaderView) {
        if (mHideableHeaderViews.contains(hideableHeaderView)) {
            mHideableHeaderViews.remove(hideableHeaderView);
        }
    }

    public void autoShowOrHideActionBar(boolean show) {
        if (show == mActionBarShown) {
            return;
        }
        mActionBarShown = show;
        onActionBarAutoShowOrHide(show);
    }

    private void onActionBarAutoShowOrHide(boolean shown) {
        mListener.actionBarVisibilityChanged(mActionBarShown);

        for (View view : mHideableHeaderViews) {
            if (shown) {
                view.animate()
                        .translationY(0)
                        .alpha(1)
                        .setDuration(HEADER_HIDE_ANIM_DURATION)
                        .setInterpolator(new DecelerateInterpolator());
            } else {
                view.animate()
                        .translationY(-view.getBottom())
                        .alpha(0)
                        .setDuration(HEADER_HIDE_ANIM_DURATION)
                        .setInterpolator(new DecelerateInterpolator());
            }
        }
    }

    /**
     * Initializes the Action Bar auto-hide (aka Quick Recall) effect.
     */
    private void initActionBarAutoHide() {
        mActionBarAutoHideEnabled = true;
        mActionBarAutoHideMinY = mContext.getResources().getDimensionPixelSize(
                R.dimen.action_bar_auto_hide_min_y);
        mActionBarAutoHideSensivity = mContext.getResources().getDimensionPixelSize(
                R.dimen.action_bar_auto_hide_sensivity);
    }

    public boolean isActionBarAutoHideEnabled() {
        return mActionBarAutoHideEnabled;
    }

    public boolean isActionBarShown() {
        return mActionBarShown;
    }

    public void enableActionBarAutoHide(final ListView listView) {
        initActionBarAutoHide();
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            final static int ITEMS_THRESHOLD = 3;
            int lastFvi = 0;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                onMainContentScrolled(firstVisibleItem <= ITEMS_THRESHOLD ? 0 : Integer.MAX_VALUE,
                        lastFvi - firstVisibleItem > 0 ? Integer.MIN_VALUE :
                                lastFvi == firstVisibleItem ? 0 : Integer.MAX_VALUE
                );
                lastFvi = firstVisibleItem;
            }
        });
    }

    public void enableActionBarAutoHide(final RecyclerView recyclerView) {
        initActionBarAutoHide();
        mCurrentScrollingView = recyclerView;
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            int lastFvi = 0;

            @Override
            public void onScrollStateChanged(RecyclerView view, int scrollState) {
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int deltaY) {
                super.onScrolled(recyclerView, dx, deltaY);
                lastFvi += deltaY;

                if (deltaY > mActionBarAutoHideSensivity) {
                    deltaY = mActionBarAutoHideSensivity;
                } else if (deltaY < -mActionBarAutoHideSensivity) {
                    deltaY = -mActionBarAutoHideSensivity;
                }

                if (Math.signum(deltaY) * Math.signum(mActionBarAutoHideSignal) < 0) {
                    // deltaY is a motion opposite to the accumulated signal, so reset signal
                    mActionBarAutoHideSignal = deltaY;
                } else {
                    // add to accumulated signal
                    mActionBarAutoHideSignal += deltaY;
                }

                boolean shouldShow = lastFvi <= mActionBarAutoHideMinY ||
                        (mActionBarAutoHideSignal <= -mActionBarAutoHideSensivity);
                autoShowOrHideActionBar(shouldShow);
            }
        });
    }

    public void disableActionBarAutoHide() {
        mActionBarAutoHideEnabled = false;
        if (mCurrentScrollingView != null) {
            mCurrentScrollingView.setOnScrollListener(null);
            mCurrentScrollingView = null;
        }
    }

    /**
     * Indicates that the main content has scrolled (for the purposes of showing/hiding
     * the action bar for the "action bar auto hide" effect). currentY and deltaY may be exact
     * (if the underlying view supports it) or may be approximate indications:
     * deltaY may be INT_MAX to mean "scrolled forward indeterminately" and INT_MIN to mean
     * "scrolled backward indeterminately".  currentY may be 0 to mean "somewhere close to the
     * start of the list" and INT_MAX to mean "we don't know, but not at the start of the list"
     */
    private void onMainContentScrolled(int currentY, int deltaY) {
        if (deltaY > mActionBarAutoHideSensivity) {
            deltaY = mActionBarAutoHideSensivity;
        } else if (deltaY < -mActionBarAutoHideSensivity) {
            deltaY = -mActionBarAutoHideSensivity;
        }

        if (Math.signum(deltaY) * Math.signum(mActionBarAutoHideSignal) < 0) {
            // deltaY is a motion opposite to the accumulated signal, so reset signal
            mActionBarAutoHideSignal = deltaY;
        } else {
            // add to accumulated signal
            mActionBarAutoHideSignal += deltaY;
        }

        boolean shouldShow = currentY < mActionBarAutoHideMinY ||
                (mActionBarAutoHideSignal <= -mActionBarAutoHideSensivity);
        autoShowOrHideActionBar(shouldShow);
    }

    public interface ActionBarShowHideListener {
        void actionBarVisibilityChanged(boolean shown);
    }
}
