package com.microsoft.codepush.react.datacontracts;

import com.google.gson.annotations.SerializedName;
import com.microsoft.codepush.react.enums.CodePushDeploymentStatus;
import com.microsoft.codepush.react.enums.CodePushSyncStatus;

public class CodePushStatusReport {
    @SerializedName("appVersion")
    public String AppVersion;

    @SerializedName("deploymentKey")
    public String DeploymentKey;

    @SerializedName("label")
    public String Label;

    @SerializedName("package")
    public CodePushLocalPackage Package;

    @SerializedName("previousDeploymentKey")
    public String PreviousDeploymentKey;

    @SerializedName("previousLabelOrAppVersion")
    public String PreviousLabelOrAppVersion;

    @SerializedName("status")
    public CodePushDeploymentStatus Status;
}
