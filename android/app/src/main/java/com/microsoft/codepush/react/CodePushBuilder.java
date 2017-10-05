package com.microsoft.codepush.react;

import android.content.Context;

public class CodePushBuilder {
    private String mDeploymentKey;
    private Context mContext;
    private boolean mIsDebugMode;
    private String mServerUrl;
    private Integer mPublicKeyResourceDescriptor;
    private String mJsBundleFileName;

    public CodePushBuilder(String deploymentKey, Context context) {
        this.mDeploymentKey = deploymentKey;
        this.mContext = context;
        this.mServerUrl = CodePushCore.getServerUrl();
    }

    public CodePushBuilder setIsDebugMode(boolean isDebugMode) {
        this.mIsDebugMode = isDebugMode;
        return this;
    }

    public CodePushBuilder setServerUrl(String serverUrl) {
        this.mServerUrl = serverUrl;
        return this;
    }

    public CodePushBuilder setPublicKeyResourceDescriptor(Integer publicKeyResourceDescriptor) {
        this.mPublicKeyResourceDescriptor = publicKeyResourceDescriptor;
        return this;
    }

    public CodePushBuilder setJsBundleFileName(String jsBundleFileName) {
        this.mJsBundleFileName = jsBundleFileName;
        return this;
    }

    public CodePush build() {
        return new CodePush(
                this.mDeploymentKey,
                this.mContext,
                this.mIsDebugMode,
                this.mServerUrl,
                this.mPublicKeyResourceDescriptor,
                this.mJsBundleFileName
        );
    }
}
