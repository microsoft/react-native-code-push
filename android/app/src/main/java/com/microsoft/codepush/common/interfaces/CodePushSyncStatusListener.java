package com.microsoft.codepush.common.interfaces;

import com.microsoft.codepush.common.enums.CodePushSyncStatus;

/**
 * Interface for listener of sync status event.
 */
public interface CodePushSyncStatusListener {

    /**
     * Callback for handling sync status changed event.
     *
     * @param syncStatus new synchronization status.
     */
    void syncStatusChanged(CodePushSyncStatus syncStatus);
}
