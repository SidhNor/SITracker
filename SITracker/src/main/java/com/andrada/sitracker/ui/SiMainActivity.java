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

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.AppUriContract;
import com.andrada.sitracker.events.AuthorSelectedEvent;
import com.andrada.sitracker.ui.fragment.AuthorsFragment;
import com.andrada.sitracker.ui.fragment.AuthorsFragment_;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.ViewById;
import org.jetbrains.annotations.NotNull;

import de.greenrobot.event.EventBus;

@EActivity
public class SiMainActivity extends BaseActivity {

    @ViewById(R.id.fragment_holder)
    View fragmentHolder;

    public static final String AUTHORS_PROCESSED_EXTRA = "authors_total_processed";
    public static final String AUTHORS_SUCCESSFULLY_IMPORTED_EXTRA = "authors_successfully_imported";
    @Extra(AUTHORS_PROCESSED_EXTRA)
    int authorsProcessed = -1;
    @Extra(AUTHORS_SUCCESSFULLY_IMPORTED_EXTRA)
    int authorsSuccessfullyImported = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int priority = 1;
        EventBus.getDefault().register(this, priority);

        setContentView(R.layout.activity_si_main);
    }

    @AfterViews
    protected void afterViews() {
        Fragment fragment = getFragmentManager().findFragmentByTag("currentFrag");
        if (fragment == null) {
            //Bootstrap app with initial fragment
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            AuthorsFragment authFrag = AuthorsFragment_.builder().build();
            transaction.replace(R.id.fragment_holder, authFrag, "currentFrag");
            transaction.setCustomAnimations(0, 0);
            transaction.commit();
        }
        super.afterViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();
        attemptToShowImportProgress();
    }

    @Override
    protected void onNewIntent(@NotNull Intent intent) {
        super.onNewIntent(intent);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey(AUTHORS_PROCESSED_EXTRA)) {
                authorsProcessed = extras.getInt(AUTHORS_PROCESSED_EXTRA);
            }
            if (extras.containsKey(AUTHORS_SUCCESSFULLY_IMPORTED_EXTRA)) {
                authorsSuccessfullyImported = extras.getInt(AUTHORS_SUCCESSFULLY_IMPORTED_EXTRA);
            }
        }
    }

    public void onEvent(AuthorSelectedEvent event) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                AppUriContract.buildAuthorUri(event.authorId, event.authorName), this,
                AuthorDetailsActivity_.class);
        ActivityCompat.startActivity(this, intent, null);
    }


    private void attemptToShowImportProgress() {
        if (authorsProcessed != -1 && authorsSuccessfullyImported != -1) {
            View view = getLayoutInflater().inflate(R.layout.widget_import_result, null);
            TextView totalTextV = (TextView) view.findViewById(R.id.totalAuthorsText);
            totalTextV.setText(getResources().getString(R.string.author_import_total_crouton_message,
                    authorsProcessed));
            TextView successTextV = (TextView) view.findViewById(R.id.successAuthorsText);
            successTextV.setText(getResources().getString(R.string.author_import_processed_crouton_message,
                    authorsSuccessfullyImported));
            TextView failedTextV = (TextView) view.findViewById(R.id.failedAuthorsText);
            failedTextV.setText(getResources().getString(R.string.author_import_failed_crouton_message,
                    authorsProcessed - authorsSuccessfullyImported));

            new MaterialDialog.Builder(this)
                    .customView(view, false)
                    .positiveText(R.string.ok)
                    .autoDismiss(true)
                    .show();

            //Remove extras to avoid reinitialization on config change
            getIntent().removeExtra(AUTHORS_PROCESSED_EXTRA);
            getIntent().removeExtra(AUTHORS_SUCCESSFULLY_IMPORTED_EXTRA);
            authorsSuccessfullyImported = -1;
            authorsProcessed = -1;
        }
    }
}
