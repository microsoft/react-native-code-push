package com.microsoft.codepush.common;

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
import com.microsoft.codepush.common.enums.CodePushSyncStatus;
import com.microsoft.codepush.common.enums.CodePushUpdateState;
import com.microsoft.codepush.common.utils.CodePushDownloadPackageResult;
import com.microsoft.appcenter.utils.AppCenterLog;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;

/**
 * Tests all the data classes.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(AppCenterLog.class)
public class CodePushDataTest {
    private String clientUniqueId = "YHFv65";
    private String deploymentKey = "ABC123";
    private String previousDeploymentKey = "prevABC123";
    private String previousLabel = "awesome package previous";
    private String appVersion = "2.2.1";
    private String status = "Succeeded";
    private String label = "awesome package";
    private String description = "short description";
    private boolean failedInstall = false;
    private boolean isMandatory = true;
    private boolean isPending = true;
    private boolean isDebugOnly = false;
    private boolean isFirstRun = false;
    private boolean updateAppVersion = true;
    private boolean isAvailable = true;
    private boolean shouldRunBinary = false;
    private long packageSize = 102546723;
    private String downloadUrl = "https://url.com";
    private String packageHash = "HASH";
    private String error = "An error has occurred";

    @Test
    public void enumsTest() throws Exception {
        CodePushCheckFrequency codePushCheckFrequency = CodePushCheckFrequency.MANUAL;
        int checkFrequencyValue = codePushCheckFrequency.getValue();
        assertEquals(2, checkFrequencyValue);

        CodePushDeploymentStatus codePushDeploymentStatus = CodePushDeploymentStatus.SUCCEEDED;
        String deploymentStatusValue = codePushDeploymentStatus.getValue();
        assertEquals("DeploymentSucceeded", deploymentStatusValue);

        CodePushInstallMode codePushInstallMode = CodePushInstallMode.IMMEDIATE;
        int installModeValue = codePushInstallMode.getValue();
        assertEquals(0, installModeValue);

        CodePushSyncStatus codePushSyncStatus = CodePushSyncStatus.AWAITING_USER_ACTION;
        int syncStatusValue = codePushSyncStatus.getValue();
        assertEquals(6, syncStatusValue);

        CodePushUpdateState codePushUpdateState = CodePushUpdateState.LATEST;
        int updateStateValue = codePushUpdateState.getValue();
        assertEquals(2, updateStateValue);
    }

    @Test
    public void dataClassesTest() throws Exception {

        /* Checks DownloadPackageResult work.*/
        File file = new File("/");
        CodePushDownloadPackageResult codePushDownloadPackageResult = new CodePushDownloadPackageResult(file, false);
        assertEquals(false, codePushDownloadPackageResult.isZip());
        assertEquals(file, codePushDownloadPackageResult.getDownloadFile());
    }

    @Test
    public void dataContractsTest() throws Exception {

        /* Check download report. */
        CodePushDownloadStatusReport codePushDownloadStatusReport = CodePushDownloadStatusReport.createReport(clientUniqueId, deploymentKey, label);
        checkDownloadReport(codePushDownloadStatusReport);

        /* Check deployment report. */
        CodePushDeploymentStatusReport codePushDeploymentStatusReport = new CodePushDeploymentStatusReport();
        codePushDeploymentStatusReport.setClientUniqueId(clientUniqueId);
        codePushDeploymentStatusReport.setDeploymentKey(deploymentKey);
        codePushDeploymentStatusReport.setLabel(label);
        codePushDeploymentStatusReport.setAppVersion(appVersion);
        codePushDeploymentStatusReport.setPreviousDeploymentKey(previousDeploymentKey);
        codePushDeploymentStatusReport.setPreviousLabelOrAppVersion(previousLabel);
        codePushDeploymentStatusReport.setStatus(status);
        checkDeploymentReport(codePushDeploymentStatusReport);

        /* Check update response info. */
        CodePushUpdateResponseUpdateInfo codePushUpdateResponseUpdateInfo = new CodePushUpdateResponseUpdateInfo();
        codePushUpdateResponseUpdateInfo.setAppVersion(appVersion);
        codePushUpdateResponseUpdateInfo.setAvailable(isAvailable);
        codePushUpdateResponseUpdateInfo.setDescription(description);
        codePushUpdateResponseUpdateInfo.setDownloadUrl(downloadUrl);
        codePushUpdateResponseUpdateInfo.setLabel(label);
        codePushUpdateResponseUpdateInfo.setMandatory(isMandatory);
        codePushUpdateResponseUpdateInfo.setPackageHash(packageHash);
        codePushUpdateResponseUpdateInfo.setPackageSize(packageSize);
        codePushUpdateResponseUpdateInfo.setShouldRunBinaryVersion(shouldRunBinary);
        codePushUpdateResponseUpdateInfo.setUpdateAppVersion(updateAppVersion);
        checkUpdateResponse(codePushUpdateResponseUpdateInfo);

        /* Check update response. */
        CodePushUpdateResponse codePushUpdateResponse = new CodePushUpdateResponse();
        codePushUpdateResponse.setUpdateInfo(codePushUpdateResponseUpdateInfo);
        assertEquals(codePushUpdateResponseUpdateInfo, codePushUpdateResponse.getUpdateInfo());

        /* Check package. */
        CodePushPackage codePushPackage = new CodePushPackage();
        codePushPackage.setAppVersion(appVersion);
        codePushPackage.setDeploymentKey(deploymentKey);
        codePushPackage.setDescription(description);
        codePushPackage.setFailedInstall(failedInstall);
        codePushPackage.setLabel(label);
        codePushPackage.setMandatory(isMandatory);
        codePushPackage.setPackageHash(packageHash);
        checkPackage(codePushPackage);

        /* Check local package. */
        CodePushLocalPackage codePushLocalPackage = CodePushLocalPackage.createLocalPackage(failedInstall, isFirstRun, isPending, isDebugOnly, codePushPackage);
        checkLocalPackage(codePushLocalPackage);
        CodePushLocalPackage failedPackage = CodePushLocalPackage.createFailedLocalPackage(new Exception(error));
        assertEquals(error, failedPackage.getDownloadException().getMessage());

        /* Check remote package. */
        CodePushRemotePackage codePushDefaultRemotePackage = CodePushRemotePackage.createDefaultRemotePackage(appVersion, updateAppVersion);
        assertEquals(appVersion, codePushDefaultRemotePackage.getAppVersion());
        assertEquals(updateAppVersion, codePushDefaultRemotePackage.isUpdateAppVersion());
        CodePushRemotePackage codePushRemotePackage = CodePushRemotePackage.createRemotePackage(failedInstall, packageSize, downloadUrl, updateAppVersion, codePushPackage);
        checkRemotePackage(codePushRemotePackage);
        CodePushRemotePackage codePushUpdateRemotePackage = CodePushRemotePackage.createRemotePackageFromUpdateInfo(deploymentKey, codePushUpdateResponseUpdateInfo);
        checkRemotePackage(codePushUpdateRemotePackage);

        /* Check update request. */
        CodePushUpdateRequest codePushUpdateRequest = CodePushUpdateRequest.createUpdateRequest(deploymentKey, codePushLocalPackage, clientUniqueId);
        codePushUpdateRequest.setCompanion(false);
        assertEquals(deploymentKey, codePushUpdateRequest.getDeploymentKey());
        assertEquals(clientUniqueId, codePushUpdateRequest.getClientUniqueId());
        assertEquals(codePushLocalPackage.getAppVersion(), codePushUpdateRequest.getAppVersion());
        assertEquals(codePushLocalPackage.getLabel(), codePushUpdateRequest.getLabel());
        assertEquals(codePushLocalPackage.getPackageHash(), codePushUpdateRequest.getPackageHash());
        assertEquals(false, codePushUpdateRequest.isCompanion());

        /* Verify errors are logged. */
        PowerMockito.mockStatic(AppCenterLog.class);
        codePushUpdateRequest.setDeploymentKey(null);
        codePushUpdateRequest.setAppVersion(null);
        codePushDownloadStatusReport.setLabel(null);
        codePushDownloadStatusReport.setClientUniqueId(null);
        codePushDownloadStatusReport.setDeploymentKey(null);
        codePushDeploymentStatusReport.setAppVersion(null);
        codePushDeploymentStatusReport.setPreviousDeploymentKey(null);
        codePushUpdateResponse.setUpdateInfo(null);
        PowerMockito.verifyStatic(VerificationModeFactory.times(8));
        AppCenterLog.error(eq(CodePush.LOG_TAG), anyString());

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
        CodePushSyncOptions codePushSyncOptions = new CodePushSyncOptions(deploymentKey);
        codePushSyncOptions.setUpdateDialog(codePushUpdateDialog);
        assertEquals(deploymentKey, codePushSyncOptions.getDeploymentKey());
        assertEquals(0, codePushSyncOptions.getMinimumBackgroundDuration());
        assertEquals(CodePushInstallMode.ON_NEXT_RESTART, codePushSyncOptions.getInstallMode());
        assertEquals(CodePushInstallMode.IMMEDIATE, codePushSyncOptions.getMandatoryInstallMode());
        assertEquals(true, codePushSyncOptions.getIgnoreFailedUpdates());
        assertEquals(codePushUpdateDialog, codePushSyncOptions.getUpdateDialog());
        assertEquals(CodePushCheckFrequency.ON_APP_START, codePushSyncOptions.getCheckFrequency());
    }

    private void checkDeploymentReport(CodePushDeploymentStatusReport codePushDeploymentStatusReport) {
        assertEquals(appVersion, codePushDeploymentStatusReport.getAppVersion());
        assertEquals(previousDeploymentKey, codePushDeploymentStatusReport.getPreviousDeploymentKey());
        assertEquals(previousLabel, codePushDeploymentStatusReport.getPreviousLabelOrAppVersion());
        assertEquals(status, codePushDeploymentStatusReport.getStatus());
        checkDownloadReport(codePushDeploymentStatusReport);
    }

    private void checkDownloadReport(CodePushDownloadStatusReport codePushDownloadStatusReport) {
        assertEquals(clientUniqueId, codePushDownloadStatusReport.getClientUniqueId());
        assertEquals(deploymentKey, codePushDownloadStatusReport.getDeploymentKey());
        assertEquals(label, codePushDownloadStatusReport.getLabel());
    }

    private void checkLocalPackage(CodePushLocalPackage codePushLocalPackage) {
        assertEquals(isFirstRun, codePushLocalPackage.isFirstRun());
        assertEquals(isPending, codePushLocalPackage.isPending());
        assertEquals(isDebugOnly, codePushLocalPackage.isDebugOnly());
        checkPackage(codePushLocalPackage);
    }

    private void checkRemotePackage(CodePushRemotePackage codePushRemotePackage) {
        assertEquals(updateAppVersion, codePushRemotePackage.isUpdateAppVersion());
        assertEquals(packageSize, codePushRemotePackage.getPackageSize());
        assertEquals(downloadUrl, codePushRemotePackage.getDownloadUrl());
        checkPackage(codePushRemotePackage);
    }

    private void checkPackage(CodePushPackage codePushPackage) {
        assertEquals(appVersion, codePushPackage.getAppVersion());
        assertEquals(deploymentKey, codePushPackage.getDeploymentKey());
        assertEquals(description, codePushPackage.getDescription());
        assertEquals(failedInstall, codePushPackage.isFailedInstall());
        assertEquals(label, codePushPackage.getLabel());
        assertEquals(isMandatory, codePushPackage.isMandatory());
        assertEquals(packageHash, codePushPackage.getPackageHash());
    }

    private void checkUpdateResponse(CodePushUpdateResponseUpdateInfo codePushUpdateResponseUpdateInfo) {
        assertEquals(appVersion, codePushUpdateResponseUpdateInfo.getAppVersion());
        assertEquals(isAvailable, codePushUpdateResponseUpdateInfo.isAvailable());
        assertEquals(description, codePushUpdateResponseUpdateInfo.getDescription());
        assertEquals(downloadUrl, codePushUpdateResponseUpdateInfo.getDownloadUrl());
        assertEquals(label, codePushUpdateResponseUpdateInfo.getLabel());
        assertEquals(isMandatory, codePushUpdateResponseUpdateInfo.isMandatory());
        assertEquals(packageHash, codePushUpdateResponseUpdateInfo.getPackageHash());
        assertEquals(packageSize, codePushUpdateResponseUpdateInfo.getPackageSize());
        assertEquals(shouldRunBinary, codePushUpdateResponseUpdateInfo.isShouldRunBinaryVersion());
        assertEquals(updateAppVersion, codePushUpdateResponseUpdateInfo.isUpdateAppVersion());
    }
}