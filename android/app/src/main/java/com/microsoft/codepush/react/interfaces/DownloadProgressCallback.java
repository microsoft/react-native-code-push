package com.microsoft.codepush.react.interfaces;

import com.microsoft.codepush.react.DownloadProgress;

public interface DownloadProgressCallback {
    void call(DownloadProgress downloadProgress);
}
