package com.microsoft.codepush.react.enums;

import com.google.gson.annotations.SerializedName;

public enum CodePushSyncStatus {
    @SerializedName("0")
    UP_TO_DATE(0),

    @SerializedName("1")
    UPDATE_INSTALLED(1),

    @SerializedName("2")
    UPDATE_IGNORED(2),

    @SerializedName("3")
    UNKNOWN_ERROR(3),

    @SerializedName("4")
    SYNC_IN_PROGRESS(4),

    @SerializedName("5")
    CHECKING_FOR_UPDATE(5),

    @SerializedName("6")
    AWAITING_USER_ACTION(6),

    @SerializedName("7")
    DOWNLOADING_PACKAGE(7),

    @SerializedName("8")
    INSTALLING_UPDATE(8);

    private final int value;
    CodePushSyncStatus(int value) {
        this.value = value;
    }
    public int getValue() {
        return this.value;
    }
}