/*
 * Copyright 2014 Gleb Godonoga.
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
import android.widget.ImageView;
import android.widget.TextView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.ui.widget.CheckedRelativeLayout;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

@EViewGroup(R.layout.file_folder_list_item)
public class FileFolderView extends CheckedRelativeLayout {

    @ViewById
    TextView fileNameTextView;

    @ViewById
    ImageView fileFolderIconImageView;

    public FileFolderView(Context context) {
        super(context);
    }

    public void bind(String fileName, boolean isDirectory) {

        fileNameTextView.setText(fileName);

        fileFolderIconImageView.setImageResource(isDirectory ? R.drawable.glyph_folder_white : R.drawable.glyph_file);
    }

}
