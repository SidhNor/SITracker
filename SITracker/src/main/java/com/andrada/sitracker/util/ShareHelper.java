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

package com.andrada.sitracker.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.exceptions.SharePublicationException;
import com.github.kevinsawicki.http.HttpRequest;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.andrada.sitracker.util.LogUtils.LOGW;

public final class ShareHelper {

    @NotNull
    public static Intent getSharePublicationIntent(Uri file) {
        Intent share = new Intent(Intent.ACTION_VIEW);
        share.addCategory(Intent.CATEGORY_DEFAULT);
        share.setDataAndType(file, "text/html");
        return share;
    }

    /**
     * Fetches publication html file into downloadFolder or default files folder
     *
     * @param context       of the App, can be null if pubFolder is not blank
     * @param pub           the actual publication to download
     * @param pubFolder     folder to download or to look into
     * @param forceDownload if the file exists
     * @return Intent for downloaded or existing file
     * @throws SharePublicationException
     */
    public static Intent fetchPublication(Context context, @NotNull Publication pub,
                                          String pubFolder, boolean forceDownload)
            throws SharePublicationException {

        String pubUrl = pub.getUrl();
        File file;
        if (TextUtils.isEmpty(pubFolder)) {
            file = ShareHelper.getPublicationStorageFile(context,
                    pub.getAuthor().getName() + "_" + pub.getName());
        } else {
            file = ShareHelper.getPublicationStorageFileWithPath(pubFolder,
                    pub.getAuthor().getName() + "_" + pub.getName());
        }

        if (file == null) {
            throw new SharePublicationException(
                    SharePublicationException.SharePublicationErrors.STORAGE_NOT_ACCESSIBLE_FOR_PERSISTANCE);
        }

        if (forceDownload ||
                !file.exists() ||
                file.lastModified() < pub.getUpdateDate().getTime()) {
            try {
                String cgiPubUrl = Constants.SAMLIB_CGI_PUBLICAITON_URL + SamlibPageHelper.getReducedUrlFromCompletePublicationUrl(pubUrl);
                URL publicaitonUrl = new URL(cgiPubUrl);
                HttpRequest request = HttpRequest.get(publicaitonUrl);
                if (request.code() == 200) {
                    BufferedReader reader = request.bufferedReader(Constants.DEFAULT_SAMLIB_ENCODING);
                    boolean result = ShareHelper.saveHtmlPageToFile(file, reader);
                    if (!result) {
                        throw new SharePublicationException(
                                SharePublicationException.SharePublicationErrors.COULD_NOT_PERSIST);
                    }
                } else {
                    throw new SharePublicationException(
                            SharePublicationException.SharePublicationErrors.COULD_NOT_LOAD);
                }
            } catch (MalformedURLException e) {
                throw new SharePublicationException(
                        SharePublicationException.SharePublicationErrors.WRONG_PUBLICATION_URL);
            } catch (HttpRequest.HttpRequestException e) {
                throw new SharePublicationException(
                        SharePublicationException.SharePublicationErrors.COULD_NOT_LOAD);
            }
        }
        return getSharePublicationIntent(Uri.fromFile(file));
    }

    public static boolean shouldRefreshPublication(Context context, @NotNull Publication pub,
                                                   String pubFolder) {
        File file;
        if (TextUtils.isEmpty(pubFolder)) {
            file = ShareHelper.getPublicationStorageFile(context,
                    pub.getAuthor().getName() + "_" + pub.getName());
        } else {
            file = ShareHelper.getPublicationStorageFileWithPath(pubFolder,
                    pub.getAuthor().getName() + "_" + pub.getName());
        }
        return file == null || !file.exists() || file.lastModified() < pub.getUpdateDate().getTime();
    }

    /**
     * Get a usable File of the publication on external storage
     *
     * @param context               The context to use
     * @param hashedPublicationName A unique hash of the publication url
     * @return The file or null if storage is not accessible.
     */
    @Nullable
    public static File getPublicationStorageFile(@NotNull Context context, String hashedPublicationName) {

        File storageDir = getPublicationStorageDirectory(context);
        if (storageDir == null) {
            return null;
        }
        return new File(storageDir, hashedPublicationName + ".html");
    }

