package com.andrada.sitracker.util;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.exceptions.AddAuthorException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SamlibPageParser {

    public static String getAuthor(String pageContent) throws AddAuthorException {
        int index = pageContent.indexOf('.', pageContent.indexOf("<title>")) + 1;
        int secondPointIndex = pageContent.indexOf(".", index);
        String authorName = pageContent.substring(index, secondPointIndex);
        if (authorName == null || "".equals(authorName.trim())) {
            throw new AddAuthorException(AddAuthorException.AuthorAddErrors.AUTHOR_NAME_NOT_FOUND);
        }
        return authorName;
    }

    public static Date getAuthorUpdateDate(String pageContent) throws AddAuthorException {
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

    public static List<Publication> getPublications(String body, Author author) {
        ArrayList<Publication> publicationList = new ArrayList<Publication>();
        Pattern pattern = Pattern.compile(Constants.PUBLICATIONS_REGEX, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(body);
        while (matcher.find()) {

            Publication item = new Publication();
            String baseUrl = author.getUrl().replace(Constants.AUTHOR_PAGE_URL_ENDING_WI_SLASH, "");

            item.setAuthor(author);
            item.setUpdateDate(new Date());
            //Group 1 - LinkToText
            String itemURL = matcher.group(1) == null ? "" : matcher.group(1);
            item.setUrl(baseUrl + "/" + itemURL);
            //Group 2 - NameOfText
            String itemTitle = matcher.group(2) == null ? "" : matcher.group(2);
            item.setName(escapeHTML(itemTitle));
            //Group 3 - SizeOfText
            String sizeOfText = matcher.group(3) == null ? "0" : matcher.group(3);
            item.setSize(Integer.parseInt(sizeOfText));
            //Group 4 - DescriptionOfRating
            String descriptionOfRating = matcher.group(4) == null ? "" : matcher.group(4);
            item.setRating(escapeHTML(descriptionOfRating));
            //Group 5 - Rating
            String rating = matcher.group(5) == null ? "0" : matcher.group(5);
            //Group 6 - Section
            String categoryName = matcher.group(6) == null ? "" : matcher.group(6);
            item.setCategory(escapeHTML(categoryName).replace("@", ""));
            //Group 7 - Genres
            String genre = matcher.group(7) == null ? "" : matcher.group(7);
            //Group 8 - Link to Comments
            String commentsUrl = matcher.group(8) == null ? "" : matcher.group(8);
            item.setCommentUrl(commentsUrl);
            //Group 9 - CommentsDescription
            String commentsDescription = matcher.group(9) == null ? "" : matcher.group(9);
            //Group 10 - CommentsCount
            String commentsCount = matcher.group(10) == null ? "0" : matcher.group(10);
            item.setCommentsCount(Integer.parseInt(commentsCount));
            //Group 11 - Description
            String itemDescription = matcher.group(11) == null ? "" : matcher.group(11);
            item.setDescription(escapeHTML(itemDescription).trim());
            publicationList.add(item);
        }
        return publicationList;
    }

    public static String sanitizeHTML(String value) {
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
                .replaceAll("(?i)&quot;?", "\"");
        return value;
    }

    public static String escapeHTML(String value) {
        value = value.replaceAll("(?si)[\\r\\n\\x85\\f]+", "")
                .replaceAll("(?i)<(br|li)[^>]*>", "\n")
                .replaceAll("(?i)<td[^>]*>", "\t")
                .replaceAll("(?si)<script[^>]*>.*?</\\s*script[^>]*>", "")
                .replaceAll("<[^>]*>", "")
                .replaceAll("(?si)\\n[\\p{Z}\\t]+\\n", "\n\n")
                .replaceAll("(?si)\\n\\n+", "\\n\\n");
        return value;
    }
}
