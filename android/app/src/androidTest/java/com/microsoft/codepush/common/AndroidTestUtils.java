package com.microsoft.codepush.common;

import android.os.Environment;

import com.microsoft.codepush.common.connection.PackageDownloader;
import com.microsoft.codepush.common.interfaces.DownloadProgressCallback;
import com.microsoft.codepush.common.managers.CodePushUpdateManager;
import com.microsoft.codepush.common.utils.CodePushDownloadPackageResult;

import org.json.JSONObject;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.zip.ZipEntry;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;

/**
 * Utils to make testing process easier and avoid code repetition.
 */
class AndroidTestUtils {

    final static String FULL_PACKAGE_URL = "https://codepush.blob.core.windows.net/storagev2/6CjTRZUgaYrHlhH3mKy2JsQVIJtsa0021bd2-9be1-4904-b4c6-16ce9c797779";
    final static String DIFF_PACKAGE_URL = "https://codepush.blob.core.windows.net/storagev2/8wuI2wwTlf4RioIb1cLRtyQyzRW80840428d-683e-4d30-a120-c592a355a594";
    final static String SIGNED_PACKAGE_URL = "https://codepush.blob.core.windows.net/storagev2/OWIRaqwJQUbNeiX60nDnijj9HxMza0021bd2-9be1-4904-b4c6-16ce9c797779";
    final static String FULL_PACKAGE_HASH = "a1d28a073a1fa45745a8b1952ccc5c2bd4753e533e7b9e48459a6c186ecd32af";
    final static String DIFF_PACKAGE_HASH = "ff46674f196ae852ccb67e49346a11cb9d8c0243ba24003e11b83dd7469b5dd4";
    final static String SIGNED_PACKAGE_HASH = "ce9148e0d0422dc7ffefba3a82f527a0e75f51c449f34a5f7dabab6f36251aaf";
    final static String SIGNED_PACKAGE_PUBLIC_KEY = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAM4bfGAHAEx+IVl5/qaRHisPvpGfCY47O7EkW8XhZVer+bo1k6VT3s8hPBMQfcFw/ZQotWwLkvStelvrQptJFiUCAwEAAQ";

    /**
     * Mocks a file to fail when performing <code>mkdirs()</code>.
     *
     * @return mocked file returning <code>false</code> when <code>mkdirs()</code> is called.
     */
    static File mockDirMkDirsFail() {
        File mocked = getFileMock();
        doReturn(false).when(mocked).mkdirs();
        return mocked;
    }

    /**
     * Mocks a file to fail when performing <code>renameTo()</code>.
     *
     * @return mocked file returning <code>false</code> when <code>renameTo()</code> is called.
     */
    static File mockFileRenameToFail() {
        File mocked = getFileMock();
        doReturn(false).when(mocked).renameTo(any(File.class));
        return mocked;
    }

    /**
     * Creates a real (not mocked) folder for testing.
     *
     * @return real test folder.
     */
    static File getRealTestFolder() {
        File testFolder = new File(Environment.getExternalStorageDirectory(), "Test35941");
        testFolder.mkdirs();
        return testFolder;
    }

    /**
     * Creates a real (not mocked) file for testing.
     *
     * @return real test file.
     * @throws IOException exception occurred when creating a file.
     */
    static File getRealFile() throws IOException {
        File testFolder = getRealTestFolder();
        File realFile = new File(testFolder, "file.txt");
        realFile.createNewFile();
        return realFile;
    }

    /**
     * Mocks a file to fail when performing <code>setLastModified()</code>.
     *
     * @return mocked file returning <code>false</code> when <code>setLastModified()</code> is called.
     */
    static File mockSetLastModifiedFail() throws IOException {
        File mocked = getRealFile();
        mocked = spy(mocked);
        doReturn(false).when(mocked).setLastModified(anyLong());
        return mocked;
    }

    /**
     * Mocks a {@link ZipEntry} to simulate real entry behaviour.
     *
     * @param isDirectory whether this should represent a directory.
     * @return mocked zip entry.
     */
    static ZipEntry mockZipEntry(boolean isDirectory) {
        ZipEntry mocked = mock(ZipEntry.class);
        doReturn(new Date().getTime()).when(mocked).getTime();
        doReturn(isDirectory).when(mocked).isDirectory();
        return mocked;
    }

