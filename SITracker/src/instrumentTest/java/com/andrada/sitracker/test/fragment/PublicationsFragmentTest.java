package com.andrada.sitracker.test.fragment;

import com.andrada.sitracker.R;
import com.andrada.sitracker.fragment.PublicationsFragment_;
import com.andrada.sitracker.test.MainActivityBaseTestCase;

import static org.fest.assertions.api.ANDROID.assertThat;

/**
 * Created by ggodonoga on 06/08/13.
 */
public class PublicationsFragmentTest extends MainActivityBaseTestCase {

    private PublicationsFragment_ publicationsFragment;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        publicationsFragment = (PublicationsFragment_) mMainActivity.getPubFragment();
    }

    public void testPreconditions() {
        assertThat(mMainActivity).isNotNull();
        assertThat(publicationsFragment).isNotNull()
                .isAdded()
                .hasId(R.id.fragment_publications);
    }

}
