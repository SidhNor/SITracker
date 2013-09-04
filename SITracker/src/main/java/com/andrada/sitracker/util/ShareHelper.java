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

package com.andrada.sitracker.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ShareHelper {

    public static Intent getSharePublicationIntent(Uri file) {
        Intent share = new Intent(Intent.ACTION_VIEW);
        share.addCategory(Intent.CATEGORY_DEFAULT);
        share.setDataAndType(file, "text/html");
        return share;
    }

    /**
     * Get a usable File of the publication on external storage
     *
     * @param context               The context to use
     * @param hashedPublicationName A unique hash of the publication url
     * @return The file or null if storage is not accessible.
     */
    public static File getPublicationStorageFile(Context context, String hashedPublicationName) {

        File storageDir = getPublicationStorageDirectory(context);
        if (storageDir == null) {
            return storageDir;
        }

        return new File(storageDir, hashedPublicationName + ".html");
    }

    public static File getPublicationStorageFileWithPath(Context context, String path, String filename) {
        File storageDir = getExternalDirectoryBasedOnPath(path);
        if (storageDir == null) {
            return storageDir;
        }

        return new File(storageDir, sanitizeFileName(filename + ".html"));
    }

    private static String sanitizeFileName(String badFileName) {
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
    public static File getPublicationStorageDirectory(Context context) {
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
    public static File getExternalDirectoryBasedOnPath(String path) {
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
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + path);
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

    /**
     * Saves a file with specified html page content and character set.
     * If the page does not contain a meta Content-Type header, it is added and the files is saved as UTF-8
     *
     * @param file    The file to save to
     * @param content String content to save
     * @param charSet Character set to use during save
     * @return true if save was successful, false otherwise
     */
    public static boolean saveHtmlPageToFile(File file, String content, String charSet) {
        boolean result = true;
        if (!content.contains("<meta http-equiv=\"Content-Type\"")) {
            //Use UTF-8
            charSet = "UTF-8";
            content = content.replace("<head>",
                    "<head><meta http-equiv=\"Content-Type\" content=\"text/html;charset=UTF-8\">");
        }
        try {
            BufferedOutputStream bs;
            FileOutputStream fs = new FileOutputStream(file);
            bs = new BufferedOutputStream(fs);
            bs.write(content.getBytes(charSet));
            bs.close();

        } catch (IOException e) {
            result = false;
        }

        return result;
    }

}
