package com.microsoft.codepush.react.enums;

import com.google.gson.annotations.SerializedName;

public enum CodePushCheckFrequency {
    @SerializedName("0")
    ON_APP_START(0),

    @SerializedName("1")
    ON_APP_RESUME(1),

    @SerializedName("2")
    MANUAL(2);

    private final int value;
    CodePushCheckFrequency(int value) {
        this.value = value;
    }
    public int getValue() {
        return this.value;
    }
}