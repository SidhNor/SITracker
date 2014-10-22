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

package com.andrada.sitracker.loader;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.net.Uri;

import com.andrada.sitracker.contracts.AppUriContract;
import com.andrada.sitracker.db.beans.SearchedAuthor;
import com.andrada.sitracker.exceptions.SearchException;
import com.andrada.sitracker.reader.SamlibCgiSearchStrategyImpl;
import com.andrada.sitracker.reader.SamlibSeekSearchStrategyImpl;
import com.andrada.sitracker.reader.SearchStrategy;
import com.github.kevinsawicki.http.HttpRequest;

import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import static com.andrada.sitracker.util.LogUtils.LOGD;
import static com.andrada.sitracker.util.LogUtils.LOGE;

public class SamlibSearchLoader extends AsyncTaskLoader<AsyncTaskResult<List<SearchedAuthor>>> {

    public static final int SEARCH_TOKEN = 0x3;
    private static final String TAG = "SamlibSearchLoader";

    private SearchStrategy mSearchStrategy;
    private List<SearchedAuthor> mAuthors = new ArrayList<SearchedAuthor>();
    private Uri mQuery;

    public SamlibSearchLoader(Context context, Uri query) {
        super(context);
        mQuery = query;
    }

    /**
     * Called when there is new data to deliver to the client.  The
     * super class will take care of delivering it; the implementation
     * here just adds a little more logic.
     */
    @Override
    public void deliverResult(@NotNull AsyncTaskResult<List<SearchedAuthor>> result) {
        if (isReset()) {
            // An async query came in while the loader is stopped.  We
            // don't need the result.
            if (result.getResult().size() != 0) {
                onReleaseResources(result.getResult());
            }
        }
        List<SearchedAuthor> oldAuthors = mAuthors;
        mAuthors = result.getResult();

        if (isStarted()) {
            // If the Loader is currently started, we can immediately
            // deliver its results.
            super.deliverResult(result);
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
            deliverResult(new AsyncTaskResult<List<SearchedAuthor>>(mAuthors, null));
        }
        if (takeContentChanged() || mAuthors.size() == 0) {
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
        // Force cancel the current load task.
        LOGD(TAG, "Force stopping task");
        //Sanity
        if (mSearchStrategy != null) {
            mSearchStrategy.cancelAnyRunningTasks();
        }
        cancelLoad();
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
            onReleaseResources(null);
        }
    }

    /**
     * Handles a request to cancel a load.
     */
    @Override
    public void onCanceled(AsyncTaskResult<List<SearchedAuthor>> result) {
        super.onCanceled(result);
        if (mSearchStrategy != null) {
            mSearchStrategy.cancelAnyRunningTasks();
        }
        // if needed.
        onReleaseResources(result.getResult());
    }

    @Override
    public AsyncTaskResult<List<SearchedAuthor>> loadInBackground() {
        int searchType = AppUriContract.getSearchTypeParam(mQuery);

        Exception possibleException = null;
        try {

            if (searchType == 0) {
                mSearchStrategy = new SamlibCgiSearchStrategyImpl();
            } else {
                mSearchStrategy = new SamlibSeekSearchStrategyImpl();
            }
            mAuthors.addAll(mSearchStrategy.searchForQuery(mQuery));
        } catch (MalformedURLException e) {
            possibleException = new SearchException(SearchException.SearchErrors.ERROR_UNKNOWN);
            LOGE(TAG, "Got 404", e);
        } catch (HttpRequest.HttpRequestException e) {
            possibleException = new SearchException(SearchException.SearchErrors.NETWORK_ERROR);
            LOGE(TAG, "Error reading stream", e);
        } catch (SearchException e) {
            possibleException = e;
            LOGE(TAG, "Samlib server is busy", e);
        } catch (InterruptedException e) {
            possibleException = new SearchException(SearchException.SearchErrors.INTERNAL_ERROR);
            LOGE(TAG, "Got thread interrupt", e);
        }
        return new AsyncTaskResult<List<SearchedAuthor>>(mAuthors, possibleException);
    }

    /**
     * Helper function to take care of releasing resources associated
     * with an actively loaded data set.
     */
    protected void onReleaseResources(List<SearchedAuthor> result) {
        // For a simple List<> there is nothing to do.  For something
        // like a Cursor, we would close it here.
        mAuthors.clear();
    }
}
