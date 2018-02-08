package com.microsoft.codepush.common.apirequests;

import android.os.AsyncTask;

import com.microsoft.codepush.common.exceptions.CodePushApiHttpRequestException;

import java.util.concurrent.ExecutionException;

/**
 * Represents request to CodePush server.
 *
 * @param <T> result of execution of request.
 */
public class ApiHttpRequest<T> {

    /**
     * Task for making request.
     */
    private BaseHttpTask<T> mRequestTask;

    /**
     * Creates an instance of {@link ApiHttpRequest}.
     *
     * @param mRequestTask Task for making request.
     */
    public ApiHttpRequest(BaseHttpTask<T> mRequestTask) {
        this.mRequestTask = mRequestTask;
    }

    /**
     * Makes request to CodePush server.
     *
     * @return result of execution of request.
     * @throws CodePushApiHttpRequestException if there was error during the execution of request.
     */
    public T makeRequest() throws CodePushApiHttpRequestException {
        mRequestTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        T taskResult;
        try {
            taskResult = mRequestTask.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new CodePushApiHttpRequestException(e);
        }
        CodePushApiHttpRequestException innerException = mRequestTask.getInnerException();
        if (innerException != null) {
            throw innerException;
        }
        return taskResult;
    }
}