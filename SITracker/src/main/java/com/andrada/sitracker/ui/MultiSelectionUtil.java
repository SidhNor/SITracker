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

package com.andrada.sitracker.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lucasr.twowayview.ItemClickSupport;
import org.lucasr.twowayview.ItemSelectionSupport;

/**
 * Utilities for handling ActionMode in Recycler views.
 */
public class MultiSelectionUtil {
    @NotNull
    public static Controller attachMultiSelectionController(@NotNull final RecyclerView listView,
                                                            @NotNull final ActionBarActivity activity,
                                                            @NotNull final MultiChoiceModeListener listener) {
        return Controller.attach(listView, activity, listener);
    }

    /**
     * @see android.widget.AbsListView.MultiChoiceModeListener
     */
    public static interface MultiChoiceModeListener extends ActionMode.Callback {
        public void onItemCheckedStateChanged(ActionMode mode);
    }

    public static class Controller implements ActionMode.Callback {

        @Nullable
        private ActionMode mActionMode;
        @NotNull
        private RecyclerView mRecyclerView;
        @NotNull
        private ActionBarActivity mActivity;
        @NotNull
        private MultiChoiceModeListener mListener;

        private ItemClickSupport.OnItemLongClickListener mLongTapListener;
        private ItemSelectionSupport itemSelection;
        private ItemClickSupport itemClick;

        private Controller() {

        }

        @NotNull
        public static Controller attach(@NotNull RecyclerView recyclerView, @NotNull ActionBarActivity activity,
                                        @NotNull MultiChoiceModeListener listener) {
            Controller controller = new Controller();
            controller.mRecyclerView = recyclerView;
            controller.mActivity = activity;
            controller.mListener = listener;
            controller.attachDefaultListener();
            return controller;
        }

        public ItemSelectionSupport getItemSelection() {
            return itemSelection;
        }

        private void attachDefaultListener() {

            this.mLongTapListener = new ItemClickSupport.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(RecyclerView recyclerView, View view, int position, long id) {
                    SparseBooleanArray checkedStates = itemSelection.getCheckedItemPositions();

                    boolean newState = true;
                    if (checkedStates != null) {
                        newState = !checkedStates.get(position, false);
                    }
                    if (mActionMode == null) {
                        itemSelection.setChoiceMode(ItemSelectionSupport.ChoiceMode.MULTIPLE);
                        itemSelection.setItemChecked(position, newState);
                        mActionMode = mActivity.startSupportActionMode(Controller.this);
                    } else {
                        itemSelection.setItemChecked(position, newState);
                        mListener.onItemCheckedStateChanged(mActionMode);
                        if (itemSelection.getCheckedItemCount() == 0) {
                            mActionMode.finish();
                        }
                    }
                    return true;
                }
            };

            itemClick = ItemClickSupport.addTo(mRecyclerView);
            itemSelection = ItemSelectionSupport.addTo(mRecyclerView);
            itemClick.setOnItemLongClickListener(mLongTapListener);
        }

        public void finish() {
            if (mActionMode != null) {
                mActionMode.finish();
            }
            //TODO This is usually caled on fragment destruction, make sure there is no memory leak here for ItemSelection and ItemClick Support
            /*if (mRecyclerView != null) {
                ItemSelectionSupport.removeFrom(mRecyclerView);
                ItemClickSupport.removeFrom(mRecyclerView);
            }*/

        }

        public void tryRestoreInstanceState(Bundle state) {
            if (itemSelection == null || mRecyclerView.getAdapter() == null || state == null) {
                return;
            }
            itemSelection.onRestoreInstanceState(state);
            if (itemSelection.getCheckedItemCount() > 0) {
                itemSelection.setChoiceMode(ItemSelectionSupport.ChoiceMode.MULTIPLE);
                SparseBooleanArray itemsCheck = itemSelection.getCheckedItemPositions();
                for (int i = 0; i < itemsCheck.size(); i++) {
                    itemSelection.setItemChecked(itemsCheck.keyAt(i), itemsCheck.valueAt(i));
                }
                mActionMode = mActivity.startSupportActionMode(Controller.this);
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            if (mListener.onCreateActionMode(actionMode, menu)) {
                mActionMode = actionMode;
                if (itemSelection.getCheckedItemCount() > 0) {
                    mListener.onItemCheckedStateChanged(mActionMode);
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            if (mListener.onPrepareActionMode(actionMode, menu)) {
                mActionMode = actionMode;
                return true;
            }
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            return mListener.onActionItemClicked(actionMode, menuItem);
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            itemSelection.clearChoices();
            itemSelection.setChoiceMode(ItemSelectionSupport.ChoiceMode.NONE);
            mListener.onDestroyActionMode(actionMode);
            mActionMode = null;
        }
    }
}
