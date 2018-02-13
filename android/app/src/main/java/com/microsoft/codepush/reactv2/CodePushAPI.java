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

    /**
     * Creates react-specific modules.
     *
     * @param reactApplicationContext app context.
     * @return {@link List} of {@link NativeModule} instances.
     */
    List<NativeModule> createNativeModules(ReactApplicationContext reactApplicationContext);

    /* Deprecated in RN v0.47. */
    List<Class<? extends JavaScriptModule>> createJSModules();

    List<ViewManager> createViewManagers(ReactApplicationContext reactApplicationContext);

    /**
     * Gets a link to the default javascript bundle file.
     *
     * @return link starting with "assets://" and leading to javascript bundle file.
     * @throws CodePushNativeApiCallException exception occurred when performing the operation.
     */
    String getJSBundleFile() throws CodePushNativeApiCallException;

    /**
     * Gets a link to the specified javascript bundle file.
     *
     * @param assetsBundleFileName custom bundle file name.
     * @return link starting with "assets://" and leading to javascript bundle file.
     * @throws CodePushNativeApiCallException exception occurred when performing the operation.
     */
    String getJSBundleFile(String assetsBundleFileName) throws CodePushNativeApiCallException;

    /**
     * Gets native CodePush configuration.
     *
     * @return native CodePush configuration.
     */
    CodePushConfiguration getNativeConfiguration();

    /**
     * Gets instance of {@link CodePushAcquisitionManager}.
     *
     * @return instance of {@link CodePushAcquisitionManager}.
     */
    CodePushAcquisitionManager getAcquisitionSdk();

    /**
     * Asks the CodePush service whether the configured app deployment has an update available
     * using deploymentKey already set in constructor.
     *
     * @return remote package info if there is an update, <code>null</code> otherwise.
     * @throws CodePushNativeApiCallException if error occurred during the execution of operation.
     */
    CodePushRemotePackage checkForUpdate() throws CodePushNativeApiCallException;

    /**
     * Asks the CodePush service whether the configured app deployment has an update available
     * using specified deployment key.
     *
     * @param deploymentKey deployment key to use.
     * @return remote package info if there is an update, <code>null</code> otherwise.
     * @throws CodePushNativeApiCallException if error occurred during the execution of operation.
     */
    CodePushRemotePackage checkForUpdate(String deploymentKey) throws CodePushNativeApiCallException;

    /**
     * Gets current installed package.
     *
     * @return current installed package.
     * @throws CodePushNativeApiCallException if error occurred during the execution of operation.
     * @deprecated use {@link #getUpdateMetadata()} instead.
     */
    @Deprecated
    CodePushLocalPackage getCurrentPackage() throws CodePushNativeApiCallException;

    /**
     * Retrieves the metadata for an installed update (e.g. description, mandatory)
     * whose state matches the specified <code>updateState</code> parameter.
     *
     * @param updateState current update state.
     * @return installed update metadata.
     * @throws CodePushNativeApiCallException if error occurred during the operation.
     */
    CodePushLocalPackage getUpdateMetadata(CodePushUpdateState updateState) throws CodePushNativeApiCallException;

    /**
     * Retrieves the metadata for an installed update (e.g. description, mandatory)
     * whose state matches {@link CodePushUpdateState#RUNNING}.
     *
     * @return installed update metadata.
     * @throws CodePushNativeApiCallException if error occurred during the operation.
     */
    CodePushLocalPackage getUpdateMetadata() throws CodePushNativeApiCallException;

    /**
     * Logs custom message on device.
     *
     * @param message message to be logged.
     */
    void log(String message);

    /**
     * Notifies the CodePush runtime that a freshly installed update should be considered successful,
     * and therefore, an automatic client-side rollback isn't necessary.
     *
     * @throws CodePushNativeApiCallException if error occurred during the execution of operation.
     */
    void notifyApplicationReady() throws CodePushNativeApiCallException;

    /**
     * Attempts to restart the application.
     */
    void restartApp();

    /**
     * Attempts to restart the application.
     *
     * @param onlyIfUpdateIsPending if <code>true</code>, restart is performed only if update is pending.
     */
    void restartApp(boolean onlyIfUpdateIsPending);

    /**
     * Permits restarts.
     */
    void disallowRestart();

    /**
     * Allows restarts.
     */
    void allowRestart();

    /**
     * Synchronizes your app assets with the latest release to the configured deployment using default sync options.
     *
     * @throws CodePushNativeApiCallException if error occurred during the execution of operation.
     */
    void sync() throws CodePushNativeApiCallException;

    /**
     * Synchronizes your app assets with the latest release to the configured deployment.
     *
     * @param syncOptions sync options.
     * @throws CodePushNativeApiCallException if error occurred during the execution of operation.
     */
    void sync(CodePushSyncOptions syncOptions) throws CodePushNativeApiCallException;

    /**
     * Adds listener for sync status change event.
     *
     * @param syncStatusListener listener for sync status change event.
     */
    void addSyncStatusListener(CodePushSyncStatusListener syncStatusListener);

    /**
     * Adds listener for download progress change event.
     *
     * @param downloadProgressListener listener for download progress change event.
     */
    void addDownloadProgressListener(CodePushDownloadProgressListener downloadProgressListener);
}