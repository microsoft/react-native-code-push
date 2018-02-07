package com.microsoft.codepush.common.connection;

import android.os.AsyncTask;

import com.microsoft.codepush.common.utils.CodePushUtils;
import com.microsoft.codepush.common.utils.FileUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * This class represent basic code push job.
 *
 * @param <T> type of the returned parameter.
 */
public abstract class BaseHttpJob<T> extends AsyncTask<Void, Void, T> {

    /**
     * Instance of {@link FileUtils} to work with.
     */
    protected FileUtils mFileUtils;

    /**
     * Instance of {@link CodePushUtils} to work with.
     */
    protected CodePushUtils mCodePushUtils;

    /**
     * Opens url connection for the provided url.
     *
     * @param urlString url to open.
     * @return instance of url connection.
     * @throws IOException read/write error occurred while accessing the file system.
     */
    public HttpURLConnection createConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection;
        connection = (HttpURLConnection) url.openConnection();
        return connection;
    }
}
