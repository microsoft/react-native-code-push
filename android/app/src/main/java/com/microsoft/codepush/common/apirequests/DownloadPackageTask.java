package com.microsoft.codepush.common.apirequests;

import com.microsoft.codepush.common.CodePushConstants;
import com.microsoft.codepush.common.DownloadProgress;
import com.microsoft.codepush.common.exceptions.CodePushDownloadPackageException;
import com.microsoft.codepush.common.exceptions.CodePushFinalizeException;
import com.microsoft.codepush.common.interfaces.DownloadProgressCallback;
import com.microsoft.codepush.common.datacontracts.CodePushDownloadPackageResult;
import com.microsoft.codepush.common.utils.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Downloads an update.
 */
public class DownloadPackageTask extends BaseHttpTask<CodePushDownloadPackageResult> {

    /**
     * Header in the beginning of every zip file.
     */
    private final static Integer ZIP_HEADER = 0x504b0304;

    /**
     * Path to download file to.
     */
    private File mDownloadFile;

    /**
     * Callback for download process.
     */
    private DownloadProgressCallback mDownloadProgressCallback;

    /**
     * Creates instance of {@link DownloadPackageTask}.
     *
     * @param fileUtils                instance of {@link FileUtils} to work with.
     * @param requestUrl               url for downloading an update.
     * @param downloadFile             path to download file to.
     * @param downloadProgressCallback callback for download progress.
     */
    public DownloadPackageTask(FileUtils fileUtils, String requestUrl, File downloadFile, DownloadProgressCallback downloadProgressCallback) {
        mFileUtils = fileUtils;
        mRequestUrl = requestUrl;
        mDownloadFile = downloadFile;
        mDownloadProgressCallback = downloadProgressCallback;
    }

    @Override
    protected CodePushDownloadPackageResult doInBackground(Void... params) {
        HttpURLConnection connection;
        BufferedInputStream bufferedInputStream = null;
        FileOutputStream fileOutputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        try {
            connection = createConnection(mRequestUrl);
        } catch (IOException e) {

            /* We can't throw custom errors from this function, so any error will be passed to the result. */
            mExecutionException = new CodePushDownloadPackageException(mRequestUrl, e);
            return null;
        }
        try {
            long totalBytes = connection.getContentLength();
            long receivedBytes = 0;
            bufferedInputStream = new BufferedInputStream(connection.getInputStream());
            fileOutputStream = new FileOutputStream(mDownloadFile);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream, CodePushConstants.DOWNLOAD_BUFFER_SIZE);
            byte[] data = new byte[CodePushConstants.DOWNLOAD_BUFFER_SIZE];

            /* Header allows us to check whether this is a zip-stream. */
            byte[] header = new byte[4];
            int numBytesRead;
            while ((numBytesRead = bufferedInputStream.read(data, 0, CodePushConstants.DOWNLOAD_BUFFER_SIZE)) > 0) {
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
                if (mDownloadProgressCallback != null) {
                    mDownloadProgressCallback.call(new DownloadProgress(totalBytes, receivedBytes));
                }
            }
            if (totalBytes >= 0 && totalBytes != receivedBytes) {
                mExecutionException = new CodePushDownloadPackageException(receivedBytes, totalBytes);
                return null;
            }
            boolean isZip = ByteBuffer.wrap(header).getInt() == ZIP_HEADER;
            return new CodePushDownloadPackageResult(mDownloadFile, isZip);
        } catch (IOException e) {
            mExecutionException = new CodePushDownloadPackageException(e);
            return null;
        } finally {
            Exception e = mFileUtils.finalizeResources(
                    Arrays.asList(bufferedOutputStream, fileOutputStream, bufferedInputStream),
                    null);
            if (e != null) {
                mFinalizeException = new CodePushFinalizeException(e);
            }
        }
    }
}
