package com.microsoft.codepush.common;

import com.microsoft.codepush.common.exceptions.CodePushIllegalArgumentException;

/**
 * Provides info regarding current app state and settings.
 */
public final class CodePushConfiguration {

    /**
     * Application name, if provided.
     */
    private String appName;

    /**
     * Semantic version for app for use when getting updates, if provided.
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
     * CodePush base directory, if provided.
     */
    private String baseDirectory;

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
     * Get the appName value. May not be set.
     *
     * @return appName value.
     */
    public String getAppName() {
        return this.appName;
    }

    /**
     * Get the appVersion value. May not be set.
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
     * Get the baseDirectory value. May not be set.
     *
     * @return the baseDirectory value.
     */
    public String getBaseDirectory() {
        return this.baseDirectory;
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
     * Set the appName value.
     *
     * @param appName the appName value to set.
     * @return this instance.
     */
    public CodePushConfiguration setAppName(final String appName) throws CodePushIllegalArgumentException {
        if (appName != null) {
            this.appName = appName;
        } else {
            throw new CodePushIllegalArgumentException(this.getClass().getName(), "appName");
        }
        return this;
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
     * Set the baseDirectory value.
     *
     * @param baseDirectory the baseDirectory value to set.
     * @return this instance.
     */
    public CodePushConfiguration setBaseDirectory(final String baseDirectory) throws CodePushIllegalArgumentException {
        if (baseDirectory != null) {
            this.baseDirectory = baseDirectory;
        } else {
            throw new CodePushIllegalArgumentException(this.getClass().getName(), "baseDirectory");
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