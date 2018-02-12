package com.microsoft.codepush.react.enums;

import com.google.gson.annotations.SerializedName;

public enum CodePushDeploymentStatus {
    @SerializedName("DeploymentSucceeded")
    SUCCEEDED("DeploymentSucceeded"),

    @SerializedName("DeploymentFailed")
    FAILED("DeploymentFailed");

    private final String value;
    CodePushDeploymentStatus(String value) {
        this.value = value;
    }
    public String getValue() {
        return this.value;
    }
}