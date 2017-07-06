package com.microsoft.codepush.react.managers;

import android.os.AsyncTask;

import com.microsoft.codepush.react.CodePushConfiguration;
import com.microsoft.codepush.react.exceptions.CodePushUnknownException;
import com.microsoft.codepush.react.utils.CodePushUtils;
import com.microsoft.codepush.react.datacontracts.CodePushDeploymentStatusReport;
import com.microsoft.codepush.react.datacontracts.CodePushDownloadStatusReport;
import com.microsoft.codepush.react.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.react.datacontracts.CodePushRemotePackage;
import com.microsoft.codepush.react.datacontracts.CodePushStatusReport;
import com.microsoft.codepush.react.datacontracts.CodePushUpdateRequest;
import com.microsoft.codepush.react.datacontracts.CodePushUpdateResponse;
import com.microsoft.codepush.react.datacontracts.CodePushUpdateResponseUpdateInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

public class CodePushAcquisitionManager {
    private String mServerUrl;
    private String mAppVersion;
    private String mClientUniqueId;
    private String mDeploymentKey;

    public CodePushAcquisitionManager(CodePushConfiguration configuration) {
        mServerUrl = configuration.ServerUrl;
        if (!mServerUrl.endsWith("/")) {
            mServerUrl += "/";
        };
        mAppVersion = configuration.AppVersion;
        mClientUniqueId = configuration.ClientUniqueId;
        mDeploymentKey = configuration.DeploymentKey;
    }

    public CodePushRemotePackage queryUpdateWithCurrentPackage(CodePushLocalPackage currentPackage) {
        if (currentPackage == null || currentPackage.AppVersion == null || currentPackage.AppVersion.isEmpty()) {
            throw new IllegalArgumentException("Calling common acquisition SDK with incorrect package");
        }

        CodePushUpdateRequest updateRequest = new CodePushUpdateRequest(
                mDeploymentKey,
                currentPackage.AppVersion,
                currentPackage.PackageHash,
                false,
                currentPackage.Label,
                mClientUniqueId
        );

        final String requestUrl = mServerUrl + "updateCheck?" + CodePushUtils.getQueryStringFromObject(updateRequest);
        try {
            URL url = new URL(requestUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if(connection.getResponseCode() == HttpsURLConnection.HTTP_OK){
                InputStream inputStream = connection.getInputStream();
                Scanner s = new Scanner(inputStream).useDelimiter("\\A");
                String result = s.hasNext() ? s.next() : "";
                CodePushUpdateResponseUpdateInfo updateInfo = CodePushUtils.convertStringToObject(result, CodePushUpdateResponse.class).UpdateInfo;
                if (updateInfo == null) {
                    throw new CodePushUnknownException(result);
                } else if (updateInfo.UpdateAppVersion) {
                    return new CodePushRemotePackage(
                            updateInfo.AppVersion,
                            null,
                            null,
                            false,
                            false,
                            null,
                            null,
                            0,
                            null,
                            updateInfo.UpdateAppVersion);
                } else if (!updateInfo.IsAvailable) {
                    return null;
                }

                return new CodePushRemotePackage(
                        updateInfo.AppVersion,
                        mDeploymentKey,
                        updateInfo.Description,
                        false,
                        updateInfo.IsMandatory,
                        updateInfo.Label,
                        updateInfo.PackageHash,
                        updateInfo.PackageSize,
                        updateInfo.DownloadUrl,
                        updateInfo.UpdateAppVersion);
            } else {
                InputStream inputStream = connection.getErrorStream();
                Scanner s = new Scanner(inputStream).useDelimiter("\\A");
                String result = s.hasNext() ? s.next() : "";
                CodePushUtils.log(result);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    };

    public boolean reportStatusDeploy(CodePushStatusReport statusReport) {
        final String requestUrl = mServerUrl + "reportStatus/download";

        if (statusReport.Package != null) {
            switch (statusReport.Status) {
                case SUCCEEDED:
                case FAILED:
                    break;
                default: {
                    if (statusReport.Status == null) {
                        throw new IllegalArgumentException("Missing status argument.");
                    } else {
                        throw new IllegalArgumentException("Unrecognized status \"" + statusReport.Status + "\".");
                    }
                }
            }
        }

        final CodePushDeploymentStatusReport deploymentStatusReport =
                new CodePushDeploymentStatusReport(
                        statusReport.Package != null ? statusReport.Package.AppVersion : mAppVersion,
                        mDeploymentKey,
                        mClientUniqueId,
                        statusReport.PreviousDeploymentKey,
                        statusReport.PreviousLabelOrAppVersion,
                        statusReport.Package != null ? statusReport.Package.Label : null,
                        statusReport.Status.getValue()
                );
        final String deploymentStatusReportJsonString = CodePushUtils.convertObjectToJsonString(deploymentStatusReport);

        try {
            URL url = new URL(requestUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type","application/json");
            connection.connect();

            OutputStream os = connection.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
            osw.write(deploymentStatusReportJsonString);
            osw.flush();
            osw.close();
            os.close();

            InputStream stream;
            if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                stream = connection.getInputStream();
                Scanner s = new Scanner(stream).useDelimiter("\\A");
                String result = s.hasNext() ? s.next() : "";
                CodePushUtils.log("Report status deploy: " + result);
                return true;
            } else  {
                stream = connection.getErrorStream();
                Scanner s = new Scanner(stream).useDelimiter("\\A");
                String result = s.hasNext() ? s.next() : "";
                CodePushUtils.log("Report status deploy: " + result);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void reportStatusDownload(CodePushLocalPackage downloadedPackage) {
        final String requestUrl = mServerUrl + "reportStatus/download";
        final CodePushDownloadStatusReport downloadStatusReport = new CodePushDownloadStatusReport(mClientUniqueId, mDeploymentKey, downloadedPackage.Label);
        final String downloadStatusReportJsonString = CodePushUtils.convertObjectToJsonString(downloadStatusReport);

        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    URL url = new URL(requestUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setUseCaches(false);
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type","application/json");
                    connection.connect();

                    OutputStream os = connection.getOutputStream();
                    OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
                    osw.write(downloadStatusReportJsonString);
                    osw.flush();
                    osw.close();
                    os.close();

                    InputStream stream;
                    if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                        stream = connection.getInputStream();
                    } else  {
                        stream = connection.getErrorStream();
                    }
                    Scanner s = new Scanner(stream).useDelimiter("\\A");
                    String result = s.hasNext() ? s.next() : "";
                    CodePushUtils.log("Report status download: " + result);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };

        asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
