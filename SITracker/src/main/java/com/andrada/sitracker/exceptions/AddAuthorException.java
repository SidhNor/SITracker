package com.andrada.sitracker.exceptions;

/**
 * Created by ggodonoga on 03/06/13.
 */
public class AddAuthorException extends Exception {

    private AuthorAddErrors mError = AuthorAddErrors.AUTHOR_UNKNOWN;

    public enum AuthorAddErrors {
        AUTHOR_UNKNOWN,
        AUTHOR_NAME_NOT_FOUND,
        AUTHOR_DATE_NOT_FOUND,
        AUTHOR_ALREADY_EXISTS,
        AUTHOR_NO_PUBLICATIONS
    }

    public AddAuthorException(AuthorAddErrors error) {
        super();
        mError = error;
    }

    public AuthorAddErrors getError() {
        return mError;
    }

    public AddAuthorException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
