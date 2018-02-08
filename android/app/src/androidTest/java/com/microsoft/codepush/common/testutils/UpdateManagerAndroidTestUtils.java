package com.microsoft.codepush.common.testutils;

import android.os.Environment;

import com.microsoft.codepush.common.CodePushConstants;
import com.microsoft.codepush.common.DownloadProgress;
import com.microsoft.codepush.common.apirequests.DownloadPackageTask;
import com.microsoft.codepush.common.interfaces.DownloadProgressCallback;
import com.microsoft.codepush.common.managers.CodePushUpdateManager;
import com.microsoft.codepush.common.datacontracts.CodePushDownloadPackageResult;
import com.microsoft.codepush.common.utils.FileUtils;

import java.io.File;

import static com.microsoft.codepush.common.CodePushConstants.CODE_PUSH_FOLDER_PREFIX;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Utils to make {@link CodePushUpdateManager} testing process easier and avoid code repetition.
 */
public class UpdateManagerAndroidTestUtils {

    /**
     * Executes "download" workflow.
     *
     * @param codePushUpdateManager instance of code push update manager.
     * @param packageHash         package hash to use.
     * @param verify                whether verify that callback is called.
     * @param url                   url for downloading.
     * @return result of the download.
     */
    public static CodePushDownloadPackageResult executeDownload(CodePushUpdateManager codePushUpdateManager, String packageHash, boolean verify, String url) throws Exception {
        DownloadPackageTask downloadPackageJob = new DownloadPackageTask(FileUtils.getInstance());
        DownloadProgressCallback downloadProgressCallback = mock(DownloadProgressCallback.class);
        File downloadFolder = new File(Environment.getExternalStorageDirectory(), CODE_PUSH_FOLDER_PREFIX);
        downloadFolder.mkdirs();
        File downloadFilePath = new File(downloadFolder, CodePushConstants.DOWNLOAD_FILE_NAME);
        downloadPackageJob.setParameters(url, downloadFilePath, downloadProgressCallback);
        CodePushDownloadPackageResult codePushDownloadPackageResult = codePushUpdateManager.downloadPackage(packageHash, downloadPackageJob);
        if (verify) {
            verify(downloadProgressCallback, timeout(5000).atLeast(1)).call(any(DownloadProgress.class));
        }
        return codePushDownloadPackageResult;
    }

    /**
     * Performs very common workflow: download -> unzip.
     *
     * @param codePushUpdateManager instance of update manager.
     * @param packageHash         package hash to use.
     * @param url                   url for downloading.
     */
    public static void executeWorkflow(CodePushUpdateManager codePushUpdateManager, String packageHash, String url) throws Exception {
        CodePushDownloadPackageResult downloadPackageResult = executeDownload(codePushUpdateManager, packageHash, true, url);
        File downloadFile = downloadPackageResult.getDownloadFile();
        codePushUpdateManager.unzipPackage(downloadFile);
    }

    /**
     * Performs full testing workflow: download -> unzip -> install -> write metadata.
     *
     * @param packageHash         package hash to use.
     * @param codePushUpdateManager instance of update manager.
     * @param packageUrl            package url to use.
     */
    public static void executeFullWorkflow(CodePushUpdateManager codePushUpdateManager, String packageHash, String packageUrl) throws Exception {
        executeWorkflow(codePushUpdateManager, packageHash, packageUrl);
        String appEntryPoint = codePushUpdateManager.mergeDiff(packageHash, null, "index.html");
        assertEquals("/www/index.html", appEntryPoint);
        codePushUpdateManager.installPackage(packageHash, false);
    }
}
