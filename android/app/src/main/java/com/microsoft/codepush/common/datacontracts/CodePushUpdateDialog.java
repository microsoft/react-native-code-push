package com.microsoft.codepush.common.datacontracts;

import com.google.gson.annotations.SerializedName;

/**
 * An "options" object used to determine whether a confirmation dialog should be displayed to the end user when an update is available,
 * and if so, what strings to use. Defaults to null, which has the effect of disabling the dialog completely. Setting this to any truthy
 * value will enable the dialog with the default strings, and passing an object to this parameter allows enabling the dialog as well as
 * overriding one or more of the default strings.
 */
public class CodePushUpdateDialog {

    /**
     * Indicates whether you would like to append the description of an available release to the
     * notification message which is displayed to the end user.
     * Defaults to <code>false</code>.
     */
    @SerializedName("appendReleaseDescription")
    private boolean appendReleaseDescription;

    /**
     * Indicates the string you would like to prefix the release description with, if any, when
     * displaying the update notification to the end user.
     * Defaults to " Description: ".
     */
    @SerializedName("descriptionPrefix")
    private String descriptionPrefix;

    /**
     * The text to use for the button the end user must press in order to install a mandatory update.
     * Defaults to "Continue".
     */
    @SerializedName("mandatoryContinueButtonLabel")
    private String mandatoryContinueButtonLabel;

    /**
     * The text used as the body of an update notification, when the update is specified as mandatory.
     * Defaults to "An update is available that must be installed.".
     */
    @SerializedName("mandatoryUpdateMessage")
    private String mandatoryUpdateMessage;

    /**
     * The text to use for the button the end user can press in order to ignore an optional update that is available.
     * Defaults to "Ignore".
     */
    @SerializedName("optionalIgnoreButtonLabel")
    private String optionalIgnoreButtonLabel;

    /**
     * The text to use for the button the end user can press in order to install an optional update.
     * Defaults to "Install".
     */
    @SerializedName("optionalInstallButtonLabel")
    private String optionalInstallButtonLabel;

    /**
     * The text used as the body of an update notification, when the update is optional.
     * Defaults to "An update is available. Would you like to install it?".
     */
    @SerializedName("optionalUpdateMessage")
    private String optionalUpdateMessage;

    /**
     * The text used as the header of an update notification that is displayed to the end user.
     * Defaults to "Update available".
     */
    @SerializedName("title")
    public String title;

    /**
     * Creates default dialog with default button labels and messages.
     */
    public CodePushUpdateDialog() {
        setDescriptionPrefix("Description: ");
        setMandatoryContinueButtonLabel("Continue");
        setMandatoryUpdateMessage("An update is available that must be installed.");
        setOptionalIgnoreButtonLabel("Ignore");
        setOptionalInstallButtonLabel("Install");
        setOptionalUpdateMessage("An update is available. Would you like to install it?");
        setTitle("Update available");
        setAppendReleaseDescription(false);
    }

    /**
     * Gets whether you would like to append the description of an available release to the
     * notification message which is displayed to the end user and returns it.
     *
     * @return whether you would like to append the description of an available release to the
     * notification message which is displayed to the end user.
     */
    public boolean getAppendReleaseDescription() {
        return appendReleaseDescription;
    }

    /**
     * Sets whether you would like to append the description of an available release to the
     * notification message which is displayed to the end user.
     *
     * @param appendReleaseDescription whether you would like to append the description of an available release to the
     *                                 notification message which is displayed to the end user.
     */
    public void setAppendReleaseDescription(boolean appendReleaseDescription) {
        this.appendReleaseDescription = appendReleaseDescription;
    }

    /**
     * Gets the string you would like to prefix the release description with.
     *
     * @return the string you would like to prefix the release description with.
     */
    public String getDescriptionPrefix() {
        return descriptionPrefix;
    }

    /**
     * Sets the string you would like to prefix the release description with.
     *
     * @param descriptionPrefix the string you would like to prefix the release description with.
     */
    @SuppressWarnings("WeakerAccess")
    public void setDescriptionPrefix(String descriptionPrefix) {
        this.descriptionPrefix = descriptionPrefix;
    }

