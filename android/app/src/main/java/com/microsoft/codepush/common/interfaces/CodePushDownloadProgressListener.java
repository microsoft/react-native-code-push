package com.microsoft.codepush.common.interfaces;

/**
 * Interface for listener of progress of update event.
 */
public interface CodePushDownloadProgressListener {

    /**
     * Callback for handling download progress changed event.
     *
     * @param receivedBytes Total size of update in bytes.
     * @param totalBytes    Amount of bytes already downloaded from server.
     */
    void downloadProgressChanged(long receivedBytes, long totalBytes);
}
