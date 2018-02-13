package com.microsoft.codepush.common.datacontracts;

import com.google.gson.annotations.SerializedName;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.codepush.common.CodePush;
import com.microsoft.codepush.common.enums.CodePushDeploymentStatus;

/**
 * Represents a report about the deployment.
 */
public class CodePushDeploymentStatusReport extends CodePushDownloadStatusReport {

    /**
     * The version of the app that was deployed (for a native app upgrade).
     */
    @SerializedName("appVersion")
    private String appVersion;

    /**
     * Deployment key used when deploying the previous package.
     */
    @SerializedName("previousDeploymentKey")
    private String previousDeploymentKey;

    /**
     * The label (v#) of the package that was upgraded from.
     */
    @SerializedName("previousLabelOrAppVersion")
    private String previousLabelOrAppVersion;

    /**
     * Whether the deployment succeeded or failed.
     */
    @SerializedName("status")
    private CodePushDeploymentStatus status;

    @SerializedName("package")
    private transient CodePushPackage aPackage;

    /**
     * Gets the version of the app that was deployed and returns it.
     *
     * @return the version of the app that was deployed.
     */
    public String getAppVersion() {
        return appVersion;
    }

    /**
     * Sets the version of the app that was deployed.
     *
     * @param appVersion the version of the app that was deployed.
     */
    public void setAppVersion(String appVersion) {
        if (appVersion != null) {
            this.appVersion = appVersion;
        } else {
            AppCenterLog.error(CodePush.LOG_TAG, "\"appVersion\" property cannot be null.");
        }
    }

    /**
     * Gets deployment key used when deploying the previous package and returns it.
     *
     * @return deployment key used when deploying the previous package.
     */
    public String getPreviousDeploymentKey() {
        return previousDeploymentKey;
    }

    /**
     * Sets deployment key used when deploying the previous package.
     *
     * @param previousDeploymentKey deployment key used when deploying the previous package.
     */
    public void setPreviousDeploymentKey(String previousDeploymentKey) {
        if (previousDeploymentKey != null) {
            this.previousDeploymentKey = previousDeploymentKey;
        } else {
            AppCenterLog.error(CodePush.LOG_TAG, "\"previousDeploymentKey\" property cannot be null.");
        }
    }

    /**
     * Gets the label (v#) of the package that was upgraded from and returns it.
     *
     * @return the label (v#) of the package that was upgraded from.
     */
    public String getPreviousLabelOrAppVersion() {
        return previousLabelOrAppVersion;
    }

    /**
     * Sets the label (v#) of the package that was upgraded from.
     *
     * @param previousLabelOrAppVersion the label (v#) of the package that was upgraded from.
     */
    public void setPreviousLabelOrAppVersion(String previousLabelOrAppVersion) {
        this.previousLabelOrAppVersion = previousLabelOrAppVersion;
    }

    /**
     * Gets whether the deployment succeeded or failed and returns it.
     *
     * @return whether the deployment succeeded or failed.
     */
    public CodePushDeploymentStatus getStatus() {
        return status;
    }

    /**
     * Sets whether the deployment succeeded or failed.
     *
     * @param status whether the deployment succeeded or failed.
     */
    public void setStatus(CodePushDeploymentStatus status) {
        this.status = status;
    }

    /**
     * Sets local installed/failed package, (will not be serialized).
     *
     * @return local installed package.
     */
    public CodePushPackage getPackage() {
        return aPackage;
    }

    /**
     * Gets local installed/failed package, (will not be serialized).
     *
     * @param aPackage local installed/failed package, (will not be serialized).
     */
    public void setPackage(CodePushPackage aPackage) {
        this.aPackage = aPackage;
    }
}
