package com.microsoft.codepush.react;

import com.microsoft.codepush.react.enums.CodePushInstallMode;

public class CodePushSyncOptions {
    public String DeploymentKey;

    public CodePushInstallMode InstallMode;

    public CodePushInstallMode MandatoryInstallMode;

    public Integer MinimumBackgroundDuration;

    public Boolean IgnoreFailedUpdates = true;
}
