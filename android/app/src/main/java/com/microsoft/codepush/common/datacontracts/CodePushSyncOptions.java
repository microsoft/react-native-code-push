package com.microsoft.codepush.common.datacontracts;

import com.google.gson.annotations.SerializedName;
import com.microsoft.codepush.common.enums.CodePushCheckFrequency;
import com.microsoft.codepush.common.enums.CodePushInstallMode;

/**
 * Contains synchronization options.
 */
public class CodePushSyncOptions {

    /**
     * Specifies the deployment key you want to query for an update against.
     * By default, this value is derived from the MainActivity.java file (Android),
     * but this option allows you to override it from the script-side if you need to
     * dynamically use a different deployment for a specific call to sync.
     */
    @SerializedName("deploymentKey")
    private String deploymentKey;

    /**
     * Specifies when you would like to install optional updates (i.e. those that aren't marked as mandatory).
     * Defaults to {@link CodePushInstallMode#ON_NEXT_RESTART}.
     */
    @SerializedName("installMode")
    private CodePushInstallMode installMode;

    /**
     * Specifies when you would like to install updates which are marked as mandatory.
     * Defaults to {@link CodePushInstallMode#IMMEDIATE}.
     */
    @SerializedName("mandatoryInstallMode")
    private CodePushInstallMode mandatoryInstallMode;

    /**
     * Specifies the minimum number of seconds that the app needs to have been in the background before restarting the app.
     * This property only applies to updates which are installed using {@link CodePushInstallMode#ON_NEXT_RESUME},
     * and can be useful for getting your update in front of end users sooner, without being too obtrusive.
     * Defaults to `0`, which has the effect of applying the update immediately after a resume, regardless
     * how long it was in the background.
     */
    @SerializedName("minimumBackgroundDuration")
    private int minimumBackgroundDuration;

    /**
     * Specifies whether to ignore failed updates.
     * Defaults to <code>true</code>.
     */
    private boolean ignoreFailedUpdates = true;

    /**
     * An "options" object used to determine whether a confirmation dialog should be displayed to the end user when an update is available,
     * and if so, what strings to use. Defaults to null, which has the effect of disabling the dialog completely.
     * Setting this to any truthy value will enable the dialog with the default strings, and passing an object to this parameter allows
     * enabling the dialog as well as overriding one or more of the default strings.
     */
    @SerializedName("updateDialog")
    private CodePushUpdateDialog updateDialog;

    /**
     * Specifies when you would like to synchronize updates with the CodePush server.
     * Defaults to {@link CodePushCheckFrequency#ON_APP_START}.
     */
    @SerializedName("checkFrequency")
    private CodePushCheckFrequency checkFrequency;

    /**
     * Creates default instance of sync options.
     *
     * @param deploymentKey the deployment key you want to query for an update against.
     */
    public CodePushSyncOptions(String deploymentKey) {
        setDeploymentKey(deploymentKey);
        setInstallMode(CodePushInstallMode.ON_NEXT_RESTART);
        setMandatoryInstallMode(CodePushInstallMode.IMMEDIATE);
        setMinimumBackgroundDuration(0);
        setIgnoreFailedUpdates(true);
        setCheckFrequency(CodePushCheckFrequency.ON_APP_START);
    }

    /**
     * Creates default instance of sync options.
     */
    public CodePushSyncOptions() {
        this(null);
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
    public void setDeploymentKey(String deploymentKey) {
        this.deploymentKey = deploymentKey;
    }

    /**
     * Gets when you would like to install optional updates.
     *
     * @return when you would like to install optional updates.
     */
    public CodePushInstallMode getInstallMode() {
        return installMode;
    }

    /**
     * Sets when you would like to install optional updates.
     *
     * @param installMode when you would like to install optional updates.
     */
    @SuppressWarnings("WeakerAccess")
    public void setInstallMode(CodePushInstallMode installMode) {
        this.installMode = installMode;
    }

    /**
     * Gets specifies when you would like to install updates which are marked as mandatory and returns it.
     *
     * @return specifies when you would like to install updates which are marked as mandatory.
     */
    public CodePushInstallMode getMandatoryInstallMode() {
        return mandatoryInstallMode;
    }

    /**
     * Sets specifies when you would like to install updates which are marked as mandatory.
     *
     * @param mandatoryInstallMode specifies when you would like to install updates which are marked as mandatory.
     */
    @SuppressWarnings("WeakerAccess")
    public void setMandatoryInstallMode(CodePushInstallMode mandatoryInstallMode) {
        this.mandatoryInstallMode = mandatoryInstallMode;
    }

    /**
     * Gets the minimum number of seconds that the app needs to have been in the background before restarting the app.
     *
     * @return the minimum number of seconds that the app needs to have been in the background before restarting the app.
     */
    public int getMinimumBackgroundDuration() {
        return minimumBackgroundDuration;
    }

    /**
     * Sets the minimum number of seconds that the app needs to have been in the background before restarting the app.
     *
     * @param minimumBackgroundDuration the minimum number of seconds that the app needs to have been in the background before restarting the app.
     */
    @SuppressWarnings("WeakerAccess")
    public void setMinimumBackgroundDuration(int minimumBackgroundDuration) {
        this.minimumBackgroundDuration = minimumBackgroundDuration;
    }

    /**
     * Gets whether to ignore failed updates and returns it.
     *
     * @return whether to ignore failed updates.
     */
    public boolean getIgnoreFailedUpdates() {
        return ignoreFailedUpdates;
    }

    /**
     * Sets whether to ignore failed updates.
     *
     * @param ignoreFailedUpdates whether to ignore failed updates.
     */
    public void setIgnoreFailedUpdates(boolean ignoreFailedUpdates) {
        this.ignoreFailedUpdates = ignoreFailedUpdates;
    }

    /**
     * Gets whether to ignore failed updates and returns it.
     *
     * @return whether to ignore failed updates.
     */
    public CodePushUpdateDialog getUpdateDialog() {
        return updateDialog;
    }

    /**
     * Sets whether to ignore failed updates.
     *
     * @param updateDialog whether to ignore failed updates.
     */
    public void setUpdateDialog(CodePushUpdateDialog updateDialog) {
        this.updateDialog = updateDialog;
    }

    /**
     * Gets when you would like to synchronize updates with the CodePush server and returns it.
     *
     * @return when you would like to synchronize updates with the CodePush server.
     */
    public CodePushCheckFrequency getCheckFrequency() {
        return checkFrequency;
    }

    /**
     * Sets when you would like to synchronize updates with the CodePush server.
     *
     * @param checkFrequency when you would like to synchronize updates with the CodePush server.
     */
    public void setCheckFrequency(CodePushCheckFrequency checkFrequency) {
        this.checkFrequency = checkFrequency;
    }
}
