package com.microsoft.codepush.react.datacontracts;

import com.google.gson.annotations.SerializedName;

public class CodePushUpdateRequest {
    @SerializedName("deploymentKey")
    public final String DeploymentKey;

    @SerializedName("appVersion")
    public final String AppVersion;

    @SerializedName("packageHash")
    public final String PackageHash;

    @SerializedName("isCompanion")
    public final boolean IsCompanion;

    @SerializedName("label")
    public final String Label;

    @SerializedName("clientUniqueId")
    public final String ClientUniqueId;

    public CodePushUpdateRequest(
            final String deploymentKey,
            final String appVersion,
            final String packageHash,
            final boolean isCompanion,
            final String label,
            final String clientUniqueId
    ) {
        AppVersion = appVersion;
        DeploymentKey = deploymentKey;
        PackageHash = packageHash;
        IsCompanion = isCompanion;
        Label = label;
        ClientUniqueId = clientUniqueId;
    }
}
