package com.microsoft.codepush.common.connection;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.codepush.common.datacontracts.CodePushUpdateResponse;
import com.microsoft.codepush.common.exceptions.CodePushFinalizeException;
import com.microsoft.codepush.common.exceptions.CodePushQueryUpdateException;
import com.microsoft.codepush.common.utils.CodePushUtils;
import com.microsoft.codepush.common.utils.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

import static com.microsoft.codepush.common.CodePush.LOG_TAG;

/**
 * Performs sending status reports to server.
 */
public class QueryUpdateJob extends BaseHttpJob<CodePushUpdateResponse> {

    /**
     * Url to query update against.
     */
    private String mRequestUrl;

    /**
     * Creates an instance of {@link QueryUpdateJob}.
     *
     * @param fileUtils     instance of {@link FileUtils} to work with.
     * @param codePushUtils instance of {@link CodePushUtils} to work with.
     */
    public QueryUpdateJob(FileUtils fileUtils, CodePushUtils codePushUtils) {
        mFileUtils = fileUtils;
        mCodePushUtils = codePushUtils;
    }

    /**
     * Sets additional parameters to the job.
     *
     * @param requestUrl url to query update against.
     */
    public void setParameters(String requestUrl) {
        mRequestUrl = requestUrl;
    }

    @Override
    protected CodePushUpdateResponse doInBackground(Void... voids) {
        InputStream stream = null;
        Scanner scanner = null;
        HttpURLConnection connection;
        try {
            connection = createConnection(mRequestUrl);
        } catch (IOException e) {

            /* We can't throw custom errors from this function, so any error will be passed to the result. */
            return CodePushUpdateResponse.createFailed(new CodePushQueryUpdateException(e));
        }
        try {
            boolean failed;
            if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                stream = connection.getInputStream();
                failed = false;
            } else {
                stream = connection.getErrorStream();
                failed = true;
            }
            scanner = new Scanner(stream).useDelimiter("\\A");
            String result = scanner.hasNext() ? scanner.next() : "";
            if (failed) {
                AppCenterLog.info(LOG_TAG, result);
                return CodePushUpdateResponse.createFailed(new CodePushQueryUpdateException(result));
            } else {
                return mCodePushUtils.convertStringToObject(result, CodePushUpdateResponse.class);
            }
        } catch (IOException e) {
            return CodePushUpdateResponse.createFailed(new CodePushQueryUpdateException(e));
        } finally {
            Exception e = mFileUtils.finalizeResources(
                    Arrays.asList(stream, scanner),
                    null);
            if (e != null) {
                return CodePushUpdateResponse.createFailed(new CodePushQueryUpdateException(new CodePushFinalizeException(e)));
            }
        }
    }
}
