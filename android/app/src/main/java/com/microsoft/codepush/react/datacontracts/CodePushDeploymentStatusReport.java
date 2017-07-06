package com.microsoft.codepush.react.datacontracts;

import com.google.gson.annotations.SerializedName;

public class CodePushDeploymentStatusReport {
    @SerializedName("appVersion")
    public final String AppVersion;

    @SerializedName("deploymentKey")
    public final String DeploymentKey;

    @SerializedName("clientUniqueId")
    public final String ClientUniqueId;

    @SerializedName("previousDeploymentKey")
    public final String PreviousDeploymentKey;

    @SerializedName("previousLabelOrAppVersion")
    public final String PreviousLabelOrAppVersion;

    @SerializedName("label")
    public final String Label;

    @SerializedName("status")
    public final String Status;

    public CodePushDeploymentStatusReport(
            final String appVersion,
            final String deploymentKey,
            final String clientUniqueId,
            final String previousDeploymentKey,
            final String previousLabelOrAppVersion,
            final String label,
            final String status
    ) {
        AppVersion = appVersion;
        DeploymentKey = deploymentKey;
        ClientUniqueId = clientUniqueId;
        PreviousDeploymentKey = previousDeploymentKey;
        PreviousLabelOrAppVersion = previousLabelOrAppVersion;
        Label = label;
        Status = status;
    }
}
