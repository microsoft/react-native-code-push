/**
 * Copyright (c) 2015-present, Microsoft
 * All rights reserved.
 */

package com.reactnativecodepush;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import java.util.Properties;

public class CodePushConfig {

    WritableMap config;
    Properties configProperties;

    public static final String APP_VERSION_KEY = "appVersion";
    public static final String BUILD_VERSION_KEY = "buildVersion";
    public static final String DEPLOYMENT_KEY_KEY = "deploymentKey";
    public static final String SERVER_URL_KEY = "serverUrl";

    public CodePushConfig(Properties configProperties) {
        this.configProperties = configProperties;
        config = Arguments.createMap();
        config.putString(APP_VERSION_KEY, configProperties.getProperty(APP_VERSION_KEY));
        config.putString(BUILD_VERSION_KEY, configProperties.getProperty(BUILD_VERSION_KEY));
        config.putString(DEPLOYMENT_KEY_KEY, configProperties.getProperty(DEPLOYMENT_KEY_KEY));
        config.putString(SERVER_URL_KEY,
                configProperties.getProperty(SERVER_URL_KEY) == null
                        ? "http://localhost:3000/"
                        : configProperties.getProperty(SERVER_URL_KEY));
    }

    public void setDeploymentKey(String deploymentKey) {
        config.putString(DEPLOYMENT_KEY_KEY, deploymentKey);
    }

    public String getDeploymentKey() {
        return config.getString(DEPLOYMENT_KEY_KEY);
    }

    public void setServerUrl(String serverUrl) {
        config.putString(SERVER_URL_KEY, serverUrl);
    }

    public String getServerUrl() {
        return config.getString(SERVER_URL_KEY);
    }

    public void setAppVersion(String appVersion) {
        config.putString(APP_VERSION_KEY, appVersion);
    }

    public String getAppVersion(){
        return config.getString(APP_VERSION_KEY);
    }

    public void setBuildVersion(String buildVersion) {
        config.putString(BUILD_VERSION_KEY, buildVersion);
    }

    public String getBuildVersion() {
        return config.getString(BUILD_VERSION_KEY);
    }

    public WritableMap getConfiguration() {
        config = Arguments.createMap();
        config.putString(APP_VERSION_KEY, configProperties.getProperty(APP_VERSION_KEY));
        config.putString(BUILD_VERSION_KEY, configProperties.getProperty(BUILD_VERSION_KEY));
        config.putString(DEPLOYMENT_KEY_KEY, configProperties.getProperty(DEPLOYMENT_KEY_KEY));
        config.putString(SERVER_URL_KEY,
                configProperties.getProperty(SERVER_URL_KEY) == null
                        ? "http://localhost:3000/"
                        : configProperties.getProperty(SERVER_URL_KEY));
        return config;
    }

}
