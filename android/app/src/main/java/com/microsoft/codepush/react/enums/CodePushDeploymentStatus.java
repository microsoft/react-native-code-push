package com.microsoft.codepush.react.enums;

public enum CodePushDeploymentStatus {
    SUCCEEDED("DeploymentSucceeded"),
    FAILED("DeploymentFailed");

    private final String value;
    CodePushDeploymentStatus(String value) {
        this.value = value;
    }
    public String getValue() {
        return this.value;
    }
}