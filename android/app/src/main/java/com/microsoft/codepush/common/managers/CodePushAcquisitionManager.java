package com.microsoft.codepush.common.managers;

import android.os.AsyncTask;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.codepush.common.CodePushConfiguration;
import com.microsoft.codepush.common.connection.CheckForUpdateJob;
import com.microsoft.codepush.common.connection.ReportStatusJob;
import com.microsoft.codepush.common.datacontracts.CodePushDeploymentStatusReport;
import com.microsoft.codepush.common.datacontracts.CodePushDownloadStatusReport;
import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.datacontracts.CodePushRemotePackage;
import com.microsoft.codepush.common.datacontracts.CodePushReportStatusResult;
import com.microsoft.codepush.common.datacontracts.CodePushUpdateRequest;
import com.microsoft.codepush.common.datacontracts.CodePushUpdateResponse;
import com.microsoft.codepush.common.datacontracts.CodePushUpdateResponseUpdateInfo;
import com.microsoft.codepush.common.exceptions.CodePushMalformedDataException;
import com.microsoft.codepush.common.exceptions.CodePushQueryUpdateException;
import com.microsoft.codepush.common.exceptions.CodePushReportStatusException;
import com.microsoft.codepush.common.utils.CodePushUtils;
import com.microsoft.codepush.common.utils.FileUtils;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

import static com.microsoft.codepush.common.CodePush.LOG_TAG;

public class CodePushAcquisitionManager {

    /**
     * Endpoint for sending {@link CodePushDownloadStatusReport}.
     */
    final private String REPORT_DOWNLOAD_STATUS_ENDPOINT = "reportStatus/download";

    /**
     * Endpoint for sending {@link CodePushDeploymentStatusReport}.
     */
    final private String REPORT_DEPLOYMENT_STATUS_ENDPOINT = "reportStatus/deploy";

    /**
     * Query updates string pattern.
     */
    final private String UPDATE_CHECK_ENDPOINT = "updateCheck?%s";

    /**
     * Server url.
     */
    private String mServerUrl;

    /**
     * Version of the app from configuration.
     */
    private String mAppVersion;

    /**
     * Device id.
     */
    private String mClientUniqueId;

    /**
     * Current deployment key from configuration.
     */
    private String mDeploymentKey;

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
     * @param configuration current application configuration.
     * @param codePushUtils instance of {@link CodePushUtils} to work with.
     * @param fileUtils     instance of {@link FileUtils} to work with.
     */
    public CodePushAcquisitionManager(CodePushConfiguration configuration, CodePushUtils codePushUtils, FileUtils fileUtils) {
        mCodePushUtils = codePushUtils;
        mFileUtils = fileUtils;
        mServerUrl = configuration.getServerUrl();
        if (!mServerUrl.endsWith("/")) {
            mServerUrl += "/";
        }
        mAppVersion = configuration.getAppVersion();
        mClientUniqueId = configuration.getClientUniqueId();
        mDeploymentKey = configuration.getDeploymentKey();
    }

    /**
     * Sends a request to server for updates of the current package.
     *
     * @param currentPackage instance of {@link CodePushLocalPackage}.
     * @return {@link CodePushRemotePackage} or <code>null</code> if there is no update.
     * @throws CodePushQueryUpdateException exception occurred during querying for update.
     */
    public CodePushRemotePackage queryUpdateWithCurrentPackage(CodePushLocalPackage currentPackage) throws CodePushQueryUpdateException {
        if (currentPackage == null || currentPackage.getAppVersion() == null || currentPackage.getAppVersion().isEmpty()) {
            throw new CodePushQueryUpdateException("Calling common acquisition SDK with incorrect package");
        }
        CodePushUpdateRequest updateRequest = CodePushUpdateRequest.createUpdateRequest(mDeploymentKey, currentPackage, mClientUniqueId);
        try {
            final String requestUrl = mServerUrl + String.format(Locale.getDefault(), UPDATE_CHECK_ENDPOINT, mCodePushUtils.getQueryStringFromObject(updateRequest));
            CheckForUpdateJob checkForUpdateJob = new CheckForUpdateJob(mFileUtils, mCodePushUtils);
            checkForUpdateJob.setParameters(requestUrl);
            checkForUpdateJob.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            try {
                CodePushUpdateResponse codePushUpdateResponse = checkForUpdateJob.get();
                if (codePushUpdateResponse.isFailed()) {
                    throw codePushUpdateResponse.getCodePushQueryUpdateException();
                }
                CodePushUpdateResponseUpdateInfo updateInfo = codePushUpdateResponse.getUpdateInfo();
                if (updateInfo.isUpdateAppVersion()) {
                    return CodePushRemotePackage.createDefaultRemotePackage(updateInfo.getAppVersion(), updateInfo.isUpdateAppVersion());
                } else if (!updateInfo.isAvailable()) {
                    return null;
                }
                return CodePushRemotePackage.createRemotePackageFromUpdateInfo(mDeploymentKey, updateInfo);
            } catch (ExecutionException | InterruptedException e) {
                throw new CodePushQueryUpdateException(e, currentPackage.getPackageHash());
            }
        } catch (CodePushMalformedDataException e) {
            throw new CodePushQueryUpdateException(e, currentPackage.getPackageHash());
        }
    }

