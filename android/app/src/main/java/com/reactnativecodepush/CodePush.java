/**
 * Copyright (c) 2015-present, Microsoft
 * All rights reserved.
 */

package com.reactnativecodepush;

import android.app.Activity;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.shell.MainReactPackage;
import com.facebook.react.uimanager.ViewManager;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


public class CodePush extends ReactContextBaseJavaModule {

    private CodePushReactInstanceManager codePushReactInstanceManager = null;
    private CodePushReactRootView codePushReactRootView = null;
    private CodePushConfig config = null;
    private Activity mainActivity;

    public CodePush(ReactApplicationContext reactContext) {
        super(reactContext);
    }
    public CodePush(ReactApplicationContext reactContext, Activity mainActivity) {
        super(reactContext);
        this.mainActivity = mainActivity;
    }

    private void setInstanceManager(CodePushReactInstanceManager codePushReactInstanceManager) {
        this.codePushReactInstanceManager = codePushReactInstanceManager;
    }

    @Override
    public String getName() {
        return "CodePush";
    }

    @ReactMethod
    public void getConfiguration(Callback successCallback) {
        successCallback.invoke(config.getConfiguration());
    }

    @ReactMethod
    public void getCurrentPackage(Callback successCallback, Callback errorCallback) {
        try {
            if(new File(CodePushPackage.getPackagePath()).exists())
                successCallback.invoke(CodePushPackage.getCurrentPackageInfo());
            else
                successCallback.invoke(false);
        } catch (JSONException e) {
            errorCallback.invoke(e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            errorCallback.invoke(e.getMessage());
            e.printStackTrace();
        }
    }

    @ReactMethod
    public void applyUpdate() {
        mainActivity.finish();
        mainActivity.startActivity(mainActivity.getIntent());
    }

    @ReactMethod
    public void downloadUpdate(ReadableMap updatePackage, Callback successCallback, Callback errorCallback) {
        try {
            CodePushPackage.downloadPackage(updatePackage);
            successCallback.invoke();
        } catch (JSONException e) {
            errorCallback.invoke(e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            errorCallback.invoke(e.getMessage());
            e.printStackTrace();
        }
    }

    public static class CodePushReactPackage extends MainReactPackage {

        private CodePush codepush;
        private Activity mainActivity;
        private String appVersion;
        private String deploymentKey;

        public boolean hasSavedBundle(){
            return new File(getBundleLocation()).exists();
        }

        public String getBundleLocation(){
            return CodePushPackage.getBundlePath();
        }

        public void setInstanceManager(CodePushReactInstanceManager codePushReactInstanceManager) {
            codepush.codePushReactInstanceManager = codePushReactInstanceManager;
        }

        public void setRootView(CodePushReactRootView codePushReactRootView) {
            codepush.codePushReactRootView = codePushReactRootView;
        }

        public CodePushReactPackage(String appVersion, String deploymentKey, Activity mainActivity){
            super();
            this.appVersion = appVersion;
            this.deploymentKey = deploymentKey;
            this.mainActivity = mainActivity;
        }

        @Override
        public List<NativeModule> createNativeModules(
                ReactApplicationContext reactContext) {

            codepush = new CodePush(reactContext, mainActivity);
            codepush.config = new CodePushConfig(appVersion, deploymentKey);

            List<NativeModule> modules = new ArrayList<>();
            modules.addAll(super.createNativeModules(reactContext));
            modules.add(codepush);

            return modules;
        }

        @Override
        public List<Class<? extends JavaScriptModule>> createJSModules() {
            return super.createJSModules();
        }

        @Override
        public List<ViewManager> createViewManagers(ReactApplicationContext reactApplicationContext) {
            return super.createViewManagers(reactApplicationContext);
        }
    }

}
