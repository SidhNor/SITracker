package com.andrada.sitracker.reader;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.db.beans.SearchedAuthor;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SamlibAuthorSearchReader implements AuthorSearchReader {

    private static final String BASE_URL = "http://samlib.ru";

    @Override
    @NotNull
    public List<SearchedAuthor> getUniqueAuthorsFromPage(String pageContent) {
        List<SearchedAuthor> authors = new ArrayList<SearchedAuthor>();

        Pattern pattern = Pattern.compile(Constants.SAMLIB_AUTHOR_SEARCH_REGEX, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(pageContent);
        while (matcher.find()) {
            String authorUrl = matcher.group(1) == null ? "" : matcher.group(1);
            if (authorUrl.equals("")) {
                continue;
            }
            authorUrl = this.normalizeUrl(authorUrl);
            String authorName = matcher.group(2) == null ? "" : matcher.group(2);
            String descr = matcher.group(3) == null ? "" : matcher.group(3);
            SearchedAuthor auth = new SearchedAuthor(authorUrl, authorName, descr);
            if (!authors.contains(auth)) {
                authors.add(auth);
            }
        }

        return authors;
    }

    private String normalizeUrl(String value) {
        if (value.startsWith("/")) {
            value = BASE_URL + value;
        } else {
            value = BASE_URL + "/" + value;
        }
        if (value.endsWith("/")) {
            value = value + Constants.AUTHOR_PAGE_URL_ENDING_WO_SLASH;
        } else {
            value = value + Constants.AUTHOR_PAGE_URL_ENDING_WI_SLASH;
        }
        return value;
    }
}
