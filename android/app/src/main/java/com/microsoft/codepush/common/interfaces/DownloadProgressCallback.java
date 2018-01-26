package com.microsoft.codepush.common.interfaces;

import com.microsoft.codepush.common.DownloadProgress;

/**
 * Interface for download progress of update callback.
 */
public interface DownloadProgressCallback {

    /**
     * Callback function for handling progress of downloading of update.
     *
     * @param downloadProgress Progress of downloading of update.
     */
    void call(DownloadProgress downloadProgress);
}
