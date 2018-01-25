package com.microsoft.codepush.common.enums;

import com.google.gson.annotations.SerializedName;

/**
 * Indicates the current status of a sync operation.
 */
public enum CodePushSyncStatus {

    /**
     * The app is up-to-date with the CodePush server.
     */
    @SerializedName("0")
    UP_TO_DATE(0),

    /**
     * An available update has been installed and will be run either immediately after the
     * <code>syncStatusChangedCallback</code> function returns or the next time the app resumes/restarts,
     * depending on the {@link CodePushInstallMode} specified in CodePushSyncOptions.
     */
    @SerializedName("1")
    UPDATE_INSTALLED(1),

    /**
     * The app had an optional update which the end user chose to ignore.
     * (This is only applicable when the updateDialog is used).
     */
    @SerializedName("2")
    UPDATE_IGNORED(2),

    /**
     * The sync operation encountered an unknown error.
     */
    @SerializedName("3")
    UNKNOWN_ERROR(3),

    /**
     * There is an ongoing sync operation running which prevents the current call from being executed.
     */
    @SerializedName("4")
    SYNC_IN_PROGRESS(4),

    /**
     * The CodePush server is being queried for an update.
     */
    @SerializedName("5")
    CHECKING_FOR_UPDATE(5),

    /**
     * An update is available, and a confirmation dialog was shown
     * to the end user. (This is only applicable when the <code>updateDialog</code> is used).
     */
    @SerializedName("6")
    AWAITING_USER_ACTION(6),

    /**
     * An available update is being downloaded from the CodePush server.
     */
    @SerializedName("7")
    DOWNLOADING_PACKAGE(7),

    /**
     * An available update was downloaded and is about to be installed.
     */
    @SerializedName("8")
    INSTALLING_UPDATE(8);

    private final int value;

    CodePushSyncStatus(int value) {
        this.value = value;
    }

    /**
     * Gets the assigned enum value.
     *
     * @return integer assigned to enum item.
     */
    public int getValue() {
        return this.value;
    }
}