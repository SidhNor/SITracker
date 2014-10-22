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
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.andrada.sitracker.ui.fragment.adapters.MultiSelectionRecyclerAdapter;
import com.andrada.sitracker.ui.widget.RecyclerItemClickListener;
import com.andrada.sitracker.ui.widget.RecyclerLongTapListener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;

/**
 * Utilities for handling multiple selection in Recycler views. Contains functionality similar to
 * {@link AbsListView#CHOICE_MODE_MULTIPLE_MODAL} but that works with {@link ActionBarActivity}.
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
        /**
         * @see android.widget.AbsListView.MultiChoiceModeListener#onItemCheckedStateChanged(
         *android.view.ActionMode, int, long, boolean)
         */
        public void onItemCheckedStateChanged(ActionMode mode,
                                              int position, long id, boolean checked);
    }

    public static class Controller implements ActionMode.Callback {

        @Nullable
        private ActionMode mActionMode;
        @NotNull
        private RecyclerView mRecyclerView;
        @NotNull
        private MultiSelectionRecyclerAdapter mAdapter;
        @NotNull
        private ActionBarActivity mActivity;
        @NotNull
        private MultiChoiceModeListener mListener;
        @Nullable
        private HashSet<Long> mTempIdsToCheckOnRestore;
        private HashSet<Pair<Integer, Long>> mItemsToCheck;
        private RecyclerLongTapListener mLongTapListener;
        private RecyclerItemClickListener mMultiselectionTouchListener;


        private Controller() {

        }

        @NotNull
        public static Controller attach(@NotNull RecyclerView recyclerView, @NotNull ActionBarActivity activity,
                                        @NotNull MultiChoiceModeListener listener) {
            Controller controller = new Controller();
            controller.mRecyclerView = recyclerView;
            if (!(recyclerView.getAdapter() instanceof MultiSelectionRecyclerAdapter)) {
                throw new IllegalArgumentException("You can use multiselect only with a MultiSelectionRecyclerAdapter");
            }
            controller.mAdapter = (MultiSelectionRecyclerAdapter) recyclerView.getAdapter();
            controller.mActivity = activity;
            controller.mListener = listener;
            controller.attachDefaultListener();
            return controller;
        }

        private void attachDefaultListener() {
            this.mLongTapListener = new RecyclerLongTapListener(mActivity, mRecyclerView, new RecyclerLongTapListener.OnItemLongTapListener() {
                @Override
                public void onItemLongTap(View view, int position) {
                    if (mActionMode != null) {
                        return;
                    }
                    long id = mRecyclerView.getAdapter().getItemId(position);
                    mItemsToCheck = new HashSet<Pair<Integer, Long>>();
                    mItemsToCheck.add(new Pair<Integer, Long>(position, id));
                    mActionMode = mActivity.startSupportActionMode(Controller.this);
                }
            });
            mRecyclerView.addOnItemTouchListener(mLongTapListener);
        }

        private void readInstanceState(@Nullable long[] itemIds) {
            mTempIdsToCheckOnRestore = null;
            if (itemIds != null && itemIds.length > 0) {
                mTempIdsToCheckOnRestore = new HashSet<Long>();
                for (long id : itemIds) {
                    mTempIdsToCheckOnRestore.add(id);
                }
            }
        }

        public void tryRestoreInstanceState(long[] itemIds) {
            readInstanceState(itemIds);
            tryRestoreInstanceState();
        }

        public void finish() {
            if (mActionMode != null) {
                mActionMode.finish();
            }
            if (mRecyclerView != null) {
                mRecyclerView.removeOnItemTouchListener(mLongTapListener);
                mRecyclerView.removeOnItemTouchListener(mMultiselectionTouchListener);
            }
        }

        public void tryRestoreInstanceState() {
            if (mTempIdsToCheckOnRestore == null || mRecyclerView.getAdapter() == null) {
                return;
            }

            boolean idsFound = false;
            RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
            for (int pos = adapter.getItemCount() - 1; pos >= 0; pos--) {
                if (mTempIdsToCheckOnRestore.contains(adapter.getItemId(pos))) {
                    idsFound = true;
                    if (mItemsToCheck == null) {
                        mItemsToCheck = new HashSet<Pair<Integer, Long>>();
                    }
                    mItemsToCheck.add(
                            new Pair<Integer, Long>(pos, adapter.getItemId(pos)));
                }
            }

            if (idsFound) {
                // We found some IDs that were checked. Let's now restore the multi-selection
                // state.
                mTempIdsToCheckOnRestore = null; // clear out this temp field
                mActionMode = mActivity.startSupportActionMode(Controller.this);
            }
        }

        public boolean saveInstanceState(@NotNull Bundle outBundle) {
            if (mActionMode != null) {
                long[] checkedIds = mAdapter.getSelectedItemsIds();
                outBundle.putLongArray(getStateKey(), checkedIds);
                return true;
            }

            return false;
        }

        @NotNull
        private String getStateKey() {
            return MultiSelectionUtil.class.getSimpleName() + "_" + mRecyclerView.getId();
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            if (mListener.onCreateActionMode(actionMode, menu)) {
                mActionMode = actionMode;
                mMultiselectionTouchListener = new RecyclerItemClickListener(mActivity, new RecyclerItemClickListener.OnItemClickListener() {
                    public void onItemClick(View view, int position) {
                        mAdapter.toggleSelection(position);
                        mListener.onItemCheckedStateChanged(mActionMode, position,
                                mRecyclerView.getAdapter().getItemId(position), true);

                        if (mAdapter.getSelectedItemCount() <= 0 && mActionMode != null) {
                            mActionMode.finish();
                        }
                    }
                });
                mRecyclerView.addOnItemTouchListener(mMultiselectionTouchListener);

                if (mItemsToCheck != null) {
                    for (Pair<Integer, Long> posAndId : mItemsToCheck) {
                        mAdapter.toggleSelection(posAndId.first, true);
                        mListener.onItemCheckedStateChanged(mActionMode, posAndId.first,
                                posAndId.second, true);
                    }
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
            mListener.onDestroyActionMode(actionMode);
            mAdapter.clearSelections();
            mRecyclerView.removeOnItemTouchListener(mMultiselectionTouchListener);
            mActionMode = null;
        }
    }
}
