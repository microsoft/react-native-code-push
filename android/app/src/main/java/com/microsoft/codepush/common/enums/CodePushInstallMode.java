package com.microsoft.codepush.common.enums;

import com.google.gson.annotations.SerializedName;

/**
 * Indicates when you would like an installed update to actually be applied.
 */
public enum CodePushInstallMode {

    /**
     * Indicates that you want to install the update and restart the app immediately.
     */
    @SerializedName("0")
    IMMEDIATE(0),

    /**
     * Indicates that you want to install the update, but not forcibly restart the app.
     */
    @SerializedName("1")
    ON_NEXT_RESTART(1),

    /**
     * Indicates that you want to install the update, but don't want to restart the
     * app until the next time the end user resumes it from the background.
     */
    @SerializedName("2")
    ON_NEXT_RESUME(2),

    /**
     * Indicates that you want to install the update when the app is in the background,
     * but only after it has been in the background for "minimumBackgroundDuration" seconds (0 by default),
     * so that user context isn't lost unless the app suspension is long enough to not matter.
     */
    @SerializedName("3")
    ON_NEXT_SUSPEND(3);

    private final int value;

    CodePushInstallMode(int value) {
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