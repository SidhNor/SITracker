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

package com.andrada.sitracker.reader;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.exceptions.AddAuthorException;
import com.andrada.sitracker.util.SamlibPageHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SamlibAuthorPageReader implements AuthorPageReader {

    private String pageContent;

    public SamlibAuthorPageReader(String page) {
        this.pageContent = this.sanitizeHTML(page);
    }

    @Override
    public Author getAuthor(String url) throws AddAuthorException {
        Author author = new Author();
        author.setUrl(url);
        String urlId = SamlibPageHelper.getUrlIdFromCompleteUrl(url);
        author.setUrlId(urlId);
        author.setName(getAuthorName());
        author.setUpdateDate(getAuthorUpdateDate());
        author.setAuthorDescription(getAuthorDescription());
        author.setAuthorImageUrl(getAuthorImageUrl(url));
        return author;
    }

    @Override
    public List<Publication> getPublications(Author author) {
        ArrayList<Publication> publicationList = new ArrayList<Publication>();
        Pattern pattern = Pattern.compile(Constants.PUBLICATIONS_REGEX, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(pageContent);
        while (matcher.find()) {

            Publication item = new Publication();
            String baseUrl = author.getUrl().replace(Constants.AUTHOR_PAGE_URL_ENDING_WI_SLASH, "");
            baseUrl = baseUrl.replace(Constants.AUTHOR_PAGE_ALT_URL_ENDING_WI_SLASH, "");

            item.setAuthor(author);
            item.setUpdateDate(new Date());
            //Group 1 - LinkToText
            String itemURL = matcher.group(3) == null ? "" : matcher.group(3);
            item.setUrl(baseUrl + "/" + itemURL);
            //Group 2 - NameOfText
            String itemTitle = matcher.group(4) == null ? "" : matcher.group(4);
            item.setName(escapeHTML(itemTitle));
            //Group 3 - SizeOfText
            String sizeOfText = matcher.group(5) == null ? "0" : matcher.group(5);
            item.setSize(Integer.parseInt(sizeOfText));
            //Group 4 - DescriptionOfRating
            String descriptionOfRating = matcher.group(6) == null ? "" : matcher.group(6);
            item.setRating(escapeHTML(descriptionOfRating));
            //Group 5 - Rating
            String rating = matcher.group(7) == null ? "0" : matcher.group(7);
            //Group 6 - Section
            String categoryName = matcher.group(8) == null ? "" : matcher.group(8);
            item.setCategory(escapeHTML(categoryName).replace("@", ""));
            //Group 7 - Genres
            String genre = matcher.group(9) == null ? "" : matcher.group(9);
            //Group 8 - Link to Comments
            String commentsUrl = matcher.group(10) == null ? "" : matcher.group(10);
            item.setCommentUrl(commentsUrl);
            //Group 9 - CommentsDescription
            String commentsDescription = matcher.group(11) == null ? "" : matcher.group(11);
            //Group 10 - CommentsCount
            String commentsCount = matcher.group(12) == null ? "0" : matcher.group(12);
            item.setCommentsCount(Integer.parseInt(commentsCount));
            //Group 11 - Description
            String itemDescription = matcher.group(13) == null ? "" : matcher.group(13);
            item.setDescription(itemDescription.trim());
            item.setImageUrl(extractImage(itemDescription.trim()));
            publicationList.add(item);
        }
        return publicationList;
    }

    @Override
    public String getAuthorImageUrl(String authorUrl) {
        authorUrl = authorUrl.replace(Constants.AUTHOR_PAGE_URL_ENDING_WO_SLASH, "");
        authorUrl = authorUrl.replace(Constants.AUTHOR_PAGE_ALT_URL_ENDING_WO_SLASH, "");
        Pattern pattern = Pattern.compile(Constants.AUTHOR_IMAGE_REGEX, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(pageContent);
        String imageUrl = null;
        if (matcher.find()) {
            imageUrl = (matcher.group(2));
            if (imageUrl != null) imageUrl = authorUrl + imageUrl;
        }
        return imageUrl;
    }

    @Override
    public String getAuthorDescription() {
        Pattern pattern = Pattern.compile(Constants.AUTHOR_DESCRIPTION_TEXT_REGEX, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(pageContent);
        String descriptionText = null;
        if (matcher.find()) {
            descriptionText = (matcher.group(1));
        }
        return descriptionText;
    }

    @Override
    public boolean isPageBlank() {
        return pageContent == null || pageContent.length() == 0;
    }


    private String sanitizeHTML(String value) {
        value = value.replaceAll("(?i)<br />", "<br>")
                .replaceAll("(?i)&bull;?", " * ")
                .replaceAll("(?i)&lsaquo;?", "<")
                .replaceAll("(?i)&rsaquo;?", ">")
                .replaceAll("(?i)&trade;?", "(tm)")
                .replaceAll("(?i)&frasl;?", "/")
                .replaceAll("(?i)&lt;?", "<")
                .replaceAll("(?i)&gt;?", ">")
                .replaceAll("(?i)&copy;?", "(c)")
                .replaceAll("(?i)&reg;?", "(r)")
                .replaceAll("(?i)&nbsp;?", " ")
                .replaceAll("(?si)[\\r\\n\\x85\\f]+", "")
                .replaceAll("(?i)&quot;?", "\"");
        return value;
    }

    private static String escapeHTML(String value) {
        value = value.replaceAll("(?si)[\\r\\n\\x85\\f]+", "")
                .replaceAll("(?i)<(br|li)[^>]*>", "\n")
                .replaceAll("(?i)<td[^>]*>", "\t")
                .replaceAll("(?si)<script[^>]*>.*?</\\s*script[^>]*>", "")
                .replaceAll("<[^>]*>", "")
                .replaceAll("(?si)\\n[\\p{Z}\\t]+\\n", "\n\n")
                .replaceAll("(?si)\\n\\n+", "\\n\\n");
        return value;
    }

    private static String extractImage(String itemDescription) {
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
        int index = pageContent.indexOf('.', pageContent.indexOf("<title>")) + 1;
        if (index == -1) {
            throw new AddAuthorException(AddAuthorException.AuthorAddErrors.AUTHOR_NAME_NOT_FOUND);
        }
        int secondPointIndex = pageContent.indexOf(".", index);
        if (secondPointIndex == -1) {
            throw new AddAuthorException(AddAuthorException.AuthorAddErrors.AUTHOR_NAME_NOT_FOUND);
        }
        String authorName = pageContent.substring(index, secondPointIndex);
        if ("".equals(authorName.trim())) {
            throw new AddAuthorException(AddAuthorException.AuthorAddErrors.AUTHOR_NAME_NOT_FOUND);
        }
        return authorName;
    }

    private Date getAuthorUpdateDate() throws AddAuthorException {
        Pattern pattern = Pattern.compile(Constants.AUTHOR_UPDATE_DATE_REGEX, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(pageContent);
        Date date = new Date();
        if (matcher.find()) {
            SimpleDateFormat ft = new SimpleDateFormat(Constants.AUTHOR_UPDATE_DATE_FORMAT);
            try {
                date = ft.parse(matcher.group(1));
            } catch (ParseException e) {
                throw new AddAuthorException(AddAuthorException.AuthorAddErrors.AUTHOR_DATE_NOT_FOUND);
            }
        }
        return date;
    }
}
