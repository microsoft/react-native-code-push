package com.microsoft.codepush.common;

import android.os.Environment;

import com.microsoft.codepush.common.connection.PackageDownloader;
import com.microsoft.codepush.common.exceptions.CodePushDownloadPackageException;

import org.junit.Test;
import org.mockito.Mockito;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.microsoft.codepush.common.utils.PackageDownloaderTestUtils.checkDoInBackgroundFails;
import static com.microsoft.codepush.common.utils.PackageDownloaderTestUtils.checkDoInBackgroundNotFails;
import static com.microsoft.codepush.common.utils.PackageDownloaderTestUtils.createPackageDownloader;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * This class tests all the {@link PackageDownloader} scenarios.
 */
public class PackageDownloaderTests {

    private final static String FULL_PACKAGE_URL = "https://codepush.blob.core.windows.net/storagev2/6CjTRZUgaYrHlhH3mKy2JsQVIJtsa0021bd2-9be1-4904-b4c6-16ce9c797779";

    /**
     * Downloading files should return a {@link CodePushDownloadPackageException}
     * if the amount of <code>receivedBytes</code> does not match the amount of <code>totalBytes</code>.
     * The amount of <code>totalBytes</code> is more than <code>receivedBytes</code>.
     */
    @Test
    public void downloadFailsIfBytesMismatchMore() throws Exception {
        PackageDownloader packageDownloader = createPackageDownloader(FULL_PACKAGE_URL);
        HttpURLConnection connectionMock = Mockito.mock(HttpURLConnection.class);
        doReturn(100).when(connectionMock).getContentLength();
        BufferedInputStream bufferedInputStream = mock(BufferedInputStream.class);
        doReturn(-1).when(bufferedInputStream).read(any(byte[].class), anyInt(), anyInt());
        doReturn(bufferedInputStream).when(connectionMock).getInputStream();
        doReturn(connectionMock).when(packageDownloader).createConnection(anyString());
        checkDoInBackgroundFails(packageDownloader);
    }

    /**
     * Downloading files should not return a {@link CodePushDownloadPackageException}
     * if the amount of <code>receivedBytes</code> does not match the amount of <code>totalBytes</code>, but <code>totalBytes</code> is less that zero.
     */
    @Test
    public void downloadNotFailsIfBytesMismatchLess() throws Exception {
        PackageDownloader packageDownloader = createPackageDownloader(FULL_PACKAGE_URL);
        HttpURLConnection realConnection = (HttpURLConnection) (new URL(FULL_PACKAGE_URL)).openConnection();
        BufferedInputStream realStream = new BufferedInputStream(realConnection.getInputStream());
        HttpURLConnection connectionMock = Mockito.mock(HttpURLConnection.class);
        doReturn(-1).when(connectionMock).getContentLength();
        doReturn(realStream).when(connectionMock).getInputStream();
        doReturn(connectionMock).when(packageDownloader).createConnection(anyString());
        checkDoInBackgroundNotFails(packageDownloader);
    }

    /**
     * Downloading files should return a {@link CodePushDownloadPackageException}
     * if a {@link java.io.InputStream#read()} throws an {@link IOException} when closing.
     */
    @Test
    public void downloadFailsIfCloseFails() throws Exception {
        PackageDownloader packageDownloader = createPackageDownloader(FULL_PACKAGE_URL);
        HttpURLConnection connectionMock = Mockito.mock(HttpURLConnection.class);
        BufferedInputStream bufferedInputStream = mock(BufferedInputStream.class);
        doReturn(-1).when(bufferedInputStream).read(any(byte[].class), anyInt(), anyInt());
        doThrow(new IOException()).when(bufferedInputStream).close();
        doReturn(bufferedInputStream).when(connectionMock).getInputStream();
        doReturn(connectionMock).when(packageDownloader).createConnection(anyString());
        checkDoInBackgroundFails(packageDownloader);
    }

    /**
     * Downloading files should return a {@link CodePushDownloadPackageException}
     * if a {@link java.io.InputStream#read()} throws an {@link IOException}.
     */
    @Test
    public void downloadFailsIfReadFails() throws Exception {
        File codePushPath = new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX);
        File downloadFolder = new File(codePushPath.getPath());
        downloadFolder.mkdirs();
        PackageDownloader packageDownloader = createPackageDownloader(FULL_PACKAGE_URL, downloadFolder);
        checkDoInBackgroundFails(packageDownloader);
    }
}
