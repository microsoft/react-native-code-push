/**
 * Copyright (c) 2015-present, Microsoft
 * All rights reserved.
 */

package com.reactnativecodepush;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import java.util.Properties;

public class CodePushConfig {

    public static final String APP_VERSION_KEY = "appVersion";
    public static final String DEPLOYMENT_KEY_KEY = "deploymentKey";
    public static final String SERVER_URL_KEY = "serverUrl";

    private static String appVersion;
    private static String deploymentKey;
    private static String serverUrl = "https://codepush.azurewebsites.net/";

    public CodePushConfig(String appVersion, String deploymentKey) {
        this.appVersion = appVersion;
        this.deploymentKey = deploymentKey;
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

    public String getAppVersion(){
        return appVersion;
    }

    public WritableMap getConfiguration() {
        WritableMap config = Arguments.createMap();
        config.putString(APP_VERSION_KEY, appVersion);
        config.putString(DEPLOYMENT_KEY_KEY, deploymentKey);
        config.putString(SERVER_URL_KEY, serverUrl);
        return config;
    }

}
