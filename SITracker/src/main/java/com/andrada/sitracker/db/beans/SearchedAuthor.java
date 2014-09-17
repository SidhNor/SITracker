package com.andrada.sitracker.db.beans;

import org.jetbrains.annotations.NotNull;

public class SearchedAuthor {
    private String authorUrl;
    private String authorName;
    private String contextDescription;

    public SearchedAuthor(@NotNull String authorUrl, String authorName, String contextDescription) {
        this.authorUrl = authorUrl;
        this.authorName = authorName;
        this.contextDescription = contextDescription;
    }

    public String getAuthorUrl() {
        return authorUrl;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getContextDescription() {
        return contextDescription;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SearchedAuthor authorVO = (SearchedAuthor) o;
        return authorUrl.equals(authorVO.authorUrl);
    }

    @Override
    public int hashCode() {
        return authorUrl.hashCode();
    }
}
