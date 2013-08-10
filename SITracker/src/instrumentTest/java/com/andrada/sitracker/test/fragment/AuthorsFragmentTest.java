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

package com.andrada.sitracker.test.fragment;

import android.test.UiThreadTest;
import android.widget.ListView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.events.AuthorSelectedEvent;
import com.andrada.sitracker.fragment.AuthorsFragment_;
import com.andrada.sitracker.fragment.adapters.AuthorsAdapter;
import com.andrada.sitracker.test.HomeActivityBaseTestCase;
import com.andrada.sitracker.test.util.DBTestSetupUtil;

import de.greenrobot.event.EventBus;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created by ggodonoga on 06/08/13.
 */
public class AuthorsFragmentTest extends HomeActivityBaseTestCase {

    private AuthorsFragment_ authorsFragment;
    private AuthorsAdapter authorsAdapter;
    private ListView listView;

    private AuthorSelectedEvent authorSelectedEvent = null;

    @Override
    protected void setUp() throws Exception {
        DBTestSetupUtil.clearDb(mMainActivity);
        DBTestSetupUtil.populateDBWithAuthors(mMainActivity);
        super.setUp();
        authorsFragment = (AuthorsFragment_) mMainActivity.getAuthorsFragment();
        authorsAdapter = authorsFragment.getAdapter();
        listView = authorsFragment.getListView();
    }

    public void testPreconditions() {
        assertThat(mMainActivity).isNotNull();
        assertThat(mActionBar).isNotNull();
        assertThat(authorsFragment).isNotNull()
                .isAdded()
                .hasId(R.id.fragment_authors);
        assertThat(authorsAdapter).isNotNull()
                .hasCount(DBTestSetupUtil.AUTHORS_COUNT);
        assertThat(listView).isNotNull()
                .hasAdapter(authorsAdapter)
                .hasCount(DBTestSetupUtil.AUTHORS_COUNT)
                .hasId(R.id.list).hasSelectedItemPosition(0);
    }

    @UiThreadTest
    public void testFragmentPostsAuthorSelectedEventWhenListItemIsClicked() {

        Object listener = new Object() {
            public void onEvent(AuthorSelectedEvent e) {
                authorSelectedEvent = e;
            }
        };
        EventBus.getDefault().register(listener);

        authorsFragment.listItemClicked(2);
        long idInList = listView.getItemIdAtPosition(2);

        assertThat(authorSelectedEvent).isNotNull();
        assertThat(authorSelectedEvent.authorId).isEqualTo(idInList);

        EventBus.getDefault().unregister(listener);
    }

    @UiThreadTest
    public void testAdapterHasCorrectItemSelectedWhenItIsClicked() {

        authorsFragment.listItemClicked(1);

        long idInList = listView.getItemIdAtPosition(1);

        assertThat(authorsAdapter.getSelectedAuthorId()).isEqualTo(idInList);
    }
}
