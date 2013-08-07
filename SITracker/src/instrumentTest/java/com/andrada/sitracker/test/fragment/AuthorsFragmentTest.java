package com.andrada.sitracker.test.fragment;

import android.widget.ListView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.fragment.AuthorsFragment_;
import com.andrada.sitracker.fragment.adapters.AuthorsAdapter;
import com.andrada.sitracker.test.MainActivityBaseTestCase;
import com.andrada.sitracker.test.util.DBTestSetupUtil;

import static org.fest.assertions.api.ANDROID.assertThat;

/**
 * Created by ggodonoga on 06/08/13.
 */
public class AuthorsFragmentTest extends MainActivityBaseTestCase {

    private AuthorsFragment_ authorsFragment;
    private AuthorsAdapter authorsAdapter;
    private ListView listView;

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
        assertThat(authorsFragment).isNotNull()
                .isAdded()
                .hasId(R.id.fragment_authors);
        assertThat(authorsAdapter).isNotNull();
        assertThat(listView).isNotNull()
                .hasAdapter(authorsAdapter)
                .hasId(R.id.list).hasSelectedItemPosition(0);
    }

}