    /**
     * Mocks a file to fail when performing <code>listFiles()</code>.
     *
     * @return mocked file returning <code>false</code> when <code>listFiles()</code> is called.
     */
    static File mockDirListFilesFail() {
        File mocked = getFileMock();
        doReturn(null).when(mocked).listFiles();
        return mocked;
    }

    /**
     * Gets a mock of the {@link File} class.
     * Note: Its method <code>exists()</code> by default returns <code>false</code>.
     *
     * @return mocked file.
     */
    static File getFileMock() {
        return getFileMock(false);
    }

    /**
     * Gets a mock of the {@link File} class.
     *
     * @param exists whether the file <code>exists()</code> call should return <code>true</code> or <code>false</code>.
     * @return mocked file.
     */
    static File getFileMock(boolean exists) {
        File mocked = mock(File.class);
        doReturn(exists).when(mocked).exists();
        return mocked;
    }

    /**
     * Executes <code>doInBackground()</code> method of {@link PackageDownloader} only and assert that it fails..
     *
     * @param packageDownloader instance of package downloader.
     * @throws Exception any exception that might occur.
     */
    static void checkDoInBackgroundFails(PackageDownloader packageDownloader) throws Exception {
        Method method = packageDownloader.getClass().getMethod("doInBackground", Void[].class);
        CodePushDownloadPackageResult codePushDownloadPackageResult = (CodePushDownloadPackageResult) method.invoke(packageDownloader, new Void[]{null});
        assertTrue(codePushDownloadPackageResult.isCorrupt());
    }

    /**
     * Creates spied package downloader instance.
     *
     * @param url custom url.
     * @return package downloader instance that can be mocked.
     */
    static PackageDownloader createPackageDownloader(String url) {
        File codePushPath = new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX);
        File downloadFolder = new File(codePushPath.getPath());
        downloadFolder.mkdirs();
        File downloadFilePath = new File(downloadFolder, CodePushConstants.DOWNLOAD_FILE_NAME);
        PackageDownloader packageDownloader = new PackageDownloader();
        DownloadProgressCallback downloadProgressCallback = mock(DownloadProgressCallback.class);
        packageDownloader.setParameters(url, downloadFilePath, downloadProgressCallback);
        return spy(packageDownloader);
    }

    /**
     * Creates spied package downloader instance.
     *
     * @param downloadFilePath custom download path.
     * @return package downloader instance that can be mocked.
     */
    static PackageDownloader createPackageDownloader(File downloadFilePath) {
        PackageDownloader packageDownloader = new PackageDownloader();
        DownloadProgressCallback downloadProgressCallback = mock(DownloadProgressCallback.class);
        packageDownloader.setParameters(FULL_PACKAGE_URL, downloadFilePath, downloadProgressCallback);
        return spy(packageDownloader);
    }

    /**
     * Creates default spied package downloader instance.
     *
     * @return package downloader instance that can be mocked.
     */
    static PackageDownloader createPackageDownloader() {
        File codePushPath = new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX);
        File downloadFolder = new File(codePushPath.getPath());
        downloadFolder.mkdirs();
        File downloadFilePath = new File(downloadFolder, CodePushConstants.DOWNLOAD_FILE_NAME);
        return createPackageDownloader(downloadFilePath);
    }

    /**
     * Executes "download" workflow.
     *
     * @param codePushUpdateManager instance of code push update manager.
     * @param packageObject         current package object.
     * @param verify                whether verify that callback is called.
     * @param url                   url for downloading.
     * @return result of the download.
     * @throws Exception any exception that might occur.
     */
    static CodePushDownloadPackageResult executeDownload(CodePushUpdateManager codePushUpdateManager, JSONObject packageObject, boolean verify, String url) throws Exception {
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
     * @throws Exception any exception that might occur.
     */
    static void executeWorkflow(CodePushUpdateManager codePushUpdateManager, JSONObject packageObject, String url) throws Exception {
        CodePushDownloadPackageResult downloadPackageResult = executeDownload(codePushUpdateManager, packageObject, true, url);
        File downloadFile = downloadPackageResult.getDownloadFile();
        codePushUpdateManager.unzipPackage(downloadFile);
    }
}
