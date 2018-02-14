package com.microsoft.codepush.reactv2;

import android.content.Context;

import com.microsoft.codepush.common.exceptions.CodePushInitializeException;

/**
 * A builder for {@link CodePush} class.
 */
public class CodePushBuilder {

    /**
     * Application deployment key.
     */
    private String mDeploymentKey;

    /**
     * Application context.
     */
    private Context mContext;

    /**
     * Whether the application is running in debug mode.
     */
    private boolean mIsDebugMode;

    /**
     * CodePush server URL.
     */
    private String mServerUrl;

    /**
     * Public-key related resource descriptor.
     */
    private Integer mPublicKeyResourceDescriptor;

    /**
     * Path to the application entry point.
     */
    private String mAppEntryPoint;

    /**
     * Creates a builder with initial parameters.
     *
     * @param deploymentKey application deployment key.
     * @param context       application context.
     */
    public CodePushBuilder(String deploymentKey, Context context) {
        this.mDeploymentKey = deploymentKey;
        this.mContext = context;
    }

    /**
     * Sets whether application is running in debug mode.
     *
     * @param isDebugMode whether application is running in debug mode.
     * @return instance of {@link CodePushBuilder}.
     */
    public CodePushBuilder setIsDebugMode(boolean isDebugMode) {
        this.mIsDebugMode = isDebugMode;
        return this;
    }

    /**
     * Sets CodePush server URL.
     *
     * @param serverUrl CodePush server URL.
     * @return instance of {@link CodePushBuilder}.
     */
    public CodePushBuilder setServerUrl(String serverUrl) {
        this.mServerUrl = serverUrl;
        return this;
    }

    /**
     * Sets public-key related resource descriptor.
     *
     * @param publicKeyResourceDescriptor public-key related resource descriptor.
     * @return instance of {@link CodePushBuilder}.
     */
    public CodePushBuilder setPublicKeyResourceDescriptor(Integer publicKeyResourceDescriptor) {
        this.mPublicKeyResourceDescriptor = publicKeyResourceDescriptor;
        return this;
    }

    /**
     * Sets path to the application entry point.
     *
     * @param appEntryPoint path to the application entry point.
     * @return instance of {@link CodePushBuilder}.
     */
    public CodePushBuilder setAppEntryPoint(String appEntryPoint) {
        this.mAppEntryPoint = appEntryPoint;
        return this;
    }

    /**
     * Builds {@link CodePush}.
     *
     * @return instance of {@link CodePush}.
     * @throws CodePushInitializeException initialization exception.
     */
    public CodePush build() throws CodePushInitializeException {
        return new CodePush(
                this.mDeploymentKey,
                this.mContext,
                this.mIsDebugMode,
                this.mServerUrl,
                this.mPublicKeyResourceDescriptor,
                this.mAppEntryPoint
        );
    }
}
