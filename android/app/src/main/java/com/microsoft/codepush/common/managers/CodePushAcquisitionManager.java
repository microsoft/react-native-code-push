package com.microsoft.codepush.common.managers;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.codepush.common.CodePushConfiguration;
import com.microsoft.codepush.common.apirequests.ApiHttpRequest;
import com.microsoft.codepush.common.apirequests.CheckForUpdateTask;
import com.microsoft.codepush.common.apirequests.ReportStatusTask;
import com.microsoft.codepush.common.datacontracts.CodePushDeploymentStatusReport;
import com.microsoft.codepush.common.datacontracts.CodePushDownloadStatusReport;
import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.datacontracts.CodePushRemotePackage;
import com.microsoft.codepush.common.datacontracts.CodePushReportStatusResult;
import com.microsoft.codepush.common.datacontracts.CodePushUpdateRequest;
import com.microsoft.codepush.common.datacontracts.CodePushUpdateResponse;
import com.microsoft.codepush.common.datacontracts.CodePushUpdateResponseUpdateInfo;
import com.microsoft.codepush.common.exceptions.CodePushApiHttpRequestException;
import com.microsoft.codepush.common.exceptions.CodePushIllegalArgumentException;
import com.microsoft.codepush.common.exceptions.CodePushMalformedDataException;
import com.microsoft.codepush.common.exceptions.CodePushQueryUpdateException;
import com.microsoft.codepush.common.exceptions.CodePushReportStatusException;
import com.microsoft.codepush.common.utils.CodePushUtils;
import com.microsoft.codepush.common.utils.FileUtils;

import java.util.Locale;

import static com.microsoft.codepush.common.CodePush.LOG_TAG;
import static com.microsoft.codepush.common.enums.ReportType.DEPLOY;
import static com.microsoft.codepush.common.enums.ReportType.DOWNLOAD;

public class CodePushAcquisitionManager {

    /**
     * Endpoint for sending {@link CodePushDownloadStatusReport}.
     */
    final private static String REPORT_DOWNLOAD_STATUS_ENDPOINT = "reportStatus/download";

    /**
     * Endpoint for sending {@link CodePushDeploymentStatusReport}.
     */
    final private static String REPORT_DEPLOYMENT_STATUS_ENDPOINT = "reportStatus/deploy";

    /**
     * Query updates string pattern.
     */
    final private static String UPDATE_CHECK_ENDPOINT = "updateCheck?%s";

    /**
     * Instance of {@link CodePushUtils} to work with.
     */
    private CodePushUtils mCodePushUtils;

    /**
     * Instance of {@link FileUtils} to work with.
     */
    private FileUtils mFileUtils;

    /**
     * Creates an instance of {@link CodePushAcquisitionManager}.
     *
     * @param codePushUtils instance of {@link CodePushUtils} to work with.
     * @param fileUtils     instance of {@link FileUtils} to work with.
     */
    public CodePushAcquisitionManager(CodePushUtils codePushUtils, FileUtils fileUtils) {
        mCodePushUtils = codePushUtils;
        mFileUtils = fileUtils;
    }

    private String fixServerUrl(String serverUrl) {
        if (!serverUrl.endsWith("/")) {
            serverUrl += "/";
        }
        return serverUrl;
    }

    /**
     * Sends a request to server for updates of the current package.
     *
     * @param configuration  current application configuration.
     * @param currentPackage instance of {@link CodePushLocalPackage}.
     * @return {@link CodePushRemotePackage} or <code>null</code> if there is no update.
     * @throws CodePushQueryUpdateException exception occurred during querying for update.
     */
    public CodePushRemotePackage queryUpdateWithCurrentPackage(CodePushConfiguration configuration, CodePushLocalPackage currentPackage) throws CodePushQueryUpdateException {
        if (currentPackage == null || currentPackage.getAppVersion() == null || currentPackage.getAppVersion().isEmpty()) {
            throw new CodePushQueryUpdateException("Calling common acquisition SDK with incorrect package");
        }

        /* Extract parameters from configuration */
        String serverUrl = fixServerUrl(configuration.getServerUrl());
        String deploymentKey = configuration.getDeploymentKey();
        String clientUniqueId = configuration.getClientUniqueId();
        try {
            CodePushUpdateRequest updateRequest = CodePushUpdateRequest.createUpdateRequest(deploymentKey, currentPackage, clientUniqueId);
            final String requestUrl = serverUrl + String.format(Locale.getDefault(), UPDATE_CHECK_ENDPOINT, mCodePushUtils.getQueryStringFromObject(updateRequest, "UTF-8"));
            CheckForUpdateTask checkForUpdateTask = new CheckForUpdateTask(mFileUtils, mCodePushUtils, requestUrl);
            ApiHttpRequest<CodePushUpdateResponse> checkForUpdateRequest = new ApiHttpRequest<>(checkForUpdateTask);
            try {
                CodePushUpdateResponse codePushUpdateResponse = checkForUpdateRequest.makeRequest();
                CodePushUpdateResponseUpdateInfo updateInfo = codePushUpdateResponse.getUpdateInfo();
                if (updateInfo.isUpdateAppVersion()) {
                    return CodePushRemotePackage.createDefaultRemotePackage(updateInfo.getAppVersion(), updateInfo.isUpdateAppVersion());
                } else if (!updateInfo.isAvailable()) {
                    return null;
                }
                return CodePushRemotePackage.createRemotePackageFromUpdateInfo(deploymentKey, updateInfo);
            } catch (CodePushApiHttpRequestException e) {
                throw new CodePushQueryUpdateException(e, currentPackage.getPackageHash());
            }
        } catch (CodePushMalformedDataException | CodePushIllegalArgumentException e) {
            throw new CodePushQueryUpdateException(e, currentPackage.getPackageHash());
        }
    }

