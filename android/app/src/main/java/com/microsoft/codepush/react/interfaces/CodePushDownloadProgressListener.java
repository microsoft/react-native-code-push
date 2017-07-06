package com.microsoft.codepush.react.interfaces;

public interface CodePushDownloadProgressListener {
    void downloadProgressChanged(long receivedBytes, long totalBytes);
}
