package com.microsoft.codepush.common.connection;

import com.microsoft.codepush.common.datacontracts.CodePushReportStatusResult;
import com.microsoft.codepush.common.exceptions.CodePushFinalizeException;
import com.microsoft.codepush.common.exceptions.CodePushReportStatusException;
import com.microsoft.codepush.common.utils.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

/**
 * Performs sending status reports to server.
 */
public class ReportStatusJob extends BaseJob<CodePushReportStatusResult> {

    /**
     * Url to send report to.
     */
    private String mRequestUrl;

    /**
     * Report as json string.
     */
    private String mJson;

    /**
     * Type of the report as listen in {@link CodePushReportStatusException.ReportType}.
     */
    private CodePushReportStatusException.ReportType mReportType;

    /**
     * Creates an instance of {@link ReportStatusJob}.
     *
     * @param fileUtils instance of {@link FileUtils} to work with.
     */
    public ReportStatusJob(FileUtils fileUtils) {
        mFileUtils = fileUtils;
    }

    /**
     * Sets additional parameters to the request.
     *
     * @param requestUrl url to send report to.
     * @param json       report as json string.
     * @param reportType type of the report as listed in {@link CodePushReportStatusException.ReportType}.
     */
    public void setParameters(String requestUrl, String json, CodePushReportStatusException.ReportType reportType) {
        mRequestUrl = requestUrl;
        mJson = json;
        mReportType = reportType;
    }

    @Override protected CodePushReportStatusResult doInBackground(Void... voids) {
        OutputStream outputStream = null;
        OutputStreamWriter outputStreamWriter = null;
        InputStream stream = null;
        Scanner scanner = null;
        HttpURLConnection connection;
        try {
            connection = createConnection(mRequestUrl);
        } catch (IOException e) {

            /* We can't throw custom errors from this function, so any error will be passed to the result. */
            return CodePushReportStatusResult.createFailed(new CodePushReportStatusException(e, mReportType));
        }
        try {
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.connect();
            outputStream = connection.getOutputStream();
            outputStreamWriter = new OutputStreamWriter(outputStream, "UTF-8");
            outputStreamWriter.write(mJson);
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
                CodePushReportStatusResult codePushReportStatusResult = CodePushReportStatusResult.createFailed(new CodePushReportStatusException(result, mReportType));
                codePushReportStatusResult.setResult(result);
                return codePushReportStatusResult;
            } else {
                return CodePushReportStatusResult.createSuccessful(result);
            }
        } catch (IOException e) {
            return CodePushReportStatusResult.createFailed(new CodePushReportStatusException(e, mReportType));
        } finally {
            Exception e = mFileUtils.finalizeResources(
                    Arrays.asList(outputStream, outputStreamWriter, stream, scanner),
                    null);
            if (e != null) {
                return CodePushReportStatusResult.createFailed(new CodePushReportStatusException(new CodePushFinalizeException(e), mReportType));
            }
        }
    }
}
