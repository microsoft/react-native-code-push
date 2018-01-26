package com.microsoft.codepush.common.datacontracts;

import com.google.gson.annotations.SerializedName;

/**
 * Update info from the server.
 */
public class CodePushUpdateResponseUpdateInfo {

    /**
     * Url to access package on server.
     */
    @SerializedName("downloadURL")
    private String downloadUrl;

    /**
     * The description of the update.
     * This is the same value that you specified in the CLI when you released the update.
     */
    @SerializedName("description")
    private String description;

    /**
     * Whether the package is available (<code>false</code> if it it disabled).
     */
    @SerializedName("isAvailable")
    private boolean isAvailable;

    /**
     * Indicates whether the update is considered mandatory.
     * This is the value that was specified in the CLI when the update was released.
     */
    @SerializedName("isMandatory")
    private boolean isMandatory;

    /**
     * The app binary version that this update is dependent on. This is the value that was
     * specified via the appStoreVersion parameter when calling the CLI's release command.
     */
    @SerializedName("appVersion")
    private String appVersion;

    /**
     * The SHA hash value of the update.
     */
    @SerializedName("packageHash")
    private String packageHash;

    /**
     * The internal label automatically given to the update by the CodePush server.
     * This value uniquely identifies the update within its deployment.
     */
    @SerializedName("label")
    private String label;

    /**
     * Size of the package.
     */
    @SerializedName("packageSize")
    private long packageSize;

    /**
     * Whether the client should trigger a store update.
     */
    @SerializedName("updateAppVersion")
    private boolean updateAppVersion;

    /**
     * Set to <code>true</code> if the update directs to use the binary version of the application.
     */
    @SerializedName("shouldRunBinaryVersion")
    private boolean shouldRunBinaryVersion;

    /**
     * Gets url to access package on server and returns it.
     *
     * @return url to access package on server.
     */
    @SuppressWarnings("WeakerAccess")
    public String getDownloadUrl() {
        return downloadUrl;
    }

    /**
     * Sets url to access package on server.
     *
     * @param downloadUrl url to access package on server.
     */
    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    /**
     * Gets the description of the update and returns it.
     *
     * @return the description of the update.
     */
    @SuppressWarnings("WeakerAccess")
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the update.
     *
     * @param description the description of the update.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets whether the package is available and returns it.
     *
     * @return whether the package is available.
     */
    public boolean isAvailable() {
        return isAvailable;
    }

    /**
     * Sets whether the package is available.
     *
     * @param available whether the package is available.
     */
    public void setAvailable(boolean available) {
        isAvailable = available;
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
    public void setMandatory(boolean mandatory) {
        isMandatory = mandatory;
    }

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
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Gets size of the package and returns it.
     *
     * @return size of the package.
     */
    @SuppressWarnings("WeakerAccess")
    public long getPackageSize() {
        return packageSize;
    }

    /**
     * Sets size of the package.
     *
     * @param packageSize size of the package.
     */
    public void setPackageSize(long packageSize) {
        this.packageSize = packageSize;
    }

    /**
     * Gets whether the client should trigger a store update and returns it.
     *
     * @return whether the client should trigger a store update.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isUpdateAppVersion() {
        return updateAppVersion;
    }

    /**
     * Sets whether the client should trigger a store update.
     *
     * @param updateAppVersion whether the client should trigger a store update.
     */
    public void setUpdateAppVersion(boolean updateAppVersion) {
        this.updateAppVersion = updateAppVersion;
    }

    /**
     * Gets whether the update directs to use the binary version of the application and returns it.
     *
     * @return whether the update directs to use the binary version of the application.
     */
    public boolean isShouldRunBinaryVersion() {
        return shouldRunBinaryVersion;
    }

    /**
     * Sets whether the update directs to use the binary version of the application.
     *
     * @param shouldRunBinaryVersion whether the update directs to use the binary version of the application.
     */
    public void setShouldRunBinaryVersion(boolean shouldRunBinaryVersion) {
        this.shouldRunBinaryVersion = shouldRunBinaryVersion;
    }
}