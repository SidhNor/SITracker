package com.andrada.sitracker.reader;

import com.andrada.sitracker.db.beans.SearchedAuthor;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface AuthorSearchReader {
    @NotNull
    List<SearchedAuthor> getUniqueAuthorsFromPage(String page);
}
