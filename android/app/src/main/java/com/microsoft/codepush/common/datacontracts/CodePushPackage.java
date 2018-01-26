package com.microsoft.codepush.common.datacontracts;

import com.google.gson.annotations.SerializedName;

/**
 * Basic package class. Contains all the basic information about the update package.
 * Extended by {@link CodePushRemotePackage} and {@link CodePushLocalPackage}.
 */
public class CodePushPackage {

    /**
     * The app binary version that this update is dependent on. This is the value that was
     * specified via the appStoreVersion parameter when calling the CLI's release command.
     */
    @SerializedName("appVersion")
    private String appVersion;

    /**
     * The deployment key that was used to originally download this update.
     */
    @SerializedName("deploymentKey")
    private String deploymentKey;

    /**
     * The description of the update. This is the same value that you specified in the CLI when you released the update.
     */
    @SerializedName("description")
    private String description;

    /**
     * Indicates whether this update has been previously installed but was rolled back.
     */
    @SerializedName("failedInstall")
    private boolean failedInstall;

    /**
     * Indicates whether the update is considered mandatory.
     * This is the value that was specified in the CLI when the update was released.
     */
    @SerializedName("isMandatory")
    private boolean isMandatory;

    /**
     * The internal label automatically given to the update by the CodePush server.
     * This value uniquely identifies the update within its deployment.
     */
    @SerializedName("label")
    private String label;

    /**
     * The SHA hash value of the update.
     */
    @SerializedName("packageHash")
    private String packageHash;

    /**
     * Gets the app binary version that this update is dependent on.
     *
     * @return the app binary version that this update is dependent on.
     */
    public String getAppVersion() {
        return appVersion;
    }

    /**
     * Sets the app binary version that this update is dependent on.
     *
     * @param appVersion the app binary version that this update is dependent on.
     */
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    /**
     * Gets the deployment key that was used to originally download this update and returns it.
     *
     * @return the deployment key that was used to originally download this update.
     */
    public String getDeploymentKey() {
        return deploymentKey;
    }

    /**
     * Sets the deployment key that was used to originally download this update.
     *
     * @param deploymentKey the deployment key that was used to originally download this update.
     */
    public void setDeploymentKey(String deploymentKey) {
        this.deploymentKey = deploymentKey;
    }

    /**
     * Gets the description of the update This is the same value that you specified in the CLI when you released the update and returns it.
     *
     * @return the description of the update This is the same value that you specified in the CLI when you released the update.
     */
    @SuppressWarnings("WeakerAccess")
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the update This is the same value that you specified in the CLI when you released the update.
     *
     * @param description the description of the update This is the same value that you specified in the CLI when you released the update.
     */
    @SuppressWarnings("WeakerAccess")
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets whether this update has been previously installed but was rolled back and returns it.
     *
     * @return whether this update has been previously installed but was rolled back.
     */
    public boolean isFailedInstall() {
        return failedInstall;
    }

    /**
     * Sets whether this update has been previously installed but was rolled back.
     *
     * @param failedInstall whether this update has been previously installed but was rolled back.
     */
    @SuppressWarnings("WeakerAccess")
    public void setFailedInstall(boolean failedInstall) {
        this.failedInstall = failedInstall;
    }

    /**
     * Gets whether the update is considered mandatory and returns it.
     *
     * @return whether the update is considered mandatory.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isMandatory() {
        return isMandatory;
    }

    /**
     * Sets whether the update is considered mandatory.
     *
     * @param mandatory whether the update is considered mandatory.
     */
    @SuppressWarnings("WeakerAccess")
    public void setMandatory(boolean mandatory) {
        isMandatory = mandatory;
    }

    /**
     * Gets the internal label automatically given to the update by the CodePush server and returns it.
     *
     * @return the internal label automatically given to the update by the CodePush server.
     */
    @SuppressWarnings("WeakerAccess")
    public String getLabel() {
        return label;
    }

    /**
     * Sets the internal label automatically given to the update by the CodePush server.
     *
     * @param label the internal label automatically given to the update by the CodePush server.
     */
    @SuppressWarnings("WeakerAccess")
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Gets the SHA hash value of the update and returns it.
     *
     * @return the SHA hash value of the update.
     */
    public String getPackageHash() {
        return packageHash;
    }

    /**
     * Sets the SHA hash value of the update.
     *
     * @param packageHash the SHA hash value of the update.
     */
    public void setPackageHash(String packageHash) {
        this.packageHash = packageHash;
    }
}
