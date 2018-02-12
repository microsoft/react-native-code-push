package com.microsoft.codepush.react.datacontracts;

import com.google.gson.annotations.SerializedName;

public class CodePushUpdateDialog {
    @SerializedName("appendReleaseDescription")
    public Boolean AppendReleaseDescription = false;

    @SerializedName("descriptionPrefix")
    public String DescriptionPrefix = " Description: ";

    @SerializedName("mandatoryContinueButtonLabel")
    public String MandatoryContinueButtonLabel = "Continue";

    @SerializedName("mandatoryUpdateMessage")
    public String MandatoryUpdateMessage = "An update is available that must be installed.";

    @SerializedName("optionalIgnoreButtonLabel")
    public String OptionalIgnoreButtonLabel = "Ignore";

    @SerializedName("optionalInstallButtonLabel")
    public String OptionalInstallButtonLabel = "Install";

    @SerializedName("optionalUpdateMessage")
    public String OptionalUpdateMessage = "An update is available. Would you like to install it?";

    @SerializedName("title")
    public String Title = "Update available";
}
