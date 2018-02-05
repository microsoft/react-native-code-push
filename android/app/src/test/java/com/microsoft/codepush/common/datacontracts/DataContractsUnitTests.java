package com.microsoft.codepush.common.datacontracts;

import com.microsoft.codepush.common.datacontracts.CodePushDeploymentStatusReport;
import com.microsoft.codepush.common.datacontracts.CodePushDownloadStatusReport;
import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.datacontracts.CodePushPackage;
import com.microsoft.codepush.common.datacontracts.CodePushRemotePackage;
import com.microsoft.codepush.common.datacontracts.CodePushSyncOptions;
import com.microsoft.codepush.common.datacontracts.CodePushUpdateDialog;
import com.microsoft.codepush.common.datacontracts.CodePushUpdateRequest;
import com.microsoft.codepush.common.datacontracts.CodePushUpdateResponse;
import com.microsoft.codepush.common.datacontracts.CodePushUpdateResponseUpdateInfo;
import com.microsoft.codepush.common.enums.CodePushCheckFrequency;
import com.microsoft.codepush.common.enums.CodePushDeploymentStatus;
import com.microsoft.codepush.common.enums.CodePushInstallMode;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests all the data classes.
 */
public class DataContractsUnitTests {

    private final static String CLIENT_UNIQUE_ID = "YHFv65";
    private final static String DEPLOYMENT_KEY = "ABC123";
    private final static String LABEL = "awesome package";
    private final static boolean FAILED_INSTALL = false;
    private final static boolean IS_PENDING = true;
    private final static boolean IS_DEBUG_ONLY = false;
    private final static boolean IS_FIRST_RUN = false;
    private final static String PREVIOUS_DEPLOYMENT_KEY = "prevABC123";
    private final static String PREVIOUS_LABEL = "awesome package previous";
    private final static String APP_VERSION = "2.2.1";
    private final static String DESCRIPTION = "short description";
    private final static boolean IS_MANDATORY = true;
    private final static boolean UPDATE_APP_VERSION = true;
    private final static boolean IS_AVAILABLE = true;
    private final static boolean SHOULD_RUN_BINARY = false;
    private final static long PACKAGE_SIZE = 102546723;
    private final static String DOWNLOAD_URL = "https://url.com";
    private final static String PACKAGE_HASH = "HASH";
    private final static String ERROR = "An error has occurred";
    private final static String APP_ENTRY_POINT = "/www/index.html";

