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

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ListView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;

/**
 * Utilities for handling multiple selection in list views. Contains functionality similar to
 * {@link AbsListView#CHOICE_MODE_MULTIPLE_MODAL} but that works with {@link Activity} and
 * backward-compatible action bars.
 */
public class MultiSelectionUtil {
    @NotNull
    public static Controller attachMultiSelectionController(@NotNull final ListView listView,
                                                            @NotNull final Activity activity,
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

    public static class Controller implements
            ActionMode.Callback,
            AdapterView.OnItemClickListener,
            AdapterView.OnItemLongClickListener {
        private final Handler mHandler = new Handler();
        private final Runnable mSetChoiceModeNoneRunnable = new Runnable() {
            @Override
            public void run() {
                mListView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
            }
        };
        @Nullable
        private ActionMode mActionMode;
        @NotNull
        private ListView mListView;
        @NotNull
        private Activity mActivity;
        @NotNull
        private MultiChoiceModeListener mListener;
        @Nullable
        private HashSet<Long> mTempIdsToCheckOnRestore;
        private HashSet<Pair<Integer, Long>> mItemsToCheck;
        private AdapterView.OnItemClickListener mOldItemClickListener;

        private Controller() {
        }

        @NotNull
        public static Controller attach(@NotNull ListView listView, @NotNull Activity activity,
                                        @NotNull MultiChoiceModeListener listener) {
            if (listView.getChoiceMode() == AbsListView.CHOICE_MODE_MULTIPLE ||
                    listView.getChoiceMode() == AbsListView.CHOICE_MODE_MULTIPLE_MODAL) {
                throw new IllegalArgumentException("ListView CHOICE_MODE_MULTIPLE or CHOICE_MODE_MULTIPLE_MODAL is not allowed. Everything is handled by this class.");
            }
            Controller controller = new Controller();
            controller.mListView = listView;
            controller.mActivity = activity;
            controller.mListener = listener;
            listView.setOnItemLongClickListener(controller);
            return controller;
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
        }

        public void tryRestoreInstanceState() {
            if (mTempIdsToCheckOnRestore == null || mListView.getAdapter() == null) {
                return;
            }

            boolean idsFound = false;
            Adapter adapter = mListView.getAdapter();
            for (int pos = adapter.getCount() - 1; pos >= 0; pos--) {
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
                mActionMode = mActivity.startActionMode(Controller.this);
            }
        }

        public boolean saveInstanceState(@NotNull Bundle outBundle) {
            // TODO: support non-stable IDs by persisting positions instead of IDs
            if (mActionMode != null && mListView.getAdapter().hasStableIds()) {
                long[] checkedIds = mListView.getCheckedItemIds();
                outBundle.putLongArray(getStateKey(), checkedIds);
                return true;
            }

            return false;
        }

        @NotNull
        private String getStateKey() {
            return MultiSelectionUtil.class.getSimpleName() + "_" + mListView.getId();
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            if (mListener.onCreateActionMode(actionMode, menu)) {
                mActionMode = actionMode;
                mOldItemClickListener = mListView.getOnItemClickListener();
                mListView.setOnItemClickListener(Controller.this);
                mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
                mHandler.removeCallbacks(mSetChoiceModeNoneRunnable);

                if (mItemsToCheck != null) {
                    for (Pair<Integer, Long> posAndId : mItemsToCheck) {
                        mListView.setItemChecked(posAndId.first, true);
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
            SparseBooleanArray checkedPositions = mListView.getCheckedItemPositions();
            if (checkedPositions != null) {
                for (int i = 0; i < checkedPositions.size(); i++) {
                    mListView.setItemChecked(checkedPositions.keyAt(i), false);
                }
            }
            mListView.setOnItemClickListener(mOldItemClickListener);
            mActionMode = null;
            mHandler.post(mSetChoiceModeNoneRunnable);
        }

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            boolean checked = mListView.isItemChecked(position);
            mListener.onItemCheckedStateChanged(mActionMode, position, id, checked);

            int numChecked = 0;
            SparseBooleanArray checkedItemPositions = mListView.getCheckedItemPositions();
            if (checkedItemPositions != null) {
                for (int i = 0; i < checkedItemPositions.size(); i++) {
                    numChecked += checkedItemPositions.valueAt(i) ? 1 : 0;
                }
            }

            if (numChecked <= 0) {
                mActionMode.finish();
            }
        }

        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position,
                                       long id) {
            if (mActionMode != null) {
                return false;
            }

            mItemsToCheck = new HashSet<Pair<Integer, Long>>();
            mItemsToCheck.add(new Pair<Integer, Long>(position, id));
            mActionMode = mActivity.startActionMode(Controller.this);
            return true;
        }
    }
}
