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

package com.andrada.sitracker.ui.components;

import android.content.Context;

import com.andrada.sitracker.R;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.ui.widget.CheckedRelativeLayout;

import org.androidannotations.annotations.EViewGroup;

@EViewGroup(R.layout.new_pub_list_item)
public class NewPubItemView extends CheckedRelativeLayout {

    public NewPubItemView(Context context) {
        super(context);
    }

    public void bind(Publication publication) {

    }
}
