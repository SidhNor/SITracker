package com.andrada.sitracker.loader;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.AsyncTaskLoader;

import com.andrada.sitracker.db.beans.SearchedAuthor;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SamlibSearchLoader extends AsyncTaskLoader<List<SearchedAuthor>> {

    public static final int SEARCH_TOKEN = 0x3;

    private List<SearchedAuthor> mAuthors = new ArrayList<SearchedAuthor>();
    private Uri mQuery;

    public SamlibSearchLoader(Context context, Uri query) {
        super(context);
        mQuery = query;
    }

    @Override
    public List<SearchedAuthor> loadInBackground() {

        return mAuthors;
    }

    /**
     * Called when there is new data to deliver to the client.  The
     * super class will take care of delivering it; the implementation
     * here just adds a little more logic.
     */
    @Override
    public void deliverResult(@NotNull List<SearchedAuthor> authors) {
        if (isReset()) {
            // An async query came in while the loader is stopped.  We
            // don't need the result.
            if (authors.size() != 0) {
                onReleaseResources(authors);
            }
        }
        List<SearchedAuthor> oldAuthors = mAuthors;
        mAuthors = authors;

        if (isStarted()) {
            // If the Loader is currently started, we can immediately
            // deliver its results.
            super.deliverResult(authors);
        }

        // At this point we can release the resources associated with
        // 'oldApps' if needed; now that the new result is delivered we
        // know that it is no longer in use.
        if (mAuthors.size() != 0) {
            onReleaseResources(oldAuthors);
        }
    }

    /**
     * Handles a request to start the Loader.
     */
    @Override
    protected void onStartLoading() {
        if (mAuthors.size() != 0) {
            // If we currently have a result available, deliver it
            // immediately.
            deliverResult(mAuthors);
        }
        if (takeContentChanged() || mAuthors.size() != 0) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }
    }

    /**
     * Handles a request to stop the Loader.
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    /**
     * Handles a request to cancel a load.
     */
    @Override
    public void onCanceled(List<SearchedAuthor> apps) {
        super.onCanceled(apps);

        // At this point we can release the resources associated with 'apps'
        // if needed.
        onReleaseResources(apps);
    }

    /**
     * Handles a request to completely reset the Loader.
     */
    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        // At this point we can release the resources associated with 'apps'
        // if needed.
        if (mAuthors.size() != 0) {
            onReleaseResources(mAuthors);
        }
    }

    /**
     * Helper function to take care of releasing resources associated
     * with an actively loaded data set.
     */
    protected void onReleaseResources(List<SearchedAuthor> apps) {
        // For a simple List<> there is nothing to do.  For something
        // like a Cursor, we would close it here.
        mAuthors.clear();
    }
}