    /**
     * Sends deployment status to server.
     *
     * @param configuration          current application configuration.
     * @param deploymentStatusReport instance of {@link CodePushDeploymentStatusReport}.
     * @throws CodePushReportStatusException exception occurred when sending the status.
     */
    public void reportStatusDeploy(CodePushConfiguration configuration, CodePushDeploymentStatusReport deploymentStatusReport) throws CodePushReportStatusException {

        /* Extract parameters from configuration */
        String appVersion = configuration.getAppVersion();
        String serverUrl = fixServerUrl(configuration.getServerUrl());
        String deploymentKey = configuration.getDeploymentKey();
        String clientUniqueId = configuration.getClientUniqueId();

        try {
            deploymentStatusReport.setClientUniqueId(clientUniqueId);
            deploymentStatusReport.setDeploymentKey(deploymentKey);
            deploymentStatusReport.setAppVersion(deploymentStatusReport.getPackage() != null ? deploymentStatusReport.getPackage().getAppVersion() : appVersion);
            deploymentStatusReport.setLabel(deploymentStatusReport.getPackage() != null ? deploymentStatusReport.getPackage().getLabel() : null);
        } catch (CodePushIllegalArgumentException e) {
            throw new CodePushReportStatusException(e, DEPLOY);
        }
        final String requestUrl = serverUrl + REPORT_DEPLOYMENT_STATUS_ENDPOINT;
        switch (deploymentStatusReport.getStatus()) {
            case SUCCEEDED:
            case FAILED:
                break;
            default: {
                if (deploymentStatusReport.getStatus() == null) {
                    throw new CodePushReportStatusException("Missing status argument.", DEPLOY);
                } else {
                    throw new CodePushReportStatusException("Unrecognized status \"" + deploymentStatusReport.getStatus().getValue() + "\".", DEPLOY);
                }
            }
        }
        deploymentStatusReport.setPackage(deploymentStatusReport.getPackage());
        final String deploymentStatusReportJsonString = mCodePushUtils.convertObjectToJsonString(deploymentStatusReport);
        ReportStatusTask reportStatusDeployTask = new ReportStatusTask(mFileUtils, requestUrl, deploymentStatusReportJsonString, DEPLOY);
        ApiHttpRequest<CodePushReportStatusResult> reportStatusDeployRequest = new ApiHttpRequest<>(reportStatusDeployTask);
        try {
            CodePushReportStatusResult codePushReportStatusResult = reportStatusDeployRequest.makeRequest();
            AppCenterLog.info(LOG_TAG, "Report status deploy: " + codePushReportStatusResult.getResult());
        } catch (CodePushApiHttpRequestException e) {
            throw new CodePushReportStatusException(e, DEPLOY);
        }
    }

    /**
     * Sends download status to server.
     *
     * @param configuration     current application configuration.
     * @param downloadedPackage instance of {@link CodePushLocalPackage} that has been downloaded.
     * @throws CodePushReportStatusException exception occurred when sending the status.
     */
    public void reportStatusDownload(CodePushConfiguration configuration, CodePushLocalPackage downloadedPackage) throws CodePushReportStatusException {

        /* Extract parameters from configuration */
        String serverUrl = fixServerUrl(configuration.getServerUrl());
        String deploymentKey = configuration.getDeploymentKey();
        String clientUniqueId = configuration.getClientUniqueId();
        final String requestUrl = serverUrl + REPORT_DOWNLOAD_STATUS_ENDPOINT;
        try {
            final CodePushDownloadStatusReport downloadStatusReport = CodePushDownloadStatusReport.createReport(clientUniqueId, deploymentKey, downloadedPackage.getLabel());
            final String downloadStatusReportJsonString = mCodePushUtils.convertObjectToJsonString(downloadStatusReport);
            ReportStatusTask reportStatusDownloadTask = new ReportStatusTask(mFileUtils, requestUrl, downloadStatusReportJsonString, DOWNLOAD);
            ApiHttpRequest<CodePushReportStatusResult> reportStatusDownloadRequest = new ApiHttpRequest<>(reportStatusDownloadTask);
            CodePushReportStatusResult codePushReportStatusResult = reportStatusDownloadRequest.makeRequest();
            AppCenterLog.info(LOG_TAG, "Report status download: " + codePushReportStatusResult.getResult());
        } catch (CodePushApiHttpRequestException | CodePushIllegalArgumentException e) {
            throw new CodePushReportStatusException(e, DOWNLOAD);
        }
    }
}
