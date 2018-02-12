package com.microsoft.codepush.common.datacontracts;

import com.google.gson.annotations.SerializedName;
import com.microsoft.codepush.common.exceptions.CodePushIllegalArgumentException;

/**
 * Represents a report sent after downloading package.
 */
public class CodePushDownloadStatusReport {

    /**
     * The id of the device.
     */
    @SerializedName("clientUniqueId")
    private String clientUniqueId;

    /**
     * The deployment key to use to query the CodePush server for an update.
     */
    @SerializedName("deploymentKey")
    private String deploymentKey;

    /**
     * The internal label automatically given to the update by the CodePush server.
     * This value uniquely identifies the update within its deployment.
     */
    @SerializedName("label")
    private String label;

    /**
     * Creates a report using the provided information.
     *
     * @param clientUniqueId id of the device.
     * @param deploymentKey  deployment key to use to query the CodePush server for an update.
     * @param label          internal label automatically given to the update by the CodePush server.
     * @return instance of {@link CodePushDownloadStatusReport}.
     */
    public static CodePushDownloadStatusReport createReport(final String clientUniqueId, final String deploymentKey, final String label) throws CodePushIllegalArgumentException {
        CodePushDownloadStatusReport codePushDownloadStatusReport = new CodePushDownloadStatusReport();
        codePushDownloadStatusReport.setClientUniqueId(clientUniqueId);
        codePushDownloadStatusReport.setDeploymentKey(deploymentKey);
        codePushDownloadStatusReport.setLabel(label);
        return codePushDownloadStatusReport;
    }

    /**
     * Gets the id of the device and returns it.
     *
     * @return the id of the device.
     */
    public String getClientUniqueId() {
        return clientUniqueId;
    }

    /**
     * Gets the deployment key to use to query the CodePush server for an update and returns it.
     *
     * @return the deployment key to use to query the CodePush server for an update.
     */
    public String getDeploymentKey() {
        return deploymentKey;
    }

    /**
     * Gets the internal label automatically given to the update by the CodePush server and returns it.
     *
     * @return the internal label automatically given to the update by the CodePush server.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the internal label automatically given to the update by the CodePush server.
     *
     * @param label the internal label automatically given to the update by the CodePush server.
     */
    public void setLabel(String label) throws CodePushIllegalArgumentException {
        if (label != null) {
            this.label = label;
        } else {
            throw new CodePushIllegalArgumentException(this.getClass().getName(), "label");
        }
    }

    /**
     * Sets the id of the device.
     *
     * @param clientUniqueId the id of the device.
     */
    @SuppressWarnings("WeakerAccess")
    public void setClientUniqueId(String clientUniqueId) throws CodePushIllegalArgumentException {
        if (clientUniqueId != null) {
            this.clientUniqueId = clientUniqueId;
        } else {
            throw new CodePushIllegalArgumentException(this.getClass().getName(), "clientUniqueId");
        }
    }

    /**
     * Sets the deployment key to use to query the CodePush server for an update.
     *
     * @param deploymentKey the deployment key to use to query the CodePush server for an update.
     */
    @SuppressWarnings("WeakerAccess")
    public void setDeploymentKey(String deploymentKey) throws CodePushIllegalArgumentException {
        if (deploymentKey != null) {
            this.deploymentKey = deploymentKey;
        } else {
            throw new CodePushIllegalArgumentException(this.getClass().getName(), "deploymentKey");
        }
    }
}
