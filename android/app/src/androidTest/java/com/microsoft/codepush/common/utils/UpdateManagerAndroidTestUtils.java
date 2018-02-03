package com.microsoft.codepush.common.utils;

import android.os.Environment;

import com.microsoft.codepush.common.CodePushConstants;
import com.microsoft.codepush.common.DownloadProgress;
import com.microsoft.codepush.common.connection.PackageDownloader;
import com.microsoft.codepush.common.interfaces.DownloadProgressCallback;
import com.microsoft.codepush.common.managers.CodePushUpdateManager;

import org.mockito.Matchers;
import org.mockito.Mockito;

import java.io.File;

import static com.microsoft.codepush.common.CodePushConstants.CODE_PUSH_FOLDER_PREFIX;
import static junit.framework.Assert.assertEquals;

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
        PackageDownloader packageDownloader = new PackageDownloader();
        DownloadProgressCallback downloadProgressCallback = Mockito.mock(DownloadProgressCallback.class);
        File downloadFolder = new File(Environment.getExternalStorageDirectory(), CODE_PUSH_FOLDER_PREFIX);
        downloadFolder.mkdirs();
        File downloadFilePath = new File(downloadFolder, CodePushConstants.DOWNLOAD_FILE_NAME);
        packageDownloader.setParameters(url, downloadFilePath, downloadProgressCallback);
        CodePushDownloadPackageResult codePushDownloadPackageResult = codePushUpdateManager.downloadPackage(packageHash, packageDownloader);
        if (verify) {
            Mockito.verify(downloadProgressCallback, Mockito.timeout(5000).atLeast(1)).call(Matchers.any(DownloadProgress.class));
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
