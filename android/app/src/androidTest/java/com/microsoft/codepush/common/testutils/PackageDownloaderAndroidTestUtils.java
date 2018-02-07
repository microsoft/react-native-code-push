package com.microsoft.codepush.common.testutils;

import android.os.Environment;

import com.microsoft.codepush.common.CodePushConstants;
import com.microsoft.codepush.common.connection.DownloadPackageJob;
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
 * Utils to make {@link DownloadPackageJob} testing process easier and avoid code repetition.
 */
public class PackageDownloaderAndroidTestUtils {

    /**
     * Executes <code>doInBackground()</code> method of {@link DownloadPackageJob} only and assert that it fails.
     *
     * @param downloadPackageJob instance of package downloader.
     */
    public static void checkDoInBackgroundFails(DownloadPackageJob downloadPackageJob) throws Exception {
        assertTrue(executeDoInBackground(downloadPackageJob).isFailed());
    }

    /**
     * Executes <code>doInBackground()</code> method of {@link DownloadPackageJob}.
     *
     * @param downloadPackageJob instance of package downloader.
     * @return download result.
     */
    private static CodePushDownloadPackageResult executeDoInBackground(DownloadPackageJob downloadPackageJob) throws Exception {
        Method method = downloadPackageJob.getClass().getMethod("doInBackground", Void[].class);
        return (CodePushDownloadPackageResult) method.invoke(downloadPackageJob, (Object[]) new Void[]{null});
    }

    /**
     * Executes <code>doInBackground()</code> method of {@link DownloadPackageJob} only and assert that it does not fail.
     *
     * @param downloadPackageJob instance of package downloader.
     */
    public static void checkDoInBackgroundNotFails(DownloadPackageJob downloadPackageJob) throws Exception {
        assertFalse(executeDoInBackground(downloadPackageJob).isFailed());
    }

    /**
     * Creates spied package downloader instance.
     *
     * @param url custom url.
     * @return package downloader instance that can be mocked.
     */
    public static DownloadPackageJob createPackageDownloader(String url) {
        File codePushPath = new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX);
        File downloadFolder = new File(codePushPath.getPath());
        downloadFolder.mkdirs();
        File downloadFilePath = new File(downloadFolder, CodePushConstants.DOWNLOAD_FILE_NAME);
        DownloadPackageJob downloadPackageJob = new DownloadPackageJob(FileUtils.getInstance());
        DownloadProgressCallback downloadProgressCallback = mock(DownloadProgressCallback.class);
        downloadPackageJob.setParameters(url, downloadFilePath, downloadProgressCallback);
        return spy(downloadPackageJob);
    }

    /**
     * Creates spied package downloader instance.
     *
     * @param downloadFilePath custom download path.
     * @param url              custom url.
     * @return package downloader instance that can be mocked.
     */
    public static DownloadPackageJob createPackageDownloader(String url, File downloadFilePath) {
        DownloadPackageJob downloadPackageJob = new DownloadPackageJob(FileUtils.getInstance());
        DownloadProgressCallback downloadProgressCallback = mock(DownloadProgressCallback.class);
        downloadPackageJob.setParameters(url, downloadFilePath, downloadProgressCallback);
        return spy(downloadPackageJob);
    }
}
