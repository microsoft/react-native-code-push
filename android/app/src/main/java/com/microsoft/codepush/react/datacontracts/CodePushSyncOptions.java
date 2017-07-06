package com.microsoft.codepush.react.datacontracts;

import com.google.gson.annotations.SerializedName;
import com.microsoft.codepush.react.enums.CodePushCheckFrequency;
import com.microsoft.codepush.react.enums.CodePushInstallMode;

public class CodePushSyncOptions {
    @SerializedName("deploymentKey")
    public String DeploymentKey;

    @SerializedName("installMode")
    public CodePushInstallMode InstallMode;

    @SerializedName("mandatoryInstallMode")
    public CodePushInstallMode MandatoryInstallMode;

    @SerializedName("minimumBackgroundDuration")
    public Integer MinimumBackgroundDuration;

    public Boolean IgnoreFailedUpdates = true;

    @SerializedName("updateDialog")
    public CodePushUpdateDialog UpdateDialog;

    @SerializedName("checkFrequency")
    public CodePushCheckFrequency CheckFrequency;
}
