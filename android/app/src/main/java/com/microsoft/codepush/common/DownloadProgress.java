package com.microsoft.codepush.common;

/**
 * Object representing progress of update download.
 */
public class DownloadProgress {

    /**
     * Total size of update in bytes.
     */
    private long mTotalBytes;

    /**
     * Amount of bytes already downloaded from server.
     */
    private long mReceivedBytes;

    /**
     * DownloadProgress constructor.
     *
     * @param totalBytes    Total size of update in bytes.
     * @param receivedBytes Amount of bytes already downloaded from server.
     */
    public DownloadProgress(long totalBytes, long receivedBytes) {
        mTotalBytes = totalBytes;
        mReceivedBytes = receivedBytes;
    }

    /**
     * Returns total size of update in bytes.
     *
     * @return Total size of update in bytes.
     */
    public long getTotalBytes() {
        return mTotalBytes;
    }

    /**
     * Returns amount of bytes already downloaded from server.
     *
     * @return Amount of bytes already downloaded from server.
     */
    public long getReceivedBytes() {
        return mReceivedBytes;
    }

    /**
     * Indicates whether downloading of update is completed.
     *
     * @return True if downloading of update is completed, false otherwise.
     */
    public boolean isCompleted() {
        return mTotalBytes == mReceivedBytes;
    }
}
