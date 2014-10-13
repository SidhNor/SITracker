package com.andrada.sitracker.loader;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.AsyncTaskLoader;

import com.andrada.sitracker.contracts.AppUriContract;
import com.andrada.sitracker.db.beans.SearchedAuthor;
import com.andrada.sitracker.exceptions.SearchException;
import com.andrada.sitracker.reader.SamlibAuthorSearchReader;
import com.github.kevinsawicki.http.HttpRequest;

import org.androidannotations.api.BackgroundExecutor;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.andrada.sitracker.util.LogUtils.LOGD;
import static com.andrada.sitracker.util.LogUtils.LOGE;

public class SamlibSearchLoader extends AsyncTaskLoader<AsyncTaskResult<List<SearchedAuthor>>> {

    public static final int SEARCH_TOKEN = 0x3;
    private static final String TAG = "SamlibSearchLoader";
    private static final String SEARCH_URL = "http://samlib.ru/cgi-bin/seek?DIR=%s&FIND=%s&PLACE=index&JANR=%d&TYPE=%d&PAGE=%d";
    private static final String DEFAULT_SAMLIB_ENCODING = "windows-1251";
    private static final String DEFAULT_DIR = "";
    private static final int DEFAULT_GENRE = 0;
    private static final int DEFAULT_TYPE = 0;

    private static final String BUFF_READER_ID = "bufferedReader";
    /**
     * Use search cache for 1 day only
     */
    private static final long MAX_STALE_CACHE = 60 * 60 * 24 * 1;

    volatile boolean finishedLoading = false;

    private List<SearchedAuthor> mAuthors = new ArrayList<SearchedAuthor>();
    private Uri mQuery;

    public SamlibSearchLoader(Context context, Uri query) {
        super(context);
        mQuery = query;
    }

    private void readData(BufferedReader reader, StringBuffer appendable) throws IOException {
        try {
            final CharBuffer buffer = CharBuffer.allocate(8192);
            int read;
            while ((read = reader.read(buffer)) != -1) {
                buffer.rewind();
                appendable.append(buffer, 0, read);
                buffer.rewind();
            }
        } catch (IOException e) {
            LOGE(TAG, "Could not read data", e);
        } finally {
            try {
                reader.close();
            } catch (IOException ignored) {

            }
            finishedLoading = true;
        }
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
        BackgroundExecutor.cancelAll(BUFF_READER_ID, true);
        //Sanity
        finishedLoading = true;
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
        BackgroundExecutor.cancelAll(BUFF_READER_ID, true);
        // if needed.
        onReleaseResources(result.getResult());
    }

    @Override
    public AsyncTaskResult<List<SearchedAuthor>> loadInBackground() {
        String searchString = AppUriContract.getSanitizedSearchQuer(mQuery);
        try {
            searchString = URLEncoder.encode(searchString, DEFAULT_SAMLIB_ENCODING);
        } catch (UnsupportedEncodingException ignored) {
            //Try to just search without encoding the query
        }
        String url = String.format(SEARCH_URL, DEFAULT_DIR, searchString, DEFAULT_GENRE, DEFAULT_TYPE, 1);
        Map<String, SearchedAuthor> hashAuthors = new HashMap<String, SearchedAuthor>();
        Exception possibleException = null;
        try {
            long requestStart = new Date().getTime();
            final HttpRequest request = HttpRequest.get(new URL(url));
            //Tolerate 1 day
            request.getConnection().addRequestProperty("Cache-Control", "max-stale=" + MAX_STALE_CACHE);
            if (request.code() == 404) {
                throw new MalformedURLException();
            }

            if (request.code() == 500) {
                throw new SearchException(SearchException.SearchErrors.SAMLIB_BUSY);
            }

            final StringBuffer buffer = new StringBuffer();
            final BufferedReader reader = request.bufferedReader();
            finishedLoading = false;
            LOGD(TAG, "Starting search: " + requestStart);
            BackgroundExecutor.execute(new BackgroundExecutor.Task(BUFF_READER_ID, 0, "") {
                @Override
                public void execute() {
                    try {
                        readData(reader, buffer);
                    } catch (Throwable e) {
                        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                    }
                }
            });
            Thread.sleep(500);
            long currentMils;
            boolean authNumberCriteriaSatisfied;
            boolean timeCriteriaSatisfied;
            do {
                currentMils = new Date().getTime();
                Collection<SearchedAuthor> authors = new SamlibAuthorSearchReader().getUniqueAuthorsFromPage(buffer.toString());
                for (SearchedAuthor auth : authors) {
                    if (!hashAuthors.containsKey(auth.getAuthorUrl())) {
                        hashAuthors.put(auth.getAuthorUrl(), auth);
                    }
                }
                LOGD(TAG, "Check for result availability. Mils passed: " + (currentMils - requestStart) + ". Unique authors got: " + hashAuthors.size());
                authNumberCriteriaSatisfied = hashAuthors.size() > 10;
                timeCriteriaSatisfied = (currentMils - requestStart) > 30000 && hashAuthors.size() != 0;
                Thread.sleep(500);
            } while (!finishedLoading && !authNumberCriteriaSatisfied && !timeCriteriaSatisfied);

            if (!finishedLoading) {
                LOGD(TAG, "Search conditions satisfied. Force stopping current request with " + hashAuthors.size() + " authors");
                finishedLoading = true;
                BackgroundExecutor.cancelAll(BUFF_READER_ID, true);
            }
            mAuthors.addAll(hashAuthors.values());
            final String unencodedQuery = AppUriContract.getSanitizedSearchQuer(mQuery).toLowerCase();
            Collections.sort(mAuthors, new Comparator<SearchedAuthor>() {
                @Override
                public int compare(SearchedAuthor searchedAuthor, SearchedAuthor searchedAuthor2) {
                    return searchedAuthor.weightedCompare(searchedAuthor2, unencodedQuery);
                }
            });

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
