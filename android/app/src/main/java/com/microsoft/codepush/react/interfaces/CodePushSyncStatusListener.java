package com.microsoft.codepush.react.interfaces;

import com.microsoft.codepush.react.enums.CodePushSyncStatus;

public interface CodePushSyncStatusListener {
    void syncStatusChanged(CodePushSyncStatus syncStatus);
}
