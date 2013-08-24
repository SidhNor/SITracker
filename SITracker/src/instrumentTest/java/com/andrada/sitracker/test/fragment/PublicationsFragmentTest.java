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

import com.andrada.sitracker.R;
import com.andrada.sitracker.test.HomeActivityBaseTestCase;
import com.andrada.sitracker.ui.fragment.PublicationsFragment_;

import static org.fest.assertions.api.ANDROID.assertThat;

/**
 * Created by ggodonoga on 06/08/13.
 */
public class PublicationsFragmentTest extends HomeActivityBaseTestCase {

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
