package com.microsoft.codepush.common.testutils;

import android.os.Environment;

import com.microsoft.codepush.common.CodePushConstants;
import com.microsoft.codepush.common.connection.PackageDownloader;
import com.microsoft.codepush.common.interfaces.DownloadProgressCallback;
import com.microsoft.codepush.common.utils.CodePushDownloadPackageResult;
import com.microsoft.codepush.common.utils.FileUtils;

import java.io.File;
import java.lang.reflect.Method;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Utils to make {@link PackageDownloader} testing process easier and avoid code repetition.
 */
public class PackageDownloaderAndroidTestUtils {

    /**
     * Executes <code>doInBackground()</code> method of {@link PackageDownloader} only and assert that it fails.
     *
     * @param packageDownloader instance of package downloader.
     */
    public static void checkDoInBackgroundFails(PackageDownloader packageDownloader) throws Exception {
        assertTrue(executeDoInBackground(packageDownloader).isCorrupt());
    }

    /**
     * Executes <code>doInBackground()</code> method of {@link PackageDownloader}.
     *
     * @param packageDownloader instance of package downloader.
     * @return download result.
     */
    private static CodePushDownloadPackageResult executeDoInBackground(PackageDownloader packageDownloader) throws Exception {
        Method method = packageDownloader.getClass().getMethod("doInBackground", Void[].class);
        return (CodePushDownloadPackageResult) method.invoke(packageDownloader, (Object[]) new Void[]{null});
    }

    /**
     * Executes <code>doInBackground()</code> method of {@link PackageDownloader} only and assert that it does not fail.
     *
     * @param packageDownloader instance of package downloader.
     */
    public static void checkDoInBackgroundNotFails(PackageDownloader packageDownloader) throws Exception {
        assertFalse(executeDoInBackground(packageDownloader).isCorrupt());
    }

    /**
     * Creates spied package downloader instance.
     *
     * @param url custom url.
     * @return package downloader instance that can be mocked.
     */
    public static PackageDownloader createPackageDownloader(String url) {
        File codePushPath = new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX);
        File downloadFolder = new File(codePushPath.getPath());
        downloadFolder.mkdirs();
        File downloadFilePath = new File(downloadFolder, CodePushConstants.DOWNLOAD_FILE_NAME);
        PackageDownloader packageDownloader = new PackageDownloader(FileUtils.getInstance());
        DownloadProgressCallback downloadProgressCallback = mock(DownloadProgressCallback.class);
        packageDownloader.setParameters(url, downloadFilePath, downloadProgressCallback);
        return spy(packageDownloader);
    }

    /**
     * Creates spied package downloader instance.
     *
     * @param downloadFilePath custom download path.
     * @param url              custom url.
     * @return package downloader instance that can be mocked.
     */
    public static PackageDownloader createPackageDownloader(String url, File downloadFilePath) {
        PackageDownloader packageDownloader = new PackageDownloader(FileUtils.getInstance());
        DownloadProgressCallback downloadProgressCallback = mock(DownloadProgressCallback.class);
        packageDownloader.setParameters(url, downloadFilePath, downloadProgressCallback);
        return spy(packageDownloader);
    }
}
