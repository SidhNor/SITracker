package com.andrada.sitracker.exceptions;

public class SearchException extends Exception {

    private static final long serialVersionUID = -8354408170538207702L;
    private SearchErrors mError = SearchErrors.ERROR_UNKNOWN;

    public enum SearchErrors {
        ERROR_UNKNOWN,
        SAMLIB_BUSY,
        NETWORK_ERROR,
        INTERNAL_ERROR
    }

    public SearchException(SearchErrors mError) {
        super();
        this.mError = mError;
    }

    public SearchErrors getError() {
        return mError;
    }
}
