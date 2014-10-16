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

package com.andrada.sitracker.reader;

import android.text.TextUtils;

import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.exceptions.AddAuthorException;
import com.andrada.sitracker.util.SamlibPageHelper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CgiSamlibAuthorPageReader implements AuthorPageReader {

    private static final int PUB_URL_INDEX = 0;
    private static final int AUTHOR_NAME_INDEX = 1;
    private static final int PUB_NAME_INDEX = 2;
    private static final int PUB_CATEGORY_INDEX = 3;
    private static final int PUB_SIZE_INDEX = 4;
    private static final int PUB_UPDATE_DATE_INDEX = 5;
    private static final int PUB_VOTE_RESULT = 6;
    private static final int PUB_VOTE_COUNT = 7;
    private static final int PUB_DESCRIPTION = 8;

    private static final String LINE_DIVIDER = "\n";
    private static final String COMPONENT_DIVIDER = "\\|";

    private static final String MIRROR_ROOT = "http://samlib.ru/";

    private String pageContent;

    public CgiSamlibAuthorPageReader(String page) {
        this.pageContent = page;
    }

    @NotNull
    @Override
    public Author getAuthor(String url) throws AddAuthorException {
        Author author = new Author();
        author.setUrl(url);
        String urlId = SamlibPageHelper.getUrlIdFromCompleteUrl(url);
        author.setUrlId(urlId);
        author.setName(getAuthorName());
        author.setUpdateDate(new Date());
        author.setAuthorDescription(getAuthorDescription());
        author.setAuthorImageUrl(getAuthorImageUrl(url));
        return author;
    }

    @NotNull
    @Override
    public List<Publication> getPublications(Author author) {
        ArrayList<Publication> publicationList = new ArrayList<Publication>();
        String[] lines = pageContent.split(LINE_DIVIDER);
        for (String pubLine : lines) {
            String[] components = pubLine.split(COMPONENT_DIVIDER);
            if (components.length != 9) {
                continue;
            }
            Publication item = new Publication();
            item.setAuthor(author);
            item.setUpdateDate(new Date());

            item.setUrl(MIRROR_ROOT + components[PUB_URL_INDEX] + ".shtml");
            item.setName(components[PUB_NAME_INDEX]);
            String sizeOfText = TextUtils.isEmpty(components[PUB_SIZE_INDEX].trim()) ? "0" : components[PUB_SIZE_INDEX];
            item.setSize(Integer.parseInt(sizeOfText));
            if (!TextUtils.isEmpty(components[PUB_VOTE_RESULT].trim()) &&
                    !TextUtils.isEmpty(components[PUB_VOTE_COUNT].trim())) {
                item.setRating(components[PUB_VOTE_RESULT] + "*" + components[PUB_VOTE_COUNT]);
            } else {
                item.setRating("");
            }
            item.setCategory(components[PUB_CATEGORY_INDEX]);
            //Link to Comments
            item.setCommentUrl(MIRROR_ROOT + "comment/" + components[PUB_URL_INDEX]);
            //No comment info for this method
            item.setCommentsCount(0);
            //Group 11 - Description
            String itemDescription = components[PUB_DESCRIPTION];
            item.setDescription(itemDescription.trim());
            item.setImageUrl(extractImage(itemDescription.trim()));

            item.setImagePageUrl(MIRROR_ROOT + "img/" + components[PUB_URL_INDEX] + "index.shtml");
            publicationList.add(item);
        }
        return publicationList;
    }

    @Nullable
    @Override
    public String getAuthorImageUrl(String authorUrl) {
        //No author image for this method
        return null;
    }

    @Nullable
    @Override
    public String getAuthorDescription() {
        //No description for this method
        return null;
    }

    @Override
    public boolean isPageBlank() {
        return pageContent == null || pageContent.length() == 0;
    }


    @Nullable
    private static String extractImage(@NotNull String itemDescription) {
        String imgUrl = null;

        Pattern pattern = Pattern.compile("(<a[^>]*>)?\\s*?<img src=[\"'](.*?)[\"'][^>]*>\\s?(</a>)?");
        Matcher matcher = pattern.matcher(itemDescription);
        if (matcher.find()) {
            String match = matcher.group(2);
            if (match != null) {
                imgUrl = match.trim();
            }
        }
        return imgUrl;
    }

    private String getAuthorName() throws AddAuthorException {
        String[] lines = pageContent.split(LINE_DIVIDER);
        if (lines.length <= 0) {
            throw new AddAuthorException(AddAuthorException.AuthorAddErrors.AUTHOR_NAME_NOT_FOUND);
        }
        String[] components = lines[0].split(COMPONENT_DIVIDER);
        if (components.length != 9) {
            throw new AddAuthorException(AddAuthorException.AuthorAddErrors.AUTHOR_NAME_NOT_FOUND);
        }
        String authorName = components[AUTHOR_NAME_INDEX];
        if ("".equals(authorName.trim())) {
            throw new AddAuthorException(AddAuthorException.AuthorAddErrors.AUTHOR_NAME_NOT_FOUND);
        }
        return authorName;
    }
}
