package com.andrada.sitracker.loader;

public class AsyncTaskResult<T> {
    private final T result;
    private final Exception error;

    public AsyncTaskResult(T result, Exception error) {
        this.result = result;
        this.error = error;
    }

    public T getResult() {
        return result;
    }

    public Exception getError() {
        return error;
    }
}