    @Nullable
    public static File getPublicationStorageFileWithPath(String path, String filename) {
        File storageDir = getExternalDirectoryBasedOnPath(path);
        if (storageDir == null) {
            return null;
        }

        return new File(storageDir, sanitizeFileName(filename + ".html"));
    }

    private static String sanitizeFileName(@NotNull String badFileName) {
        final String pattern = "[^0-9\\s_\\p{L}\\(\\)%\\-\\.]";
        StringBuffer cleanFileName = new StringBuffer();
        Pattern filePattern = Pattern.compile(pattern);
        Matcher fileMatcher = filePattern.matcher(badFileName);
        boolean match = fileMatcher.find();
        while (match) {
            fileMatcher.appendReplacement(cleanFileName, "");
            match = fileMatcher.find();
        }
        fileMatcher.appendTail(cleanFileName);
        return cleanFileName.substring(0, cleanFileName.length() > 126 ? 126 : cleanFileName.length());
    }

    /**
     * Get a the external directory name.
     *
     * @param context The context to use
     * @return external directory path, null if directory not available
     */
    @Nullable
    public static File getPublicationStorageDirectory(@NotNull Context context) {
        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise return null
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) &&
                Environment.isExternalStorageRemovable()) {
            return null;
        }
        return context.getExternalFilesDir(null);
    }

    /**
     * Get the external sd card directory based on the specified path.
     *
     * @param path path to try
     * @return File instance or null if storage is not accessible or path is invalid
     */
    @Nullable
    @Contract("null -> null")
    public static File getExternalDirectoryBasedOnPath(@Nullable String path) {
        //Sanity check 1
        if (path == null) {
            return null;
        }
        //Sanity check 2
        if (path.indexOf("/") != 0) {
            path = "/" + path;
        }
        //Sanity check 3
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) &&
                Environment.isExternalStorageRemovable()) {
            return null;
        }
        //Path here is always absolute.
        //File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(/*sdCard.getAbsolutePath() + */path);
        //Make sure we create directories if they do not exist
        if (!dir.exists()) {
            dir.mkdirs();
        }
        //Sanity check 5
        if (dir.exists() && dir.isDirectory()) {
            return dir;
        } else {
            return null;
        }
    }

    @NotNull
    public static String getTimestampFilename(@NotNull String prefix, @NotNull String extension) {
        SimpleDateFormat fmt = new SimpleDateFormat("dd-MM-yyyy");
        return prefix + fmt.format(new Date()) + extension;
    }

    /**
     * Saves a file with specified html page content and character set.
     * If the page does not contain a meta Content-Type header, it is added and the files is saved as UTF-8
     *
     * @param file   The file to save to
     * @param reader Buffered reader of content to save
     * @return true if save was successful, false otherwise
     */
    public static boolean saveHtmlPageToFile(@NotNull File file, @NotNull BufferedReader reader) {
        boolean result = true;

        BufferedOutputStream bs = null;
        FileOutputStream fs = null;

        try {
            fs = new FileOutputStream(file);
            bs = new BufferedOutputStream(fs);

            String line;
            String charSet = "UTF-8";

            line = reader.readLine();
            String[] str = line.split("\\|");
            bs.write("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html;charset=UTF-8\">".getBytes(charSet));
            bs.write(("<title>" + str[1] + "</title></head><body>").getBytes(charSet));
            bs.write(("<center><h3>" + str[0] + "</h3></center><br>").getBytes(charSet));
            bs.write(("<center><h1>" + str[1] + "</h1></center>").getBytes(charSet));

            while ((line = reader.readLine()) != null) {
                bs.write(line.getBytes(charSet));
            }
            bs.write("</body></html>".getBytes(charSet));
            bs.flush();
            bs.close();

        } catch (IOException e) {
            result = false;
        } finally {
            if (bs != null) {
                try {
                    bs.close();
                } catch (IOException e) {
                    LOGW(Constants.APP_TAG, "Could not closed saved html page", e);
                }
            } else if (fs != null) {
                try {
                    fs.close();
                } catch (IOException e) {
                    LOGW(Constants.APP_TAG, "Could not closed saved html page", e);
                }
            }
        }

        return result;
    }

}
