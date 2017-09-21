package com.microsoft.codepush.react;

public final class CodePushConfiguration {
    public final String AppVersion;
    public final String ClientUniqueId;
    public final String DeploymentKey;
    public final String ServerUrl;
    public final String PackageHash;
    public final String HandleBinaryVersionMismatchCallback;
    public CodePushConfiguration(
            final String appVersion,
            final String clientUniqueId,
            final String deploymentKey,
            final String serverUrl,
            final String packageHash,
            final String handleBinaryVersionMismatchCallback
    ) {
        AppVersion = appVersion;
        ClientUniqueId = clientUniqueId;
        DeploymentKey = deploymentKey;
        ServerUrl = serverUrl;
        PackageHash = packageHash;
        HandleBinaryVersionMismatchCallback = handleBinaryVersionMismatchCallback;
    }
}
