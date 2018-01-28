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

import static com.microsoft.codepush.common.TestUtils.CLIENT_UNIQUE_ID;
import static com.microsoft.codepush.common.TestUtils.DEPLOYMENT_KEY;
import static com.microsoft.codepush.common.TestUtils.FAILED_INSTALL;
import static com.microsoft.codepush.common.TestUtils.IS_DEBUG_ONLY;
import static com.microsoft.codepush.common.TestUtils.IS_FIRST_RUN;
import static com.microsoft.codepush.common.TestUtils.IS_PENDING;
import static com.microsoft.codepush.common.TestUtils.LABEL;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(AppCenterLog.class)
public class CodePushLoggingTest {

    @Test
    public void testLogging() throws Exception {
        CodePushPackage codePushPackage = new CodePushPackage();
        CodePushLocalPackage codePushLocalPackage = CodePushLocalPackage.createLocalPackage(FAILED_INSTALL, IS_FIRST_RUN, IS_PENDING, IS_DEBUG_ONLY, codePushPackage);
        CodePushUpdateRequest codePushUpdateRequest = CodePushUpdateRequest.createUpdateRequest(DEPLOYMENT_KEY, codePushLocalPackage, CLIENT_UNIQUE_ID);
        CodePushDownloadStatusReport codePushDownloadStatusReport = CodePushDownloadStatusReport.createReport(CLIENT_UNIQUE_ID, DEPLOYMENT_KEY, LABEL);
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
        File newTestFile = mock(File.class);
        doReturn(true).when(newTestFile).isDirectory();
        doReturn(new File[]{testFile}).when(newTestFile).listFiles();
        FileUtils.deleteFileOrFolderSilently(newTestFile);
        verifyStatic(VerificationModeFactory.times(12));
        AppCenterLog.error(eq(CodePush.LOG_TAG), anyString());
    }
}