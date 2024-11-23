package com.merino.ddfilms.utils;

public interface TaskCompletionCallback<T> {
    void onComplete(T result, Exception e);
}
