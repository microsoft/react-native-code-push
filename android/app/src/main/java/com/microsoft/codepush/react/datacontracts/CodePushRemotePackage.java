package com.microsoft.codepush.react.datacontracts;

import com.google.gson.annotations.SerializedName;
import com.microsoft.codepush.react.datacontracts.CodePushPackage;

public class CodePushRemotePackage extends CodePushPackage {
    @SerializedName("downloadUrl")
    public final String DownloadUrl;

    @SerializedName("packageSize")
    public final long PackageSize;

    @SerializedName("updateAppVersion")
    public final boolean UpdateAppVersion;

    public CodePushRemotePackage(
            final String appVersion,
            final String deploymentKey,
            final String description,
            final boolean failedInstall,
            final boolean isMandatory,
            final String label,
            final String packageHash,
            final long packageSize,
            final String downloadUrl,
            final boolean updateAppVersion
    ) {
        super(appVersion, deploymentKey, description, failedInstall, isMandatory, label, packageHash);
        DownloadUrl = downloadUrl;
        PackageSize = packageSize;
        UpdateAppVersion = updateAppVersion;
    }
}
