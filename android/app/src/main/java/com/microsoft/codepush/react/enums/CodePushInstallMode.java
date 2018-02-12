package com.microsoft.codepush.react.enums;

import com.google.gson.annotations.SerializedName;

public enum CodePushInstallMode {
    @SerializedName("0")
    IMMEDIATE(0),

    @SerializedName("1")
    ON_NEXT_RESTART(1),

    @SerializedName("2")
    ON_NEXT_RESUME(2),

    @SerializedName("3")
    ON_NEXT_SUSPEND(3);

    private final int value;
    CodePushInstallMode(int value) {
        this.value = value;
    }
    public int getValue() {
        return this.value;
    }
}