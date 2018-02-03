package com.microsoft.codepush.common;

import android.os.Environment;

import com.microsoft.codepush.common.connection.PackageDownloader;
import com.microsoft.codepush.common.exceptions.CodePushDownloadPackageException;
import com.microsoft.codepush.common.exceptions.CodePushFinalizeException;
import com.microsoft.codepush.common.utils.CodePushDownloadPackageResult;
import com.microsoft.codepush.common.utils.CodePushUtils;

import org.junit.Test;
import org.mockito.Mockito;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import static com.microsoft.codepush.common.utils.PackageDownloaderAndroidTestUtils.checkDoInBackgroundFails;
import static com.microsoft.codepush.common.utils.PackageDownloaderAndroidTestUtils.checkDoInBackgroundNotFails;
import static com.microsoft.codepush.common.utils.PackageDownloaderAndroidTestUtils.createPackageDownloader;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class tests all the {@link PackageDownloader} scenarios.
 */
public class PackageDownloaderAndroidTests {

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
     * If download fails with an {@link IOException}, it should go straight to finally block,
     * where it should throw a {@link CodePushFinalizeException}
     * if an {@link IOException} is thrown during {@link CodePushUtils#finalizeResources(List, String)}.
     * In this case, downloading fails on {@link HttpURLConnection#getInputStream()}.
     */
    @Test
    public void downloadDoubleFailureInputStream() throws Exception {
        PackageDownloader packageDownloader = createPackageDownloader(FULL_PACKAGE_URL);
        HttpURLConnection connectionMock = Mockito.mock(HttpURLConnection.class);
        BufferedInputStream bufferedInputStream = mock(BufferedInputStream.class);
        doReturn(-1).when(bufferedInputStream).read(any(byte[].class), anyInt(), anyInt());
        doThrow(new IOException()).when(bufferedInputStream).close();
        doThrow(new IOException()).when(connectionMock).getInputStream();
        doReturn(connectionMock).when(packageDownloader).createConnection(anyString());
        checkDoInBackgroundFails(packageDownloader);
    }

    /**
     * Tests the case when <code>numBytesRead</code> = 1 and we don't enter the cycle `for (int i = 1; i < numBytesRead; i++)`.
     */
    @Test
    public void downloadSkipCycleTest() throws Exception {
        PackageDownloader packageDownloader = createPackageDownloader(FULL_PACKAGE_URL);
        HttpURLConnection connectionMock = Mockito.mock(HttpURLConnection.class);
        BufferedInputStream bufferedInputStream = mock(BufferedInputStream.class);
        when(bufferedInputStream.read(any(byte[].class), anyInt(), anyInt())).thenReturn(1).thenReturn(-1);
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

    /**
     * If download fails with an {@link IOException}, it should go straight to finally block,
     * where it should throw a {@link CodePushFinalizeException}
     * if an {@link IOException} is thrown during {@link CodePushUtils#finalizeResources(List, String)}.
     * In this case, downloading fails when creating {@link java.io.FileOutputStream} for folder (should be a file, not directory).
     */
    @Test
    public void downloadDoubleFailure() throws Exception {
        File codePushPath = new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX);
        File downloadFolder = new File(codePushPath.getPath());
        downloadFolder.mkdirs();
        PackageDownloader packageDownloader = createPackageDownloader(FULL_PACKAGE_URL, downloadFolder);
        HttpURLConnection connectionMock = Mockito.mock(HttpURLConnection.class);
        BufferedInputStream bufferedInputStream = mock(BufferedInputStream.class);
        doReturn(0).when(bufferedInputStream).read(any(byte[].class), anyInt(), anyInt());
        doThrow(new IOException()).when(bufferedInputStream).close();
        doReturn(bufferedInputStream).when(connectionMock).getInputStream();
        doReturn(connectionMock).when(packageDownloader).createConnection(anyString());
        checkDoInBackgroundFails(packageDownloader);
    }

    /**
     * If download returns {@link CodePushDownloadPackageResult} with an {@link CodePushDownloadPackageException},
     * it should go straight to finally block, where it should throw a {@link CodePushFinalizeException}
     * if an {@link IOException} is thrown during {@link CodePushUtils#finalizeResources(List, String)}.
     * In this case, downloading fails due to bytes mismatch.
     */
    @Test
    public void downloadDoubleFailureMismatch() throws Exception {
        PackageDownloader packageDownloader = createPackageDownloader(FULL_PACKAGE_URL);
        HttpURLConnection connectionMock = Mockito.mock(HttpURLConnection.class);
        BufferedInputStream bufferedInputStream = mock(BufferedInputStream.class);
        doReturn(-1).when(bufferedInputStream).read(any(byte[].class), anyInt(), anyInt());
        doThrow(new IOException()).when(bufferedInputStream).close();
        doReturn(bufferedInputStream).when(connectionMock).getInputStream();
        doReturn(100).when(connectionMock).getContentLength();
        doReturn(connectionMock).when(packageDownloader).createConnection(anyString());
        checkDoInBackgroundFails(packageDownloader);
    }
}
