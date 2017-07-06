package com.microsoft.codepush.react.datacontracts;

import com.google.gson.annotations.SerializedName;

public class CodePushPackage {
    @SerializedName("appVersion")
    public final String AppVersion;

    @SerializedName("deploymentKey")
    public final String DeploymentKey;

    @SerializedName("description")
    public final String Description;

    @SerializedName("failedInstall")
    public final boolean FailedInstall;

    @SerializedName("isMandatory")
    public final boolean IsMandatory;

    @SerializedName("label")
    public final String Label;

    @SerializedName("packageHash")
    public final String PackageHash;

    public CodePushPackage(
            final String appVersion,
            final String deploymentKey,
            final String description,
            final boolean failedInstall,
            final boolean isMandatory,
            final String label,
            final String packageHash
    ) {
        AppVersion = appVersion;
        DeploymentKey = deploymentKey;
        Description = description;
        FailedInstall = failedInstall;
        IsMandatory = isMandatory;
        Label = label;
        PackageHash = packageHash;
    }
}
