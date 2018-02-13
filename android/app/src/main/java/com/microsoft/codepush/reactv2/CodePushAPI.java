package com.microsoft.codepush.reactv2;

import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;
import com.microsoft.codepush.common.CodePushBaseCore;
import com.microsoft.codepush.common.CodePushConfiguration;
import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.datacontracts.CodePushRemotePackage;
import com.microsoft.codepush.common.datacontracts.CodePushSyncOptions;
import com.microsoft.codepush.common.enums.CodePushUpdateState;
import com.microsoft.codepush.common.interfaces.CodePushDownloadProgressListener;
import com.microsoft.codepush.common.interfaces.CodePushSyncStatusListener;
import com.microsoft.codepush.common.managers.CodePushAcquisitionManager;

import java.util.List;

/**
 * Interface for users to interact with {@link CodePushBaseCore}.
 */
public interface CodePushAPI {

    List<NativeModule> createNativeModules(ReactApplicationContext reactApplicationContext); //TODO: return new ArrayList<>();

    /* Deprecated in RN v0.47. */
    List<Class<? extends JavaScriptModule>> createJSModules(); //TODO: return new ArrayList<>();

    List<ViewManager> createViewManagers(ReactApplicationContext reactApplicationContext);

    String getJSBundleFile();

    String getJSBundleFile(String assetsBundleFileName);

    CodePushConfiguration getConfiguration();

    CodePushAcquisitionManager getAcquisitionSdk(); //TODO:  new CodePushAcquisitionManager(getConfiguration());

    CodePushAcquisitionManager getAcquisitionSdk(CodePushConfiguration configuration); //TODO:  new CodePushAcquisitionManager(configuration);

    CodePushRemotePackage checkForUpdate();

    CodePushRemotePackage checkForUpdate(String deploymentKey);

    /**
     * @deprecated use {@link #getUpdateMetadata()} instead.
     */
    @Deprecated
    CodePushLocalPackage getCurrentPackage();

    CodePushLocalPackage getUpdateMetadata(CodePushUpdateState updateState);

    CodePushLocalPackage getUpdateMetadata(); //TODO: mCodePushCore.getUpdateMetadata(CodePushUpdateState.RUNNING);

    void log(String message); //TODO: implement.

    void notifyApplicationReady();

    void restartApp(); //TODO: mReactNativeCore.getRestartManager().restartApp();

    void restartApp(boolean onlyIfUpdateIsPending); //TODO: mReactNativeCore.getRestartManager().restartApp(onlyIfUpdateIsPending);

    void disallowRestart(); //TODO: mReactNativeCore.getRestartManager().disallow();

    void allowRestart(); //TODO: mReactNativeCore.getRestartManager().allow();

    void sync();

    void sync(CodePushSyncOptions syncOptions);

    void addSyncStatusListener(CodePushSyncStatusListener syncStatusListener);

    void addDownloadProgressListener(CodePushDownloadProgressListener downloadProgressListener);
}