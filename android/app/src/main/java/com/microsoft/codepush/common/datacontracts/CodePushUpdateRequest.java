package com.microsoft.codepush.common.datacontracts;

import com.google.gson.annotations.SerializedName;
import com.microsoft.codepush.common.exceptions.CodePushIllegalArgumentException;

/**
 * A request class for querying for updates.
 */
public class CodePushUpdateRequest {

    /**
     * Specifies the deployment key you want to query for an update against.
     */
    @SerializedName("deploymentKey")
    private String deploymentKey;

    /**
     * Specifies the current app version.
     */
    @SerializedName("appVersion")
    private String appVersion;

    /**
     * Specifies the current local package hash.
     */
    @SerializedName("packageHash")
    private String packageHash;

    /**
     * Whether to ignore the application version.
     */
    @SerializedName("isCompanion")
    private boolean isCompanion;

    /**
     * Specifies the current package label.
     */
    @SerializedName("label")
    private String label;

    /**
     * Device id.
     */
    @SerializedName("clientUniqueId")
    private String clientUniqueId;

    /**
     * Creates an update request based on the current {@link CodePushLocalPackage}.
     *
     * @param deploymentKey   the deployment key you want to query for an update against.
     * @param codePushPackage currently installed local package.
     * @param clientUniqueId  id of the device.
     * @return instance of the {@link CodePushUpdateRequest}.
     */
    public static CodePushUpdateRequest createUpdateRequest(final String deploymentKey,
                                                            final CodePushLocalPackage codePushPackage, final String clientUniqueId) throws CodePushIllegalArgumentException {
        CodePushUpdateRequest codePushUpdateRequest = new CodePushUpdateRequest();
        codePushUpdateRequest.setAppVersion(codePushPackage.getAppVersion());
        codePushUpdateRequest.setDeploymentKey(deploymentKey);
        codePushUpdateRequest.setPackageHash(codePushPackage.getPackageHash());
        codePushUpdateRequest.setLabel(codePushPackage.getLabel());
        codePushUpdateRequest.setClientUniqueId(clientUniqueId);
        return codePushUpdateRequest;
    }

    /**
     * Gets the deployment key you want to query for an update against and returns it.
     *
     * @return the deployment key you want to query for an update against.
     */
    public String getDeploymentKey() {
        return deploymentKey;
    }

    /**
     * Sets the deployment key you want to query for an update against.
     *
     * @param deploymentKey the deployment key you want to query for an update against.
     */
    public void setDeploymentKey(String deploymentKey) throws CodePushIllegalArgumentException {
        if (deploymentKey != null) {
            this.deploymentKey = deploymentKey;
        } else {
            throw new CodePushIllegalArgumentException(this.getClass().getName(), "deploymentKey");
        }
    }

    /**
     * Gets the current app version and returns it.
     *
     * @return the current app version.
     */
    public String getAppVersion() {
        return appVersion;
    }

    /**
     * Sets the current app version.
     *
     * @param appVersion the current app version.
     */
    public void setAppVersion(String appVersion) throws CodePushIllegalArgumentException {
        if (appVersion != null) {
            this.appVersion = appVersion;
        } else {
            throw new CodePushIllegalArgumentException(this.getClass().getName(), "appVersion");
        }
    }

    /**
     * Gets the current local package hash and returns it.
     *
     * @return the current local package hash.
     */
    public String getPackageHash() {
        return packageHash;
    }

    /**
     * Sets the current local package hash.
     *
     * @param packageHash the current local package hash.
     */
    public void setPackageHash(String packageHash) {
        this.packageHash = packageHash;
    }

    /**
     * Gets whether to ignore the application version and returns it.
     *
     * @return whether to ignore the application version.
     */
    public boolean isCompanion() {
        return isCompanion;
    }

    /**
     * Sets whether to ignore the application version.
     *
     * @param companion whether to ignore the application version.
     */
    public void setCompanion(boolean companion) {
        isCompanion = companion;
    }

    /**
     * Gets the current package label and returns it.
     *
     * @return the current package label.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the current package label.
     *
     * @param label the current package label.
     */
    @SuppressWarnings("WeakerAccess")
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Gets device id and returns it.
     *
     * @return device id.
     */
    public String getClientUniqueId() {
        return clientUniqueId;
    }

    /**
     * Sets device id.
     *
     * @param clientUniqueId device id.
     */
    @SuppressWarnings("WeakerAccess")
    public void setClientUniqueId(String clientUniqueId) {
        this.clientUniqueId = clientUniqueId;
    }
}