    @Test
    public void dataContractsTest() throws Exception {

        /* Check download report. */
        CodePushDownloadStatusReport codePushDownloadStatusReport = CodePushDownloadStatusReport.createReport(CLIENT_UNIQUE_ID, DEPLOYMENT_KEY, LABEL);
        checkDownloadReport(codePushDownloadStatusReport);

        /* Check deployment report. */
        CodePushDeploymentStatusReport codePushDeploymentStatusReport = new CodePushDeploymentStatusReport();
        codePushDeploymentStatusReport.setClientUniqueId(CLIENT_UNIQUE_ID);
        codePushDeploymentStatusReport.setDeploymentKey(DEPLOYMENT_KEY);
        codePushDeploymentStatusReport.setLabel(LABEL);
        codePushDeploymentStatusReport.setAppVersion(APP_VERSION);
        codePushDeploymentStatusReport.setPreviousDeploymentKey(PREVIOUS_DEPLOYMENT_KEY);
        codePushDeploymentStatusReport.setPreviousLabelOrAppVersion(PREVIOUS_LABEL);
        codePushDeploymentStatusReport.setStatus(CodePushDeploymentStatus.SUCCEEDED);
        checkDeploymentReport(codePushDeploymentStatusReport);

        /* Check update response info. */
        CodePushUpdateResponseUpdateInfo codePushUpdateResponseUpdateInfo = new CodePushUpdateResponseUpdateInfo();
        codePushUpdateResponseUpdateInfo.setAppVersion(APP_VERSION);
        codePushUpdateResponseUpdateInfo.setAvailable(IS_AVAILABLE);
        codePushUpdateResponseUpdateInfo.setDescription(DESCRIPTION);
        codePushUpdateResponseUpdateInfo.setDownloadUrl(DOWNLOAD_URL);
        codePushUpdateResponseUpdateInfo.setLabel(LABEL);
        codePushUpdateResponseUpdateInfo.setMandatory(IS_MANDATORY);
        codePushUpdateResponseUpdateInfo.setPackageHash(PACKAGE_HASH);
        codePushUpdateResponseUpdateInfo.setPackageSize(PACKAGE_SIZE);
        codePushUpdateResponseUpdateInfo.setShouldRunBinaryVersion(SHOULD_RUN_BINARY);
        codePushUpdateResponseUpdateInfo.setUpdateAppVersion(UPDATE_APP_VERSION);
        checkUpdateResponse(codePushUpdateResponseUpdateInfo);

        /* Check update response. */
        CodePushUpdateResponse codePushUpdateResponse = new CodePushUpdateResponse();
        codePushUpdateResponse.setUpdateInfo(codePushUpdateResponseUpdateInfo);
        assertEquals(codePushUpdateResponseUpdateInfo, codePushUpdateResponse.getUpdateInfo());

        /* Check package. */
        CodePushPackage codePushPackage = new CodePushPackage();
        codePushPackage.setAppVersion(APP_VERSION);
        codePushPackage.setDeploymentKey(DEPLOYMENT_KEY);
        codePushPackage.setDescription(DESCRIPTION);
        codePushPackage.setFailedInstall(FAILED_INSTALL);
        codePushPackage.setLabel(LABEL);
        codePushPackage.setMandatory(IS_MANDATORY);
        codePushPackage.setPackageHash(PACKAGE_HASH);
        checkPackage(codePushPackage);

        /* Check local package. */
        CodePushLocalPackage codePushLocalPackage = CodePushLocalPackage.createLocalPackage(FAILED_INSTALL, IS_FIRST_RUN, IS_PENDING, IS_DEBUG_ONLY, APP_ENTRY_POINT, codePushPackage);
        checkLocalPackage(codePushLocalPackage);
        CodePushLocalPackage failedPackage = CodePushLocalPackage.createFailedLocalPackage(new Exception(ERROR));
        assertEquals(ERROR, failedPackage.getDownloadException().getMessage());

        /* Check remote package. */
        CodePushRemotePackage codePushDefaultRemotePackage = CodePushRemotePackage.createDefaultRemotePackage(APP_VERSION, UPDATE_APP_VERSION);
        assertEquals(APP_VERSION, codePushDefaultRemotePackage.getAppVersion());
        assertEquals(UPDATE_APP_VERSION, codePushDefaultRemotePackage.isUpdateAppVersion());
        CodePushRemotePackage codePushRemotePackage = CodePushRemotePackage.createRemotePackage(FAILED_INSTALL, PACKAGE_SIZE, DOWNLOAD_URL, UPDATE_APP_VERSION, codePushPackage);
        checkRemotePackage(codePushRemotePackage);
        CodePushRemotePackage codePushUpdateRemotePackage = CodePushRemotePackage.createRemotePackageFromUpdateInfo(DEPLOYMENT_KEY, codePushUpdateResponseUpdateInfo);
        checkRemotePackage(codePushUpdateRemotePackage);

        /* Check update request. */
        CodePushUpdateRequest codePushUpdateRequest = CodePushUpdateRequest.createUpdateRequest(DEPLOYMENT_KEY, codePushLocalPackage, CLIENT_UNIQUE_ID);
        codePushUpdateRequest.setCompanion(false);
        assertEquals(DEPLOYMENT_KEY, codePushUpdateRequest.getDeploymentKey());
        assertEquals(CLIENT_UNIQUE_ID, codePushUpdateRequest.getClientUniqueId());
        assertEquals(codePushLocalPackage.getAppVersion(), codePushUpdateRequest.getAppVersion());
        assertEquals(codePushLocalPackage.getLabel(), codePushUpdateRequest.getLabel());
        assertEquals(codePushLocalPackage.getPackageHash(), codePushUpdateRequest.getPackageHash());
        assertEquals(false, codePushUpdateRequest.isCompanion());

        /* Check update dialog. */
        CodePushUpdateDialog codePushUpdateDialog = new CodePushUpdateDialog();
        assertEquals("An update is available that must be installed.", codePushUpdateDialog.getMandatoryUpdateMessage());
        assertEquals("An update is available. Would you like to install it?", codePushUpdateDialog.getOptionalUpdateMessage());
        assertEquals("Description: ", codePushUpdateDialog.getDescriptionPrefix());
        assertEquals("Continue", codePushUpdateDialog.getMandatoryContinueButtonLabel());
        assertEquals("Ignore", codePushUpdateDialog.getOptionalIgnoreButtonLabel());
        assertEquals("Install", codePushUpdateDialog.getOptionalInstallButtonLabel());
        assertEquals("Update available", codePushUpdateDialog.getTitle());
        assertEquals(false, codePushUpdateDialog.getAppendReleaseDescription());

        /* Check sync options. */
        CodePushSyncOptions codePushSyncOptions = new CodePushSyncOptions(DEPLOYMENT_KEY);
        codePushSyncOptions.setUpdateDialog(codePushUpdateDialog);
        assertEquals(DEPLOYMENT_KEY, codePushSyncOptions.getDeploymentKey());
        assertEquals(0, codePushSyncOptions.getMinimumBackgroundDuration());
        assertEquals(CodePushInstallMode.ON_NEXT_RESTART, codePushSyncOptions.getInstallMode());
        assertEquals(CodePushInstallMode.IMMEDIATE, codePushSyncOptions.getMandatoryInstallMode());
        assertEquals(true, codePushSyncOptions.getIgnoreFailedUpdates());
        assertEquals(codePushUpdateDialog, codePushSyncOptions.getUpdateDialog());
        assertEquals(CodePushCheckFrequency.ON_APP_START, codePushSyncOptions.getCheckFrequency());
    }

