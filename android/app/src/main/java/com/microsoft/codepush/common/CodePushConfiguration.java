package com.microsoft.codepush.common;

import com.microsoft.appcenter.utils.AppCenterLog;

/**
 * Provides info regarding current app state and settings.
 */
public final class CodePushConfiguration {

    /**
     * Value of <code>versionName</code> parameter from <code>build.gradle</code>.
     */
    private String appVersion;

    /**
     * Android client unique id.
     */
    private String clientUniqueId;

    /**
     * CodePush deployment key.
     */
    private String deploymentKey;

    /**
     * CodePush acquisition server URL.
     */
    private String serverUrl;

    /**
     * Package hash of currently running CodePush update.
     * See {@link com.microsoft.codepush.common.enums.CodePushUpdateState} for details.
     */
    private String packageHash;

    /**
     * Get the appVersion value.
     *
     * @return appVersion value.
     */
    public String getAppVersion() {
        return this.appVersion;
    }

    /**
     * Get the clientUniqueId value.
     *
     * @return the clientUniqueId value.
     */
    public String getClientUniqueId() {
        return this.clientUniqueId;
    }

    /**
     * Get the deploymentKey value.
     *
     * @return the deploymentKey value.
     */
    public String getDeploymentKey() {
        return this.deploymentKey;
    }

    /**
     * Get the serverUrl value.
     *
     * @return the serverUrl value.
     */
    public String getServerUrl() {
        return this.serverUrl;
    }

    /**
     * Get the packageHash value.
     *
     * @return the packageHash value.
     */
    public String getPackageHash() {
        return this.packageHash;
    }

    /**
     * Set the appVersion value.
     *
     * @param appVersion the appVersion value to set.
     * @return this instance.
     */
    public CodePushConfiguration setAppVersion(final String appVersion) {
        if (appVersion != null) {
            this.appVersion = appVersion;
        } else {
            AppCenterLog.error(CodePush.LOG_TAG, "\"appVersion\" property cannot be null.");
        }
        return this;
    }

    /**
     * Set the clientUniqueId value.
     *
     * @param clientUniqueId the clientUniqueId value to set.
     * @return this instance.
     */
    public CodePushConfiguration setClientUniqueId(final String clientUniqueId) {
        if (clientUniqueId != null) {
            this.clientUniqueId = clientUniqueId;
        } else {
            AppCenterLog.error(CodePush.LOG_TAG, "\"clientUniqueId\" property cannot be null.");
        }
        return this;
    }

    /**
     * Set the deploymentKey value.
     *
     * @param deploymentKey the deploymentKey value to set.
     * @return this instance.
     */
    public CodePushConfiguration setDeploymentKey(final String deploymentKey) {
        if (deploymentKey != null) {
            this.deploymentKey = deploymentKey;
        } else {
            AppCenterLog.error(CodePush.LOG_TAG, "\"deploymentKey\" property cannot be null.");
        }
        return this;
    }

    /**
     * Set the serverUrl value.
     *
     * @param serverUrl the serverUrl value to set.
     * @return this instance.
     */
    public CodePushConfiguration setServerUrl(final String serverUrl) {
        if (serverUrl != null) {
            this.serverUrl = serverUrl;
        } else {
            AppCenterLog.error(CodePush.LOG_TAG, "\"serverUrl\" property cannot be null.");
        }
        return this;
    }

    /**
     * Set the packageHash value.
     *
     * @param packageHash the serverUrl value to set.
     * @return this instance.
     */
    public CodePushConfiguration setPackageHash(final String packageHash) {
        this.packageHash = packageHash;
        return this;
    }
}