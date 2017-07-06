package com.microsoft.codepush.react.enums;

public enum CodePushSyncStatus {
    UP_TO_DATE(0),
    UPDATE_INSTALLED(1),
    UPDATE_IGNORED(2),
    UNKNOWN_ERROR(3),
    SYNC_IN_PROGRESS(4),
    CHECKING_FOR_UPDATE(5),
    AWAITING_USER_ACTION(6),
    DOWNLOADING_PACKAGE(7),
    INSTALLING_UPDATE(8);

    private final int value;
    CodePushSyncStatus(int value) {
        this.value = value;
    }
    public int getValue() {
        return this.value;
    }
}