package com.microsoft.codepush.react.datacontracts;

import com.google.gson.annotations.SerializedName;

public class CodePushDownloadStatusReport {
    @SerializedName("clientUniqueId")
    public final String ClientUniqueId;

    @SerializedName("deploymentKey")
    public final String DeploymentKey;

    @SerializedName("label")
    public final String Label;

    public CodePushDownloadStatusReport(
            final String clientUniqueId,
            final String deploymentKey,
            final String label
    ) {
        ClientUniqueId = clientUniqueId;
        DeploymentKey = deploymentKey;
        Label = label;
    }
}
