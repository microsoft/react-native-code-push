package com.microsoft.codepush.common.utils;

import com.microsoft.codepush.common.CodePushConstants;
import com.microsoft.codepush.common.DownloadProgress;
import com.microsoft.codepush.common.connection.PackageDownloader;
import com.microsoft.codepush.common.interfaces.DownloadProgressCallback;
import com.microsoft.codepush.common.managers.CodePushUpdateManager;

import org.json.JSONObject;
import org.mockito.Mockito;

import java.io.File;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;

/**
 * Utils to make {@link CodePushUpdateManager} testing process easier and avoid code repetition.
 */
public class UpdateManagerTestUtils {

    /**
     * Executes "download" workflow.
     *
     * @param codePushUpdateManager instance of code push update manager.
     * @param packageObject         current package object.
     * @param verify                whether verify that callback is called.
     * @param url                   url for downloading.
     * @return result of the download.
     */
    public static CodePushDownloadPackageResult executeDownload(CodePushUpdateManager codePushUpdateManager, JSONObject packageObject, boolean verify, String url) throws Exception {
        PackageDownloader packageDownloader = new PackageDownloader();
        DownloadProgressCallback downloadProgressCallback = mock(DownloadProgressCallback.class);
        packageObject.put("downloadUrl", url);
        CodePushDownloadPackageResult codePushDownloadPackageResult = codePushUpdateManager.downloadPackage(packageObject, downloadProgressCallback, packageDownloader);
        if (verify) {
            Mockito.verify(downloadProgressCallback, timeout(5000).atLeast(1)).call(any(DownloadProgress.class));
        }
        return codePushDownloadPackageResult;
    }

    /**
     * Performs very common workflow: download -> unzip.
     *
     * @param codePushUpdateManager instance of update manager.
     * @param packageObject         current package object.
     * @param url                   url for downloading.
     */
    public static void executeWorkflow(CodePushUpdateManager codePushUpdateManager, JSONObject packageObject, String url) throws Exception {
        CodePushDownloadPackageResult downloadPackageResult = executeDownload(codePushUpdateManager, packageObject, true, url);
        File downloadFile = downloadPackageResult.getDownloadFile();
        codePushUpdateManager.unzipPackage(downloadFile);
    }

    /**
     * Performs full testing workflow: download -> unzip -> install -> write metadata.
     *
     * @param packageObject         current package object.
     * @param codePushUpdateManager instance of update manager.
     * @param packageHash           package hash to use.
     * @param packageUrl            package url to use.
     */
    public static void executeFullWorkflow(JSONObject packageObject, CodePushUpdateManager codePushUpdateManager, String packageHash, String packageUrl) throws Exception {
        packageObject.put("packageHash", packageHash);
        executeWorkflow(codePushUpdateManager, packageObject, packageUrl);
        String appEntryPoint = codePushUpdateManager.mergeDiff(packageHash, null, "index.html");
        assertEquals("/www/index.html", appEntryPoint);
        codePushUpdateManager.installPackage(packageObject, false);
        String newUpdateFolderPath = codePushUpdateManager.getPackageFolderPath(packageHash);
        String newUpdateMetadataPath = FileUtils.appendPathComponent(newUpdateFolderPath, CodePushConstants.PACKAGE_FILE_NAME);
        CodePushUtils.writeJsonToFile(packageObject, newUpdateMetadataPath);
    }
}
