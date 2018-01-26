package com.microsoft.codepush.common.connection;

import android.os.AsyncTask;

import com.microsoft.codepush.common.CodePushConstants;
import com.microsoft.codepush.common.DownloadProgress;
import com.microsoft.codepush.common.exceptions.CodePushDownloadPackageException;
import com.microsoft.codepush.common.exceptions.CodePushFinalizeException;
import com.microsoft.codepush.common.interfaces.DownloadProgressCallback;
import com.microsoft.codepush.common.utils.CodePushDownloadPackageResult;
import com.microsoft.codepush.common.utils.CodePushUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Downloads an update.
 */
public class PackageDownloader extends AsyncTask<Void, Void, CodePushDownloadPackageResult> {

    /**
     * Url for downloading an update.
     */
    private String downloadUrlString;

    /**
     * Path to download file to.
     */
    private File downloadFile;

    /**
     * Callback for download process.
     */
    private DownloadProgressCallback downloadProgressCallback;

    /**
     * Sets the downloader required parameters.
     *
     * @param downloadUrlString        url for downloading an update.
     * @param downloadFile             path to download file to.
     * @param downloadProgressCallback callback for download progress.
     */
    public void setParameters(String downloadUrlString, File downloadFile, DownloadProgressCallback downloadProgressCallback) {
        this.downloadUrlString = downloadUrlString;
        this.downloadFile = downloadFile;
        this.downloadProgressCallback = downloadProgressCallback;
    }

    /**
     * Opens url connection.
     *
     * @param url url to open.
     * @return instance of url connection.
     * @throws IOException exception thrown during opening.
     */
    public HttpURLConnection createConnection(URL url) throws IOException {
        HttpURLConnection connection;
        connection = (HttpURLConnection) url.openConnection();
        return connection;
    }

    @Override
    protected CodePushDownloadPackageResult doInBackground(Void... params) {
        HttpURLConnection connection = null;

        BufferedInputStream bufferedInputStream = null;
        FileOutputStream fileOutputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        URL downloadUrl = null;
        try {
            downloadUrl = new URL(downloadUrlString);
        } catch (MalformedURLException e) {
            return new CodePushDownloadPackageResult(new CodePushDownloadPackageException(downloadUrlString, e));
        }
        try {
            connection = createConnection(downloadUrl);
            long totalBytes = connection.getContentLength();
            long receivedBytes = 0;
            bufferedInputStream = new BufferedInputStream(connection.getInputStream());
            fileOutputStream = new FileOutputStream(downloadFile);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream, CodePushConstants.DOWNLOAD_BUFFER_SIZE);
            byte[] data = new byte[CodePushConstants.DOWNLOAD_BUFFER_SIZE];
            byte[] header = new byte[4];
            int numBytesRead;
            while ((numBytesRead = bufferedInputStream.read(data, 0, CodePushConstants.DOWNLOAD_BUFFER_SIZE)) >= 0) {
                if (receivedBytes < 4) {
                    for (int i = 0; i < numBytesRead; i++) {
                        int headerOffset = (int) (receivedBytes) + i;
                        if (headerOffset >= 4) {
                            break;
                        }
                        header[headerOffset] = data[i];
                    }
                }
                receivedBytes += numBytesRead;
                bufferedOutputStream.write(data, 0, numBytesRead);
                if (downloadProgressCallback != null) {
                    downloadProgressCallback.call(new DownloadProgress(totalBytes, receivedBytes));
                }
            }
            if (totalBytes >= 0 && totalBytes != receivedBytes) {
                return new CodePushDownloadPackageResult(new CodePushDownloadPackageException(receivedBytes, totalBytes));
            }
            boolean isZip = ByteBuffer.wrap(header).getInt() == 0x504b0304;
            return new CodePushDownloadPackageResult(downloadFile, isZip);
        } catch (IOException e) {
            return new CodePushDownloadPackageResult(new CodePushDownloadPackageException(e));
        } finally {
            Exception e = CodePushUtils.finalizeResources(
                    Arrays.asList(bufferedOutputStream, fileOutputStream, bufferedInputStream),
                    null);
            if (e != null) {
                return new CodePushDownloadPackageResult(new CodePushDownloadPackageException(new CodePushFinalizeException(e)));
            }
        }
    }
}
