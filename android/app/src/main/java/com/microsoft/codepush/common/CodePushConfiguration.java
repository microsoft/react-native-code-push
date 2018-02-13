package com.microsoft.codepush.common;

import com.microsoft.codepush.common.exceptions.CodePushIllegalArgumentException;

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
    public CodePushConfiguration setAppVersion(final String appVersion) throws CodePushIllegalArgumentException {
        if (appVersion != null) {
            this.appVersion = appVersion;
        } else {
            throw new CodePushIllegalArgumentException(this.getClass().getName(), "appVersion");
        }
        return this;
    }

    /**
     * Set the clientUniqueId value.
     *
     * @param clientUniqueId the clientUniqueId value to set.
     * @return this instance.
     */
    public CodePushConfiguration setClientUniqueId(final String clientUniqueId) throws CodePushIllegalArgumentException {
        if (clientUniqueId != null) {
            this.clientUniqueId = clientUniqueId;
        } else {
            throw new CodePushIllegalArgumentException(this.getClass().getName(), "clientUniqueId");
        }
        return this;
    }

    /**
     * Set the deploymentKey value.
     *
     * @param deploymentKey the deploymentKey value to set.
     * @return this instance.
     */
    public CodePushConfiguration setDeploymentKey(final String deploymentKey) throws CodePushIllegalArgumentException {
        if (deploymentKey != null) {
            this.deploymentKey = deploymentKey;
        } else {
            throw new CodePushIllegalArgumentException(this.getClass().getName(), "deploymentKey");
        }
        return this;
    }

    /**
     * Set the serverUrl value.
     *
     * @param serverUrl the serverUrl value to set.
     * @return this instance.
     */
    public CodePushConfiguration setServerUrl(final String serverUrl) throws CodePushIllegalArgumentException {
        if (serverUrl != null) {
            this.serverUrl = serverUrl;
        } else {
            throw new CodePushIllegalArgumentException(this.getClass().getName(), "serverUrl");
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