package com.microsoft.codepush.common.apirequests;

import android.os.AsyncTask;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.codepush.common.CodePush;
import com.microsoft.codepush.common.exceptions.CodePushApiHttpRequestException;
import com.microsoft.codepush.common.exceptions.CodePushFinalizeException;
import com.microsoft.codepush.common.utils.CodePushUtils;
import com.microsoft.codepush.common.utils.FileUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * This class represent basic code push http task.
 *
 * @param <T> type of the returned parameter.
 */
public abstract class BaseHttpTask<T> extends AsyncTask<Void, Void, T> {

    /**
     * Instance of {@link FileUtils} to work with.
     */
    protected FileUtils mFileUtils;

    /**
     * Instance of {@link CodePushUtils} to work with.
     */
    protected CodePushUtils mCodePushUtils;

    /**
     * URL for making request.
     */
    protected String mRequestUrl;

    /**
     * Inner exception that might occur during the task execution.
     */
    protected CodePushApiHttpRequestException mExecutionException;

    /**
     * Inner exception that might occur during the finalizing of resources that were opened during the task execution.
     */
    protected CodePushFinalizeException mFinalizeException;

    /**
     * Opens url connection for the provided url.
     *
     * @param urlString url to open.
     * @return instance of url connection.
     * @throws IOException read/write error occurred while accessing the file system.
     */
    public HttpURLConnection createConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        return (HttpURLConnection) url.openConnection();
    }

    /**
     * Gets exception that might occur during the execution of task.
     *
     * @return exception that might occur during the execution of task.
     */
    public CodePushApiHttpRequestException getInnerException() {
        CodePushApiHttpRequestException innerException = null;
        if (mExecutionException != null) {
            innerException = new CodePushApiHttpRequestException(mExecutionException);
        }
        if (mFinalizeException != null) {
            if (innerException != null) {

                /* Suppress finalize exception because we already have execution exception and
                 * can't pass finalizeException further due to API 16 restrictions
                 * (Throwable.addSuppressed() didn't exist then) */
                AppCenterLog.error(CodePush.LOG_TAG, mFinalizeException.getMessage(), mFinalizeException);
            } else {
                innerException = new CodePushApiHttpRequestException(mFinalizeException);
            }
        }
        return innerException;
    }
}
