package com.microsoft.codepush.common;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.codepush.common.datacontracts.CodePushDeploymentStatusReport;
import com.microsoft.codepush.common.datacontracts.CodePushDownloadStatusReport;
import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.datacontracts.CodePushPackage;
import com.microsoft.codepush.common.datacontracts.CodePushUpdateRequest;
import com.microsoft.codepush.common.datacontracts.CodePushUpdateResponse;
import com.microsoft.codepush.common.utils.FileUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(AppCenterLog.class)
public class CodePushLoggingTest {
    private String clientUniqueId = "YHFv65";
    private String deploymentKey = "ABC123";
    private String label = "awesome package";
    private boolean failedInstall = false;
    private boolean isPending = true;
    private boolean isDebugOnly = false;
    private boolean isFirstRun = false;

    @Test
    public void testLogging() throws Exception {
        CodePushPackage codePushPackage = new CodePushPackage();
        CodePushLocalPackage codePushLocalPackage = CodePushLocalPackage.createLocalPackage(failedInstall, isFirstRun, isPending, isDebugOnly, codePushPackage);
        CodePushUpdateRequest codePushUpdateRequest = CodePushUpdateRequest.createUpdateRequest(deploymentKey, codePushLocalPackage, clientUniqueId);
        CodePushDownloadStatusReport codePushDownloadStatusReport = CodePushDownloadStatusReport.createReport(clientUniqueId, deploymentKey, label);
        CodePushDeploymentStatusReport codePushDeploymentStatusReport = new CodePushDeploymentStatusReport();
        CodePushUpdateResponse codePushUpdateResponse = new CodePushUpdateResponse();

         /* Verify errors are logged. */
        mockStatic(AppCenterLog.class);
        codePushUpdateRequest.setDeploymentKey(null);
        codePushUpdateRequest.setAppVersion(null);
        codePushDownloadStatusReport.setLabel(null);
        codePushDownloadStatusReport.setClientUniqueId(null);
        codePushDownloadStatusReport.setDeploymentKey(null);
        codePushDeploymentStatusReport.setAppVersion(null);
        codePushDeploymentStatusReport.setPreviousDeploymentKey(null);
        codePushUpdateResponse.setUpdateInfo(null);
        File testFile = mock(File.class);
        doReturn(null).when(testFile).listFiles();
        doReturn(true).when(testFile).isDirectory();
        FileUtils.deleteFileOrFolderSilently(testFile);
        testFile = mock(File.class);
        doReturn(false).when(testFile).delete();
        FileUtils.deleteFileOrFolderSilently(testFile);
        File testFile1 = mock(File.class);
        doReturn(true).when(testFile1).isDirectory();
        doReturn(new File[]{testFile}).when(testFile1).listFiles();
        FileUtils.deleteFileOrFolderSilently(testFile1);
        verifyStatic(VerificationModeFactory.times(12));
        AppCenterLog.error(eq(CodePush.LOG_TAG), anyString());
    }
}