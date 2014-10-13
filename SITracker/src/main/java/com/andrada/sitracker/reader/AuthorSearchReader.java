package com.andrada.sitracker.reader;

import com.andrada.sitracker.db.beans.SearchedAuthor;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface AuthorSearchReader {
    @NotNull
    Collection<SearchedAuthor> getUniqueAuthorsFromPage(String page);
}
