package com.microsoft.codepush.react.datacontracts;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.microsoft.codepush.react.datacontracts.CodePushPackage;

public class CodePushLocalPackage extends CodePushPackage {
    @SerializedName("isPending")
    public final boolean IsPending;

    @SerializedName("isFirstRun")
    public final boolean IsFirstRun;

    @SerializedName("_isDebugOnly")
    public final boolean IsDebugOnly;

    @Expose
    public Exception DownloadException;

    public CodePushLocalPackage(Exception downloadException) {
        this(null, "", "", false, false, false, false, "" , "", false);
        DownloadException = downloadException;
    }

    public CodePushLocalPackage(
            final String appVersion,
            final String deploymentKey,
            final String description,
            final boolean failedInstall,
            final boolean isFirstRun,
            final boolean isMandatory,
            final boolean isPending,
            final String label,
            final String packageHash,
            final boolean isDebugOnly
    ) {
        super(appVersion, deploymentKey, description, failedInstall, isMandatory, label, packageHash);
        IsPending = isPending;
        IsFirstRun = isFirstRun;
        IsDebugOnly = isDebugOnly;
    }
}