    /**
     * Gets the text to use for the button the end user must press in order to install a mandatory update and returns it.
     *
     * @return the text to use for the button the end user must press in order to install a mandatory update.
     */
    public String getMandatoryContinueButtonLabel() {
        return mandatoryContinueButtonLabel;
    }

    /**
     * Sets the text to use for the button the end user must press in order to install a mandatory update.
     *
     * @param mandatoryContinueButtonLabel the text to use for the button the end user must press in order to install a mandatory update.
     */
    @SuppressWarnings("WeakerAccess")
    public void setMandatoryContinueButtonLabel(String mandatoryContinueButtonLabel) {
        this.mandatoryContinueButtonLabel = mandatoryContinueButtonLabel;
    }

    /**
     * Gets the text used as the body of an update notification, when the update is specified as mandatory and returns it.
     *
     * @return the text used as the body of an update notification, when the update is specified as mandatory.
     */
    public String getMandatoryUpdateMessage() {
        return mandatoryUpdateMessage;
    }

    /**
     * Sets the text used as the body of an update notification, when the update is specified as mandatory.
     *
     * @param mandatoryUpdateMessage the text used as the body of an update notification, when the update is specified as mandatory.
     */
    @SuppressWarnings("WeakerAccess")
    public void setMandatoryUpdateMessage(String mandatoryUpdateMessage) {
        this.mandatoryUpdateMessage = mandatoryUpdateMessage;
    }

    /**
     * Gets the text to use for the button the end user can press in order to ignore an optional update that is available and returns it.
     *
     * @return the text to use for the button the end user can press in order to ignore an optional update that is available.
     */
    public String getOptionalIgnoreButtonLabel() {
        return optionalIgnoreButtonLabel;
    }

    /**
     * Sets the text to use for the button the end user can press in order to ignore an optional update that is available.
     *
     * @param optionalIgnoreButtonLabel the text to use for the button the end user can press in order to ignore an optional update that is available.
     */
    @SuppressWarnings("WeakerAccess")
    public void setOptionalIgnoreButtonLabel(String optionalIgnoreButtonLabel) {
        this.optionalIgnoreButtonLabel = optionalIgnoreButtonLabel;
    }

    /**
     * Gets the text to use for the button the end user can press in order to install an optional update and returns it.
     *
     * @return the text to use for the button the end user can press in order to install an optional update.
     */
    public String getOptionalInstallButtonLabel() {
        return optionalInstallButtonLabel;
    }

    /**
     * Sets the text to use for the button the end user can press in order to install an optional update.
     *
     * @param optionalInstallButtonLabel the text to use for the button the end user can press in order to install an optional update.
     */
    @SuppressWarnings("WeakerAccess")
    public void setOptionalInstallButtonLabel(String optionalInstallButtonLabel) {
        this.optionalInstallButtonLabel = optionalInstallButtonLabel;
    }

    /**
     * Gets the text used as the body of an update notification, when the update is optional and returns it.
     *
     * @return the text used as the body of an update notification, when the update is optional.
     */
    public String getOptionalUpdateMessage() {
        return optionalUpdateMessage;
    }

    /**
     * Sets the text used as the body of an update notification, when the update is optional.
     *
     * @param optionalUpdateMessage the text used as the body of an update notification, when the update is optional.
     */
    @SuppressWarnings("WeakerAccess")
    public void setOptionalUpdateMessage(String optionalUpdateMessage) {
        this.optionalUpdateMessage = optionalUpdateMessage;
    }

    /**
     * Gets the text used as the header of an update notification that is displayed to the end user.
     *
     * @return the text used as the header of an update notification that is displayed to the end user.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the text used as the header of an update notification that is displayed to the end user.
     *
     * @param title the text used as the header of an update notification that is displayed to the end user.
     */
    public void setTitle(String title) {
        this.title = title;
    }
}