    /**
     * Sends deployment status to server.
     *
     * @param deploymentStatusReport instance of {@link CodePushDeploymentStatusReport}.
     * @throws CodePushReportStatusException exception occurred when sending the status.
     */
    public void reportStatusDeploy(CodePushDeploymentStatusReport deploymentStatusReport) throws CodePushReportStatusException {

        /* TODO: Consider moving the following logic to some other place or removing it at all if useless. */
        deploymentStatusReport.setClientUniqueId(mClientUniqueId);
        deploymentStatusReport.setDeploymentKey(mDeploymentKey);
        deploymentStatusReport.setAppVersion(deploymentStatusReport.getLocalPackage() != null ? deploymentStatusReport.getLocalPackage().getAppVersion() : mAppVersion);
        deploymentStatusReport.setAppVersion(deploymentStatusReport.getLocalPackage() != null ? deploymentStatusReport.getLocalPackage().getLabel() : null);
        final String requestUrl = mServerUrl + REPORT_DEPLOYMENT_STATUS_ENDPOINT;
        switch (deploymentStatusReport.getStatus()) {
            case SUCCEEDED:
            case FAILED:
                break;
            default: {
                if (deploymentStatusReport.getStatus() == null) {
                    throw new CodePushReportStatusException("Missing status argument.", CodePushReportStatusException.ReportType.DEPLOY);
                } else {
                    throw new CodePushReportStatusException("Unrecognized status \"" + deploymentStatusReport.getStatus().getValue() + "\".", CodePushReportStatusException.ReportType.DEPLOY);
                }
            }
        }
        deploymentStatusReport.setLocalPackage(deploymentStatusReport.getLocalPackage());
        final String deploymentStatusReportJsonString = mCodePushUtils.convertObjectToJsonString(deploymentStatusReport);
        ReportStatusJob reportStatusJob = new ReportStatusJob(mFileUtils);
        reportStatusJob.setParameters(requestUrl, deploymentStatusReportJsonString, CodePushReportStatusException.ReportType.DEPLOY);
        reportStatusJob.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        try {
            CodePushReportStatusResult codePushReportStatusResult = reportStatusJob.get();
            AppCenterLog.info(LOG_TAG, "Report status deploy: " + codePushReportStatusResult.getResult());
            if (codePushReportStatusResult.isFailed()) {
                throw codePushReportStatusResult.getCodePushReportStatusException();
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new CodePushReportStatusException(e, CodePushReportStatusException.ReportType.DEPLOY);
        }
    }

    /**
     * Sends download status to server.
     *
     * @param downloadedPackage instance of {@link CodePushLocalPackage} that has been downloaded.
     * @throws CodePushReportStatusException exception occurred when sending the status.
     */
    public void reportStatusDownload(CodePushLocalPackage downloadedPackage) throws CodePushReportStatusException {
        final String requestUrl = mServerUrl + REPORT_DOWNLOAD_STATUS_ENDPOINT;
        final CodePushDownloadStatusReport downloadStatusReport = CodePushDownloadStatusReport.createReport(mClientUniqueId, mDeploymentKey, downloadedPackage.getLabel());
        final String downloadStatusReportJsonString = mCodePushUtils.convertObjectToJsonString(downloadStatusReport);
        ReportStatusJob reportStatusJob = new ReportStatusJob(mFileUtils);
        reportStatusJob.setParameters(requestUrl, downloadStatusReportJsonString, CodePushReportStatusException.ReportType.DOWNLOAD);
        reportStatusJob.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        try {
            CodePushReportStatusResult codePushReportStatusResult = reportStatusJob.get();
            AppCenterLog.info(LOG_TAG, "Report status download: " + codePushReportStatusResult.getResult());
            if (codePushReportStatusResult.isFailed()) {
                throw codePushReportStatusResult.getCodePushReportStatusException();
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new CodePushReportStatusException(e, CodePushReportStatusException.ReportType.DOWNLOAD);
        }
    }
}
