package com.microsoft.codepush.react;

import android.app.Application;
import android.content.Context;

import com.microsoft.appcenter.crashes.Crashes;
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
     * Application instance.
     */
    private Application mApplication;

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
     * The value of app secret from AppCenter portal to configure {@link Crashes} sdk.
     */
    private String mAppSecret;

    /**
     * App name for use when utilizing multiple CodePush instances to differentiate file locations.
     * If not provided, defaults to CodePushConstants.CODE_PUSH_DEFAULT_APP_NAME.
     */
    private String mAppName;

    /**
     * Semantic version for app for use when getting updates.
     * If not provided, defaults to <code>versionName</code> field from <code>build.gradle</code>.
     */
    private String mAppVersion;

    /**
     * Base directory for CodePush files.
     * If not provided, defaults to /data/data/<package>/files.
     */
    private String mBaseDirectory;

    /**
     * Creates a builder with initial parameters.
     *
     * @param deploymentKey application deployment key.
     * @param context       application context.
     */
    public CodePushBuilder(String deploymentKey, Context context) {
        mDeploymentKey = deploymentKey;
        mContext = context;
    }

    /**
     * Creates a builder with initial parameters for those who want to track exceptions.
     *
     * @param deploymentKey application deployment key.
     * @param application   application instance.
     * @param appSecret     the value of app secret from AppCenter portal to configure {@link Crashes} sdk.
     */
    public CodePushBuilder(String deploymentKey, Application application, String appSecret) {
        mDeploymentKey = deploymentKey;
        mApplication = application;
        mAppSecret = appSecret;
    }

    /**
     * Sets whether application is running in debug mode.
     *
     * @param isDebugMode whether application is running in debug mode.
     * @return instance of {@link CodePushBuilder}.
     */
    public CodePushBuilder setIsDebugMode(boolean isDebugMode) {
        mIsDebugMode = isDebugMode;
        return this;
    }

    /**
     * Sets CodePush server URL.
     *
     * @param serverUrl CodePush server URL.
     * @return instance of {@link CodePushBuilder}.
     */
    public CodePushBuilder setServerUrl(String serverUrl) {
        mServerUrl = serverUrl;
        return this;
    }

    /**
     * Sets public-key related resource descriptor.
     *
     * @param publicKeyResourceDescriptor public-key related resource descriptor.
     * @return instance of {@link CodePushBuilder}.
     */
    public CodePushBuilder setPublicKeyResourceDescriptor(Integer publicKeyResourceDescriptor) {
        mPublicKeyResourceDescriptor = publicKeyResourceDescriptor;
        return this;
    }

    /**
     * Sets path to the application entry point.
     *
     * @param appEntryPoint path to the application entry point.
     * @return instance of {@link CodePushBuilder}.
     */
    public CodePushBuilder setAppEntryPoint(String appEntryPoint) {
        mAppEntryPoint = appEntryPoint;
        return this;
    }

    /**
     * Sets name of application.
     *
     * @param appName name of application.
     * @return instance of {@link CodePushBuilder}.
     */
    public CodePushBuilder setAppName(String appName) {
        mAppName = appName;
        return this;
    }

    /**
     * Sets version of application.
     *
     * @param appVersion semantic version of application.
     * @return instance of {@link CodePushBuilder}.
     */
    public CodePushBuilder setAppVersion(String appVersion) {
        mAppVersion = appVersion;
        return this;
    }

    /**
     * Sets base directory for CodePush files.
     *
     * @param baseDirectory base directory for CodePush instance.
     * @return instance of {@link CodePushBuilder}.
     */
    public CodePushBuilder setBaseDir(String baseDirectory) {
        mBaseDirectory = baseDirectory;
        return this;
    }

    /**
     * Builds {@link CodePush}.
     *
     * @return instance of {@link CodePush}.
     * @throws CodePushInitializeException initialization exception.
     */
    public CodePush build() throws CodePushInitializeException {
        if (mAppSecret == null) {
            return new CodePush(
                    this.mDeploymentKey,
                    this.mContext,
                    this.mIsDebugMode,
                    this.mBaseDirectory,
                    this.mServerUrl,
                    this.mAppName,
                    this.mAppVersion,
                    this.mPublicKeyResourceDescriptor,
                    this.mAppEntryPoint
            );
        } else {
            return new CodePush(
                    this.mDeploymentKey,
                    this.mApplication,
                    this.mAppSecret,
                    this.mIsDebugMode,
                    this.mBaseDirectory,
                    this.mServerUrl,
                    this.mAppName,
                    this.mAppVersion,
                    this.mPublicKeyResourceDescriptor,
                    this.mAppEntryPoint
            );
        }
    }
}
