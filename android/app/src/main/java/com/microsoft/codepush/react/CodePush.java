package com.microsoft.codepush.react;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.soloader.SoLoader;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CodePush implements ReactPackage {

    private static boolean needToReportRollback = false;
    private static boolean isRunningBinaryVersion = false;
    private static boolean testConfigurationFlag = false;

    private boolean didUpdate = false;

    private String assetsBundleFileName;

    // Helper classes.
    private CodePushNativeModule codePushNativeModule;
    private CodePushPackage codePushPackage;
    private CodePushTelemetryManager codePushTelemetryManager;
    private SettingsManager settingsManager;

    // Config properties.
    private String appVersion;
    private int buildVersion;
    private String deploymentKey;
    private final String serverUrl = "https://codepush.azurewebsites.net/";

    private Context context;
    private final boolean isDebugMode;

    private static CodePush currentInstance;

    public CodePush(String deploymentKey, Context context) {
        this(deploymentKey, context, false);
    }

    public CodePush(String deploymentKey, Context context, boolean isDebugMode) {
        SoLoader.init(context, false);
        this.context = context.getApplicationContext();

        this.codePushPackage = new CodePushPackage(context.getFilesDir().getAbsolutePath());
        this.codePushTelemetryManager = new CodePushTelemetryManager(this.context, CodePushConstants.CODE_PUSH_PREFERENCES);
        this.deploymentKey = deploymentKey;
        this.isDebugMode = isDebugMode;
        this.settingsManager = new SettingsManager(this.context);

        try {
            PackageInfo pInfo = this.context.getPackageManager().getPackageInfo(this.context.getPackageName(), 0);
            appVersion = pInfo.versionName;
            buildVersion = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new CodePushUnknownException("Unable to get package info for " + this.context.getPackageName(), e);
        }

        currentInstance = this;

        clearDebugCacheIfNeeded();
        initializeUpdateAfterRestart();
    }

    void clearDebugCacheIfNeeded() {
        if (isDebugMode && settingsManager.isPendingUpdate(null)) {
            // This needs to be kept in sync with https://github.com/facebook/react-native/blob/master/ReactAndroid/src/main/java/com/facebook/react/devsupport/DevSupportManager.java#L78
            File cachedDevBundle = new File(this.context.getFilesDir(), "ReactNativeDevBundle.js");
            if (cachedDevBundle.exists()) {
                cachedDevBundle.delete();
            }
        }
    }

    public boolean didUpdate() {
        return didUpdate;
    }

    String getAppVersion() {
        return appVersion;
    }

    int getBuildVersion() {
        return buildVersion;
    }

    String getDeploymentKey() {
        return deploymentKey;
    }

    String getServerUrl() {
        return serverUrl;
    }

    String getAssetsBundleFileName() {
        return assetsBundleFileName;
    }

    long getBinaryResourcesModifiedTime() {
        ZipFile applicationFile = null;
        try {
            ApplicationInfo ai = this.context.getPackageManager().getApplicationInfo(this.context.getPackageName(), 0);
            applicationFile = new ZipFile(ai.sourceDir);
            ZipEntry classesDexEntry = applicationFile.getEntry(CodePushConstants.RESOURCES_BUNDLE);
            return classesDexEntry.getTime();
        } catch (PackageManager.NameNotFoundException | IOException e) {
            throw new CodePushUnknownException("Error in getting file information about compiled resources", e);
        } finally {
            if (applicationFile != null) {
                try {
                    applicationFile.close();
                } catch (IOException e) {
                    throw new CodePushUnknownException("Error in closing application file.", e);
                }
            }
        }
    }

    public static String getBundleUrl() {
        return getBundleUrl(CodePushConstants.DEFAULT_JS_BUNDLE_NAME);
    }

    public static String getBundleUrl(String assetsBundleFileName) {
        if (currentInstance == null) {
            throw new CodePushNotInitializedException("A CodePush instance has not been created yet. Have you added it to your app's list of ReactPackages?");
        }

        return currentInstance.getBundleUrlInternal(assetsBundleFileName);
    }

    public String getBundleUrlInternal(String assetsBundleFileName) {
        this.assetsBundleFileName = assetsBundleFileName;
        String binaryJsBundleUrl = CodePushConstants.ASSETS_BUNDLE_PREFIX + assetsBundleFileName;
        long binaryResourcesModifiedTime = this.getBinaryResourcesModifiedTime();

        try {
            String packageFilePath = codePushPackage.getCurrentPackageBundlePath(this.assetsBundleFileName);
            if (packageFilePath == null) {
                // There has not been any downloaded updates.
                CodePushUtils.logBundleUrl(binaryJsBundleUrl);
                isRunningBinaryVersion = true;
                return binaryJsBundleUrl;
            }

            ReadableMap packageMetadata = this.codePushPackage.getCurrentPackage();
            Long binaryModifiedDateDuringPackageInstall = null;
            String binaryModifiedDateDuringPackageInstallString = CodePushUtils.tryGetString(packageMetadata, CodePushConstants.BINARY_MODIFIED_TIME_KEY);
            if (binaryModifiedDateDuringPackageInstallString != null) {
                binaryModifiedDateDuringPackageInstall = Long.parseLong(binaryModifiedDateDuringPackageInstallString);
            }

            String packageAppVersion = CodePushUtils.tryGetString(packageMetadata, "appVersion");
            if (binaryModifiedDateDuringPackageInstall != null &&
                    binaryModifiedDateDuringPackageInstall == binaryResourcesModifiedTime &&
                    (isUsingTestConfiguration() || this.appVersion.equals(packageAppVersion))) {
                CodePushUtils.logBundleUrl(packageFilePath);
                isRunningBinaryVersion = false;
                return packageFilePath;
            } else {
                // The binary version is newer.
                this.didUpdate = false;
                if (!this.isDebugMode || !this.appVersion.equals(packageAppVersion)) {
                    this.clearUpdates();
                }

                CodePushUtils.logBundleUrl(binaryJsBundleUrl);
                isRunningBinaryVersion = true;
                return binaryJsBundleUrl;
            }
        } catch (NumberFormatException e) {
            throw new CodePushUnknownException("Error in reading binary modified date from package metadata", e);
        }
    }

    Context getContext() {
        return context;
    }

    void initializeUpdateAfterRestart() {
        // Reset the state which indicates that
        // the app was just freshly updated.
        didUpdate = false;

        JSONObject pendingUpdate = settingsManager.getPendingUpdate();
        if (pendingUpdate != null) {
            try {
                boolean updateIsLoading = pendingUpdate.getBoolean(CodePushConstants.PENDING_UPDATE_IS_LOADING_KEY);
                if (updateIsLoading) {
                    // Pending update was initialized, but notifyApplicationReady was not called.
                    // Therefore, deduce that it is a broken update and rollback.
                    CodePushUtils.log("Update did not finish loading the last time, rolling back to a previous version.");
                    needToReportRollback = true;
                    rollbackPackage();
                } else {
                    // There is in fact a new update running for the first
                    // time, so update the local state to ensure the client knows.
                    didUpdate = true;

                    // Mark that we tried to initialize the new update, so that if it crashes,
                    // we will know that we need to rollback when the app next starts.
                    settingsManager.savePendingUpdate(pendingUpdate.getString(CodePushConstants.PENDING_UPDATE_HASH_KEY),
                            /* isLoading */true);
                }
            } catch (JSONException e) {
                // Should not happen.
                throw new CodePushUnknownException("Unable to read pending update metadata stored in SharedPreferences", e);
            }
        }
    }

    void invalidateCurrentInstance() {
        currentInstance = null;
    }

    boolean isDebugMode() {
        return isDebugMode;
    }

    boolean isRunningBinaryVersion() {
        return isRunningBinaryVersion;
    }

    boolean needToReportRollback() {
        return needToReportRollback;
    }

    private void rollbackPackage() {
        WritableMap failedPackage = codePushPackage.getCurrentPackage();
        settingsManager.saveFailedUpdate(failedPackage);
        codePushPackage.rollbackPackage();
        settingsManager.removePendingUpdate();
    }

    public void setNeedToReportRollback(boolean needToReportRollback) {
        CodePush.needToReportRollback = needToReportRollback;
    }

    /* The below 3 methods are used for running tests.*/
    public static boolean isUsingTestConfiguration() {
        return testConfigurationFlag;
    }

    public static void setUsingTestConfiguration(boolean shouldUseTestConfiguration) {
        testConfigurationFlag = shouldUseTestConfiguration;
    }

    public void clearUpdates() {
        codePushPackage.clearUpdates();
        settingsManager.removePendingUpdate();
        settingsManager.removeFailedUpdates();
    }

    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactApplicationContext) {
        List<NativeModule> nativeModules = new ArrayList<>();
        this.codePushNativeModule = new CodePushNativeModule(reactApplicationContext, this, codePushPackage, codePushTelemetryManager, settingsManager);
        CodePushDialog dialogModule = new CodePushDialog(reactApplicationContext, this.codePushNativeModule);

        nativeModules.add(this.codePushNativeModule);
        nativeModules.add(dialogModule);

        return nativeModules;
    }

    @Override
    public List<Class<? extends JavaScriptModule>> createJSModules() {
        return new ArrayList<>();
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactApplicationContext) {
        return new ArrayList<>();
    }
}
