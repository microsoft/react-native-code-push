package com.microsoft.codepush.react;

import android.content.Context;
import android.support.annotation.NonNull;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;
import com.microsoft.codepush.react.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.react.datacontracts.CodePushRemotePackage;
import com.microsoft.codepush.react.datacontracts.CodePushSyncOptions;
import com.microsoft.codepush.react.enums.CodePushUpdateState;
import com.microsoft.codepush.react.interfaces.CodePushDownloadProgressListener;
import com.microsoft.codepush.react.interfaces.CodePushSyncStatusListener;
import com.microsoft.codepush.react.interfaces.ReactInstanceHolder;
import com.microsoft.codepush.react.managers.CodePushAcquisitionManager;
import com.microsoft.codepush.react.utils.CodePushUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CodePush implements ReactPackage, Serializable{

    private static CodePushCore mCodePushCore;

    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactApplicationContext) {
        return mCodePushCore.createNativeModules(reactApplicationContext);
    }

    // Deprecated in RN v0.47.
    public List<Class<? extends JavaScriptModule>> createJSModules() {
        return new ArrayList<>();
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactApplicationContext) {
        return new ArrayList<>();
    }

    public CodePush(String deploymentKey, Context context) {
        this(deploymentKey, context, false);
    }

    public CodePush(String deploymentKey, Context context, boolean isDebugMode) {
        mCodePushCore = new CodePushCore(deploymentKey, context, isDebugMode);
    }

    public CodePush(String deploymentKey, Context context, boolean isDebugMode, String serverUrl) {
        mCodePushCore = new CodePushCore(deploymentKey, context, isDebugMode, serverUrl);
    }

    public CodePush(String deploymentKey, Context context, boolean isDebugMode, int publicKeyResourceDescriptor) {
        mCodePushCore = new CodePushCore(deploymentKey, context, isDebugMode, publicKeyResourceDescriptor);
    }

    public CodePush(String deploymentKey, Context context, boolean isDebugMode, @NonNull String serverUrl, Integer publicKeyResourceDescriptor) {
        mCodePushCore = new CodePushCore(deploymentKey, context, isDebugMode, serverUrl, publicKeyResourceDescriptor);
    }

    public static String getJSBundleFile() {
        return CodePushCore.getJSBundleFile();
    }

    public static String getJSBundleFile(String assetsBundleFileName) {
        return CodePushCore.getJSBundleFile(assetsBundleFileName);
    }

    public static void setReactInstanceHolder(ReactInstanceHolder reactInstanceHolder) {
        //todo remove or not?
        mCodePushCore.setReactInstanceHolder(reactInstanceHolder);
    }

    public CodePushConfiguration getConfiguration() {
        return mCodePushCore.getConfiguration();
    }

    public CodePushAcquisitionManager getAcquisitionSdk() {
        return new CodePushAcquisitionManager(getConfiguration());
    }

    public CodePushAcquisitionManager getAcquisitionSdk(CodePushConfiguration configuration) {
        return new CodePushAcquisitionManager(configuration);
    }

    public CodePushRemotePackage checkForUpdate() {
        return mCodePushCore.checkForUpdate();
    }

    public CodePushRemotePackage checkForUpdate(String deploymentKey) {
        return mCodePushCore.checkForUpdate(deploymentKey);
    }

    /**
     * @deprecated use {@link #getUpdateMetadata()} instead.
     */
    @Deprecated
    public CodePushLocalPackage getCurrentPackage() {
        return mCodePushCore.getCurrentPackage();
    }

    public CodePushLocalPackage getUpdateMetadata(CodePushUpdateState updateState) {
        return mCodePushCore.getUpdateMetadata(updateState);
    }

    public CodePushLocalPackage getUpdateMetadata() {
        return mCodePushCore.getUpdateMetadata(CodePushUpdateState.RUNNING);
    }

    public static void log(String message) {
        CodePushUtils.log(message);
    }

    public void notifyApplicationReady() {
        mCodePushCore.notifyApplicationReady();
    }

    public void restartApp() {
        mCodePushCore.getRestartManager().restartApp(false);
    }

    public void restartApp(boolean onlyIfUpdateIsPending) {
        mCodePushCore.getRestartManager().restartApp(onlyIfUpdateIsPending);
    }

    public void disallowRestart() {
        mCodePushCore.getRestartManager().disallow();
    }

    public void allowRestart() {
        mCodePushCore.getRestartManager().allow();
    }

    public void sync() {
        mCodePushCore.sync();
    }

    public void sync(CodePushSyncOptions syncOptions) {
        mCodePushCore.sync(syncOptions);
    }

    public void addSyncStatusListener(CodePushSyncStatusListener syncStatusListener) {
        mCodePushCore.addSyncStatusListener(syncStatusListener);
    }

    public void addDownloadProgressListener(CodePushDownloadProgressListener downloadProgressListener) {
        mCodePushCore.addDownloadProgressListener(downloadProgressListener);
    }
}