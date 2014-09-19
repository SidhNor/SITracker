package com.andrada.sitracker.db.beans;


import org.jetbrains.annotations.NotNull;

public class SearchedAuthor {
    private String authorUrl;
    private String authorName;
    private String contextDescription;
    private int hitsInOnSearch;

    private static final int INDEX_OF_MAX = 50;
    private static final float NAME_WEIGHT = 0.3f;
    private static final float HITS_WEIGHT = 0.5f;


    public SearchedAuthor(@NotNull String authorUrl, String authorName, String contextDescription) {
        this.authorUrl = authorUrl;
        this.authorName = authorName;
        this.contextDescription = contextDescription;
        this.hitsInOnSearch = 1;
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

    public int getHitsInOnSearch() {
        return hitsInOnSearch;
    }

    public void recordSearchHit() {
        this.hitsInOnSearch++;
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

    public int weightedCompare(SearchedAuthor other, String query) {
        float myWeight = 1;
        float otherWeight = 1;
        int indexOfAuthOne = this.getAuthorName().toLowerCase().indexOf(query);
        int indexOfAuthTwo = other.getAuthorName().toLowerCase().indexOf(query);

        if (indexOfAuthOne != -1) {
            myWeight += (INDEX_OF_MAX - indexOfAuthOne) * NAME_WEIGHT;
        }
        if (indexOfAuthTwo != -1) {
            otherWeight += (INDEX_OF_MAX - indexOfAuthTwo) * NAME_WEIGHT;
        }
        if (this.getHitsInOnSearch() != 1) {
            myWeight += this.getHitsInOnSearch() * HITS_WEIGHT;
        }

        if (other.getHitsInOnSearch() != 1) {
            otherWeight += other.getHitsInOnSearch() * HITS_WEIGHT;
        }

        return Math.round(otherWeight - myWeight);
    }
}
