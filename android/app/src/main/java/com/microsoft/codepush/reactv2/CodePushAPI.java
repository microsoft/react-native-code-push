package com.microsoft.codepush.reactv2;

import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;
import com.microsoft.codepush.common.CodePushConfiguration;
import com.microsoft.codepush.common.core.CodePushBaseCore;
import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.datacontracts.CodePushRemotePackage;
import com.microsoft.codepush.common.datacontracts.CodePushSyncOptions;
import com.microsoft.codepush.common.enums.CodePushUpdateState;
import com.microsoft.codepush.common.exceptions.CodePushNativeApiCallException;
import com.microsoft.codepush.common.interfaces.CodePushDownloadProgressListener;
import com.microsoft.codepush.common.interfaces.CodePushSyncStatusListener;
import com.microsoft.codepush.common.managers.CodePushAcquisitionManager;

import java.util.List;

/**
 * Interface for users to interact with {@link CodePushBaseCore}.
 */
public interface CodePushAPI {

    List<NativeModule> createNativeModules(ReactApplicationContext reactApplicationContext);

    /* Deprecated in RN v0.47. */
    List<Class<? extends JavaScriptModule>> createJSModules();

    List<ViewManager> createViewManagers(ReactApplicationContext reactApplicationContext);

    String getJSBundleFile() throws CodePushNativeApiCallException;

    String getJSBundleFile(String assetsBundleFileName) throws CodePushNativeApiCallException;

    CodePushConfiguration getNativeConfiguration();

    CodePushAcquisitionManager getAcquisitionSdk();

    CodePushRemotePackage checkForUpdate() throws CodePushNativeApiCallException;

    CodePushRemotePackage checkForUpdate(String deploymentKey) throws CodePushNativeApiCallException;

    /**
     * @deprecated use {@link #getUpdateMetadata()} instead.
     */
    @Deprecated
    CodePushLocalPackage getCurrentPackage() throws CodePushNativeApiCallException;

    CodePushLocalPackage getUpdateMetadata(CodePushUpdateState updateState) throws CodePushNativeApiCallException;

    CodePushLocalPackage getUpdateMetadata() throws CodePushNativeApiCallException;

    void log(String message);

    void notifyApplicationReady() throws CodePushNativeApiCallException;

    void restartApp();

    void restartApp(boolean onlyIfUpdateIsPending);

    void disallowRestart();

    void allowRestart();

    void sync() throws CodePushNativeApiCallException;

    void sync(CodePushSyncOptions syncOptions) throws CodePushNativeApiCallException;

    void addSyncStatusListener(CodePushSyncStatusListener syncStatusListener);

    void addDownloadProgressListener(CodePushDownloadProgressListener downloadProgressListener);
}