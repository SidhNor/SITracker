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

package com.andrada.sitracker.ui;


import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.tasks.io.AuthorFileImportContext;
import com.andrada.sitracker.tasks.io.AuthorImportStrategy;
import com.andrada.sitracker.tasks.io.PlainTextAuthorImport;
import com.andrada.sitracker.tasks.io.SIInformerXMLAuthorImport;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;
import java.util.List;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

@EActivity(R.layout.activity_import)
public class ImportAuthorsActivity extends BaseActivity{

    @ViewById
    CheckBox overwriteAuthorsCheckbox;

    @ViewById
    Button chooseFileButton;

    @ViewById
    Button performImportButton;

    @ViewById
    ProgressBar progressBar;

    @ViewById
    ListView list;

    @Click(R.id.chooseFileButton)
    void chooseFileClicked() {
        //Make sure the authors are opened
        final Intent chooserIntent = new Intent(getApplicationContext(), DirectoryChooserActivity.class);
        chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_NEW_DIR_NAME, "Books");
        chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_IS_DIRECTORY_CHOOSER, false);
        startActivityForResult(chooserIntent, Constants.REQUEST_DIRECTORY);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.REQUEST_DIRECTORY) {
            if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
                String fileToImport = data.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR);
                tryParseOutTheChosenFile(fileToImport);
                progressBar.setVisibility(View.VISIBLE);
            }
        }
    }

    @Background
    void tryParseOutTheChosenFile(String fileName) {
        showParseResults(new AuthorFileImportContext().getAuthorListFromFile(fileName));
    }

    @UiThread
    void showParseResults(List<String> authorLinks) {
        if (authorLinks != null && authorLinks.size() > 0) {
            list.setAdapter(new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, authorLinks) {
                @Override
                public View getView(int position, View convertView,
                                    ViewGroup parent) {
                    View view =super.getView(position, convertView, parent);
                    TextView textView=(TextView) view.findViewById(android.R.id.text1);
                    textView.setTextColor(Color.BLACK);
                    return view;
                }
            });
            performImportButton.setEnabled(true);
        } else {
            list.setAdapter(null);
            performImportButton.setEnabled(false);
            Crouton.makeText(this, getResources().getString(R.string.cannot_import_authors_from_file), Style.ALERT).show();
        }
        progressBar.setVisibility(View.GONE);


    }
}
