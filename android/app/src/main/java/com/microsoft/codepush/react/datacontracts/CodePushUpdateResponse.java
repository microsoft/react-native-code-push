package com.microsoft.codepush.react.datacontracts;

import com.google.gson.annotations.SerializedName;

public class CodePushUpdateResponse {
    @SerializedName("updateInfo")
    public CodePushUpdateResponseUpdateInfo UpdateInfo;
}
