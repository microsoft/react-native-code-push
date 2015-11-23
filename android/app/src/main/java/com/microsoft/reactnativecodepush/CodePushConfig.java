package com.microsoft.reactnativecodepush;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableNativeMap;

public class CodePushConfig {

    private String appVersion;
    private int buildVersion;
    private String deploymentKey;
    private String serverUrl = "https://codepush.azurewebsites.net/";

    public CodePushConfig(String deploymentKey, Context applicationContext) {
        this.deploymentKey = deploymentKey;
        PackageInfo pInfo = null;
        try {
            pInfo = applicationContext.getPackageManager().getPackageInfo(applicationContext.getPackageName(), 0);
            appVersion = pInfo.versionName;
            buildVersion = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new CodePushUnknownException("Unable to get package info for " + applicationContext.getPackageName(), e);
        }
    }

    public void setDeploymentKey(String deploymentKey) {
        this.deploymentKey = deploymentKey;
    }

    public String getDeploymentKey() {
        return deploymentKey;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setBuildVersion(int buildVersion) {
        this.buildVersion = buildVersion;
    }

    public int getBuildVersion() {
        return buildVersion;
    }

    public ReadableMap getConfiguration() {
        WritableNativeMap configMap = new WritableNativeMap();
        configMap.putString("appVersion", appVersion);
        configMap.putInt("buildVersion", buildVersion);
        configMap.putString("deploymentKey", deploymentKey);
        configMap.putString("serverUrl", serverUrl);
        return configMap;
    }
}
