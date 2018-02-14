package com.microsoft.codepush.reactv2;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;
import com.microsoft.codepush.common.CodePushConfiguration;
import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.datacontracts.CodePushRemotePackage;
import com.microsoft.codepush.common.datacontracts.CodePushSyncOptions;
import com.microsoft.codepush.common.enums.CodePushUpdateState;
import com.microsoft.codepush.common.exceptions.CodePushInitializeException;
import com.microsoft.codepush.common.exceptions.CodePushNativeApiCallException;
import com.microsoft.codepush.common.interfaces.CodePushDownloadProgressListener;
import com.microsoft.codepush.common.interfaces.CodePushSyncStatusListener;
import com.microsoft.codepush.common.managers.CodePushAcquisitionManager;
import com.microsoft.codepush.reactv2.interfaces.ReactInstanceHolder;
import com.microsoft.codepush.reactv2.utils.ReactPlatformUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A class exposing CodePush API to users.
 */
public class CodePush implements ReactPackage, Serializable {

    /**
     * Instance of {@link ReactNativeCore}.
     */
    private static ReactNativeCore mReactNativeCore;

    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactApplicationContext) {
        return mReactNativeCore.createNativeModules(reactApplicationContext);
    }

    // Deprecated in RN v0.47.
    public List<Class<? extends JavaScriptModule>> createJSModules() {
        return new ArrayList<>();
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactApplicationContext) {
        return new ArrayList<>();
    }

    /**
     * @deprecated use {@link #builder} instead
     */
    @Deprecated
    public CodePush(String deploymentKey, Context context) throws CodePushInitializeException {
        this(deploymentKey, context, false);
    }

    /**
     * @deprecated use {@link #builder} instead
     */
    @Deprecated
    public CodePush(String deploymentKey, Context context, boolean isDebugMode) throws CodePushInitializeException {
        mReactNativeCore = new ReactNativeCore(
                deploymentKey,
                context,
                isDebugMode,
                null,
                new CodePushReactPublicKeyProvider(null, context),
                new CodePushReactAppEntryPointProvider(null),
                ReactPlatformUtils.getInstance());
    }

    /**
     * @deprecated use {@link #builder} instead
     */
    @Deprecated
    public CodePush(String deploymentKey, Context context, boolean isDebugMode, String serverUrl) throws CodePushInitializeException {
        mReactNativeCore = new ReactNativeCore(
                deploymentKey,
                context,
                isDebugMode,
                serverUrl,
                new CodePushReactPublicKeyProvider(null, context),
                new CodePushReactAppEntryPointProvider(null),
                ReactPlatformUtils.getInstance());
    }

    /**
     * @deprecated use {@link #builder} instead
     */
    @Deprecated
    public CodePush(String deploymentKey, Context context, boolean isDebugMode, int publicKeyResourceDescriptor) throws CodePushInitializeException {
        mReactNativeCore = new ReactNativeCore(
                deploymentKey,
                context,
                isDebugMode,
                null,
                new CodePushReactPublicKeyProvider(publicKeyResourceDescriptor, context),
                new CodePushReactAppEntryPointProvider(null),
                ReactPlatformUtils.getInstance());
    }

    /**
     * @deprecated use {@link #builder} instead
     */
    @Deprecated
    public CodePush(String deploymentKey, Context context, boolean isDebugMode, @NonNull String serverUrl, Integer publicKeyResourceDescriptor) throws CodePushInitializeException {
        mReactNativeCore = new ReactNativeCore(
                deploymentKey,
                context,
                isDebugMode,
                serverUrl,
                new CodePushReactPublicKeyProvider(publicKeyResourceDescriptor, context),
                new CodePushReactAppEntryPointProvider(null),
                ReactPlatformUtils.getInstance());
    }

    /**
     * Creates instance of {@link CodePush}.
     *
     * @param deploymentKey               application deployment key.
     * @param context                     application context.
     * @param isDebugMode                 whether the application is running in debug mode.
     * @param serverUrl                   CodePush server url.
     * @param publicKeyResourceDescriptor public-key related resource descriptor.
     * @param entryPointName              path to the application entry point.
     * @throws CodePushInitializeException initialization exception.
     */
    public CodePush(
            @NonNull String deploymentKey,
            @NonNull Context context,
            boolean isDebugMode,
            @Nullable String serverUrl,
            @Nullable Integer publicKeyResourceDescriptor,
            @Nullable String entryPointName
    ) throws CodePushInitializeException {
        mReactNativeCore = new ReactNativeCore(
                deploymentKey,
                context,
                isDebugMode,
                serverUrl,
                new CodePushReactPublicKeyProvider(publicKeyResourceDescriptor, context),
                new CodePushReactAppEntryPointProvider(entryPointName),
                ReactPlatformUtils.getInstance());
    }

    /**
     * Creates default builder for this class.
     *
     * @param deploymentKey application deployment key.
     * @param context       application context.
     * @return instance of {@link CodePushBuilder}.
     */
    public static CodePushBuilder builder(String deploymentKey, Context context) {
        return new CodePushBuilder(deploymentKey, context);
    }

    public static String getJSBundleFile() throws CodePushNativeApiCallException {
        return mReactNativeCore.getJSBundleFile();
    }

    public static String getJSBundleFile(String assetsBundleFileName) throws CodePushNativeApiCallException {
        return mReactNativeCore.getJSBundleFile(assetsBundleFileName);
    }

    public static void setReactInstanceHolder(ReactInstanceHolder reactInstanceHolder) {
        //todo remove or not?
        mReactNativeCore.setReactInstanceHolder(reactInstanceHolder);
    }

    public CodePushConfiguration getConfiguration() throws CodePushNativeApiCallException {
        return mReactNativeCore.getNativeConfiguration();
    }

    public CodePushAcquisitionManager getAcquisitionSdk() {
        return mReactNativeCore.getAcquisitionSdk();
    }

    public CodePushRemotePackage checkForUpdate() throws CodePushNativeApiCallException {
        return mReactNativeCore.checkForUpdate();
    }

    public CodePushRemotePackage checkForUpdate(String deploymentKey) throws CodePushNativeApiCallException {
        return mReactNativeCore.checkForUpdate(deploymentKey);
    }

    /**
     * @deprecated use {@link #getUpdateMetadata()} instead.
     */
    @Deprecated
    public CodePushLocalPackage getCurrentPackage() throws CodePushNativeApiCallException {
        return mReactNativeCore.getCurrentPackage();
    }

    public CodePushLocalPackage getUpdateMetadata(CodePushUpdateState updateState) throws CodePushNativeApiCallException {
        return mReactNativeCore.getUpdateMetadata(updateState);
    }

    public CodePushLocalPackage getUpdateMetadata() throws CodePushNativeApiCallException {
        return mReactNativeCore.getUpdateMetadata(CodePushUpdateState.RUNNING);
    }

    public static void log(String message) {
        mReactNativeCore.log(message);
    }

    public void notifyApplicationReady() throws CodePushNativeApiCallException {
        mReactNativeCore.notifyApplicationReady();
    }

    public void restartApp() throws CodePushNativeApiCallException {
        mReactNativeCore.restartApp(false);
    }

    public void restartApp(boolean onlyIfUpdateIsPending) throws CodePushNativeApiCallException {
        mReactNativeCore.restartApp(onlyIfUpdateIsPending);
    }

    public void disallowRestart() {
        mReactNativeCore.disallowRestart();
    }

    public void allowRestart() throws CodePushNativeApiCallException {
        mReactNativeCore.allowRestart();
    }

    public void sync() throws CodePushNativeApiCallException {
        mReactNativeCore.sync();
    }

    public void sync(CodePushSyncOptions syncOptions) throws CodePushNativeApiCallException {
        mReactNativeCore.sync(syncOptions);
    }

    public void addSyncStatusListener(CodePushSyncStatusListener syncStatusListener) {
        mReactNativeCore.addSyncStatusListener(syncStatusListener);
    }

    public void addDownloadProgressListener(CodePushDownloadProgressListener downloadProgressListener) {
        mReactNativeCore.addDownloadProgressListener(downloadProgressListener);
    }
}