package com.microsoft.codepush.common.datacontracts;

import com.google.gson.annotations.SerializedName;

/**
 * Represents information about a remote package (on server).
 */
public class CodePushRemotePackage extends CodePushPackage {

    /**
     * Url to access package on server.
     */
    @SerializedName("downloadUrl")
    private String downloadUrl;

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
     * Creates an instance of the class from the basic codepush package.
     *
     * @param failedInstall    whether this update has been previously installed but was rolled back.
     * @param packageSize      the size of the package.
     * @param downloadUrl      url to access package on server.
     * @param updateAppVersion whether the client should trigger a store update.
     * @param codePushPackage  basic package containing the information.
     * @return instance of the {@link CodePushRemotePackage}.
     */
    public static CodePushRemotePackage createRemotePackage(final boolean failedInstall, final long packageSize,
                                                            final String downloadUrl, final boolean updateAppVersion,
                                                            final CodePushPackage codePushPackage) {
        CodePushRemotePackage codePushRemotePackage = new CodePushRemotePackage();
        codePushRemotePackage.setAppVersion(codePushPackage.getAppVersion());
        codePushRemotePackage.setDeploymentKey(codePushPackage.getDeploymentKey());
        codePushRemotePackage.setDescription(codePushPackage.getDescription());
        codePushRemotePackage.setFailedInstall(failedInstall);
        codePushRemotePackage.setMandatory(codePushPackage.isMandatory());
        codePushRemotePackage.setLabel(codePushPackage.getLabel());
        codePushRemotePackage.setPackageHash(codePushPackage.getPackageHash());
        codePushRemotePackage.setPackageSize(packageSize);
        codePushRemotePackage.setDownloadUrl(downloadUrl);
        codePushRemotePackage.setUpdateAppVersion(updateAppVersion);
        return codePushRemotePackage;
    }

    /**
     * Creates instance of the class from the update response from server.
     *
     * @param deploymentKey the deployment key that was used to originally download this update.
     * @param updateInfo    update info response from server.
     * @return instance of the {@link CodePushRemotePackage}.
     */
    public static CodePushRemotePackage createRemotePackageFromUpdateInfo(String deploymentKey, CodePushUpdateResponseUpdateInfo updateInfo) {
        CodePushRemotePackage codePushRemotePackage = new CodePushRemotePackage();
        codePushRemotePackage.setAppVersion(updateInfo.getAppVersion());
        codePushRemotePackage.setDeploymentKey(deploymentKey);
        codePushRemotePackage.setDescription(updateInfo.getDescription());
        codePushRemotePackage.setFailedInstall(false);
        codePushRemotePackage.setMandatory(updateInfo.isMandatory());
        codePushRemotePackage.setLabel(updateInfo.getLabel());
        codePushRemotePackage.setPackageHash(updateInfo.getPackageHash());
        codePushRemotePackage.setPackageSize(updateInfo.getPackageSize());
        codePushRemotePackage.setDownloadUrl(updateInfo.getDownloadUrl());
        codePushRemotePackage.setUpdateAppVersion(updateInfo.isUpdateAppVersion());
        return codePushRemotePackage;
    }

    /**
     * Creates a default package from the app version.
     *
     * @param appVersion       current app version.
     * @param updateAppVersion whether the client should trigger a store update.
     * @return instance of the {@link CodePushRemotePackage}.
     */
    public static CodePushRemotePackage createDefaultRemotePackage(final String appVersion, final boolean updateAppVersion) {
        CodePushRemotePackage codePushRemotePackage = new CodePushRemotePackage();
        codePushRemotePackage.setAppVersion(appVersion);
        codePushRemotePackage.setUpdateAppVersion(updateAppVersion);
        return codePushRemotePackage;
    }

    /**
     * Gets url to access package on server and returns it.
     *
     * @return url to access package on server.
     */
    public String getDownloadUrl() {
        return downloadUrl;
    }

    /**
     * Sets url to access package on server.
     *
     * @param downloadUrl url to access package on server.
     */
    @SuppressWarnings("WeakerAccess")
    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    /**
     * Gets size of the package and returns it.
     *
     * @return size of the package.
     */
    public long getPackageSize() {
        return packageSize;
    }

    /**
     * Sets size of the package.
     *
     * @param packageSize size of the package.
     */
    @SuppressWarnings("WeakerAccess")
    public void setPackageSize(long packageSize) {
        this.packageSize = packageSize;
    }

    /**
     * Gets whether the client should trigger a store update and returns it.
     *
     * @return whether the client should trigger a store update.
     */
    public boolean isUpdateAppVersion() {
        return updateAppVersion;
    }

    /**
     * Sets whether the client should trigger a store update.
     *
     * @param updateAppVersion whether the client should trigger a store update.
     */
    @SuppressWarnings("WeakerAccess")
    public void setUpdateAppVersion(boolean updateAppVersion) {
        this.updateAppVersion = updateAppVersion;
    }
}
