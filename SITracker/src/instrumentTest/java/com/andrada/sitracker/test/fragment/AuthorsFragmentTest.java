package com.andrada.sitracker.test.fragment;

import android.test.UiThreadTest;
import android.widget.ListView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.events.AuthorSelectedEvent;
import com.andrada.sitracker.fragment.AuthorsFragment_;
import com.andrada.sitracker.fragment.adapters.AuthorsAdapter;
import com.andrada.sitracker.test.MainActivityBaseTestCase;
import com.andrada.sitracker.test.util.DBTestSetupUtil;

import de.greenrobot.event.EventBus;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created by ggodonoga on 06/08/13.
 */
public class AuthorsFragmentTest extends MainActivityBaseTestCase {

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