    private void checkDeploymentReport(CodePushDeploymentStatusReport codePushDeploymentStatusReport) {
        assertEquals(APP_VERSION, codePushDeploymentStatusReport.getAppVersion());
        assertEquals(PREVIOUS_DEPLOYMENT_KEY, codePushDeploymentStatusReport.getPreviousDeploymentKey());
        assertEquals(PREVIOUS_LABEL, codePushDeploymentStatusReport.getPreviousLabelOrAppVersion());
        assertEquals(CodePushDeploymentStatus.SUCCEEDED, codePushDeploymentStatusReport.getStatus());
        checkDownloadReport(codePushDeploymentStatusReport);
    }

    private void checkDownloadReport(CodePushDownloadStatusReport codePushDownloadStatusReport) {
        assertEquals(CLIENT_UNIQUE_ID, codePushDownloadStatusReport.getClientUniqueId());
        assertEquals(DEPLOYMENT_KEY, codePushDownloadStatusReport.getDeploymentKey());
        assertEquals(LABEL, codePushDownloadStatusReport.getLabel());
    }

    private void checkLocalPackage(CodePushLocalPackage codePushLocalPackage) {
        assertEquals(IS_FIRST_RUN, codePushLocalPackage.isFirstRun());
        assertEquals(IS_PENDING, codePushLocalPackage.isPending());
        assertEquals(IS_DEBUG_ONLY, codePushLocalPackage.isDebugOnly());
        checkPackage(codePushLocalPackage);
    }

    private void checkRemotePackage(CodePushRemotePackage codePushRemotePackage) {
        assertEquals(UPDATE_APP_VERSION, codePushRemotePackage.isUpdateAppVersion());
        assertEquals(PACKAGE_SIZE, codePushRemotePackage.getPackageSize());
        assertEquals(DOWNLOAD_URL, codePushRemotePackage.getDownloadUrl());
        checkPackage(codePushRemotePackage);
    }

    private void checkPackage(CodePushPackage codePushPackage) {
        assertEquals(APP_VERSION, codePushPackage.getAppVersion());
        assertEquals(DEPLOYMENT_KEY, codePushPackage.getDeploymentKey());
        assertEquals(DESCRIPTION, codePushPackage.getDescription());
        assertEquals(FAILED_INSTALL, codePushPackage.isFailedInstall());
        assertEquals(LABEL, codePushPackage.getLabel());
        assertEquals(IS_MANDATORY, codePushPackage.isMandatory());
        assertEquals(PACKAGE_HASH, codePushPackage.getPackageHash());
    }

    private void checkUpdateResponse(CodePushUpdateResponseUpdateInfo codePushUpdateResponseUpdateInfo) {
        assertEquals(APP_VERSION, codePushUpdateResponseUpdateInfo.getAppVersion());
        assertEquals(IS_AVAILABLE, codePushUpdateResponseUpdateInfo.isAvailable());
        assertEquals(DESCRIPTION, codePushUpdateResponseUpdateInfo.getDescription());
        assertEquals(DOWNLOAD_URL, codePushUpdateResponseUpdateInfo.getDownloadUrl());
        assertEquals(LABEL, codePushUpdateResponseUpdateInfo.getLabel());
        assertEquals(IS_MANDATORY, codePushUpdateResponseUpdateInfo.isMandatory());
        assertEquals(PACKAGE_HASH, codePushUpdateResponseUpdateInfo.getPackageHash());
        assertEquals(PACKAGE_SIZE, codePushUpdateResponseUpdateInfo.getPackageSize());
        assertEquals(SHOULD_RUN_BINARY, codePushUpdateResponseUpdateInfo.isShouldRunBinaryVersion());
        assertEquals(UPDATE_APP_VERSION, codePushUpdateResponseUpdateInfo.isUpdateAppVersion());
    }
}