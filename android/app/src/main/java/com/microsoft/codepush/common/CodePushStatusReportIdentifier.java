package com.microsoft.codepush.common;

import android.support.annotation.NonNull;

/**
 * Identifier for status report saved on users device.
 * Basically it is used in {@link com.microsoft.codepush.react.managers.CodePushTelemetryManager}
 * for identifying status reports saved on users device. Identifier consist of two parts: deployment key and version label.
 * If both are exist it means that update report was saved, if only version label exists it means that binary report was saved.
 * There shouldn't be a situation when version label doesn't exist.
 * <p>
 * Identifier can be serialized into string for saving on device and deserialized from it.
 */
public class CodePushStatusReportIdentifier {

    /**
     * Deployment key.
     */
    private String deploymentKey;

    /**
     * Version label.
     */
    private String versionLabel;

    /**
     * Creates an instance of {@link CodePushStatusReportIdentifier}.
     *
     * @param versionLabel version label.
     */
    public CodePushStatusReportIdentifier(@NonNull String versionLabel) {
        this.versionLabel = versionLabel;
    }

    /**
     * Creates an instance of {@link CodePushStatusReportIdentifier}.
     *
     * @param deploymentKey deployment key.
     * @param versionLabel  version label.
     */
    public CodePushStatusReportIdentifier(@NonNull String deploymentKey, @NonNull String versionLabel) {
        this.deploymentKey = deploymentKey;
        this.versionLabel = versionLabel;
    }

    /**
     * Deserializes identifier from string.
     *
     * @param stringIdentifier input string.
     * @return {@link CodePushStatusReportIdentifier} instance if it could be parsed, <code>null</code> otherwise.
     */
    public static CodePushStatusReportIdentifier fromString(String stringIdentifier) {
        String[] parsedIdentifier = stringIdentifier.split(":");
        if (parsedIdentifier.length == 1) {
            String versionLabel = parsedIdentifier[0];
            return new CodePushStatusReportIdentifier(versionLabel);
        } else if (parsedIdentifier.length == 2) {
            String versionLabel = parsedIdentifier[0];
            String deploymentKey = parsedIdentifier[1];
            return new CodePushStatusReportIdentifier(versionLabel, deploymentKey);
        } else {
            return null; 
        }
    }

    /**
     * Serializes identifier to string.
     *
     * @return serialized identifier.
     */
    @Override
    public String toString() {
        if (versionLabel != null) {
            if (deploymentKey == null) {
                return versionLabel;
            } else {
                return deploymentKey + ":" + versionLabel;
            }
        } else {
            return null;
        }
    }

    /**
     * Indicates whether identifier has deployment key.
     *
     * @return <code>true</code> if identifier has deployment key, <code>false</code> otherwise.
     */
    public boolean hasDeploymentKey() {
        return deploymentKey != null;
    }

    /**
     * Gets deployment key.
     *
     * @return deployment key.
     */
    public String getDeploymentKey() {
        return deploymentKey;
    }

    /**
     * Gets version label.
     *
     * @return version label.
     */
    public String getVersionLabel() {
        return versionLabel;
    }

    /**
     * Gets version label or empty string (<code>""</code>) if it equals null.
     *
     * @return version label or empty string (<code>""</code>) if it equals null
     */
    public String getVersionLabelOrEmpty() {
        return versionLabel == null ? "" : versionLabel;
    }
}
