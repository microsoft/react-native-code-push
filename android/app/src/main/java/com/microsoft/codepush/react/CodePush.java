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

    private static boolean mIsRunningBinaryVersion = false;
    private static boolean mNeedToReportRollback = false;
    private static boolean mTestConfigurationFlag = false;

    private boolean mDidUpdate = false;

    private String mAssetsBundleFileName;

    // Helper classes.
    private CodePushNativeModule mCodePushNativeModule;
    private CodePushUpdateManager mCodePushUpdateManager;
    private CodePushTelemetryManager mCodePushTelemetryManager;
    private SettingsManager mSettingsManager;

    // Config properties.
    private String mAppVersion;
    private int mBuildVersion;
    private String mDeploymentKey;
    private String mServerUrl = "https://codepush.azurewebsites.net/";

    private Context mContext;
    private final boolean mIsDebugMode;

    private static CodePush mCurrentInstance;

    public CodePush(String deploymentKey, Context context) {
        this(deploymentKey, context, false);
    }

    public CodePush(String deploymentKey, Context context, boolean isDebugMode) {
        SoLoader.init(context, false);
        mContext = context.getApplicationContext();

        mCodePushUpdateManager = new CodePushUpdateManager(context.getFilesDir().getAbsolutePath());
        mCodePushTelemetryManager = new CodePushTelemetryManager(mContext);
        mDeploymentKey = deploymentKey;
        mIsDebugMode = isDebugMode;
        mSettingsManager = new SettingsManager(mContext);

        try {
            PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            mAppVersion = pInfo.versionName;
            mBuildVersion = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new CodePushUnknownException("Unable to get package info for " + mContext.getPackageName(), e);
        }

        mCurrentInstance = this;

        clearDebugCacheIfNeeded();
        initializeUpdateAfterRestart();
    }

    public CodePush(String deploymentKey, Context context, boolean isDebugMode, String serverUrl) {
        this(deploymentKey, context, isDebugMode);
        mServerUrl = serverUrl;
    }

    public void clearDebugCacheIfNeeded() {
        if (mIsDebugMode && mSettingsManager.isPendingUpdate(null)) {
            // This needs to be kept in sync with https://github.com/facebook/react-native/blob/master/ReactAndroid/src/main/java/com/facebook/react/devsupport/DevSupportManager.java#L78
            File cachedDevBundle = new File(mContext.getFilesDir(), "ReactNativeDevBundle.js");
            if (cachedDevBundle.exists()) {
                cachedDevBundle.delete();
            }
        }
    }

    public boolean didUpdate() {
        return mDidUpdate;
    }

    public String getAppVersion() {
        return mAppVersion;
    }

    public String getAssetsBundleFileName() {
        return mAssetsBundleFileName;
    }

    long getBinaryResourcesModifiedTime() {
        ZipFile applicationFile = null;
        try {
            ApplicationInfo ai = this.mContext.getPackageManager().getApplicationInfo(this.mContext.getPackageName(), 0);
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

    public int getBuildVersion() {
        return mBuildVersion;
    }

    @Deprecated
    public static String getBundleUrl() {
        return getJSBundleFile();
    }

    @Deprecated
    public static String getBundleUrl(String assetsBundleFileName) {
        return getJSBundleFile(assetsBundleFileName);
    }

    public Context getContext() {
        return mContext;
    }

    public String getDeploymentKey() {
        return mDeploymentKey;
    }

    public static String getJSBundleFile() {
        return CodePush.getBundleUrl();
    }

    public static String getJSBundleFile(String assetsBundleFileName) {
        if (mCurrentInstance == null) {
            throw new CodePushNotInitializedException("A CodePush instance has not been created yet. Have you added it to your app's list of ReactPackages?");
        }

        return mCurrentInstance.getJSBundleFileInternal(assetsBundleFileName);
    }

    public String getJSBundleFileInternal(String assetsBundleFileName) {
        this.mAssetsBundleFileName = assetsBundleFileName;
        String binaryJsBundleUrl = CodePushConstants.ASSETS_BUNDLE_PREFIX + assetsBundleFileName;
        long binaryResourcesModifiedTime = this.getBinaryResourcesModifiedTime();

        try {
            String packageFilePath = mCodePushUpdateManager.getCurrentPackageBundlePath(this.mAssetsBundleFileName);
            if (packageFilePath == null) {
                // There has not been any downloaded updates.
                CodePushUtils.logBundleUrl(binaryJsBundleUrl);
                mIsRunningBinaryVersion = true;
                return binaryJsBundleUrl;
            }

            ReadableMap packageMetadata = this.mCodePushUpdateManager.getCurrentPackage();
            Long binaryModifiedDateDuringPackageInstall = null;
            String binaryModifiedDateDuringPackageInstallString = CodePushUtils.tryGetString(packageMetadata, CodePushConstants.BINARY_MODIFIED_TIME_KEY);
            if (binaryModifiedDateDuringPackageInstallString != null) {
                binaryModifiedDateDuringPackageInstall = Long.parseLong(binaryModifiedDateDuringPackageInstallString);
            }

            String packageAppVersion = CodePushUtils.tryGetString(packageMetadata, "appVersion");
            if (binaryModifiedDateDuringPackageInstall != null &&
                    binaryModifiedDateDuringPackageInstall == binaryResourcesModifiedTime &&
                    (isUsingTestConfiguration() || this.mAppVersion.equals(packageAppVersion))) {
                CodePushUtils.logBundleUrl(packageFilePath);
                mIsRunningBinaryVersion = false;
                return packageFilePath;
            } else {
                // The binary version is newer.
                this.mDidUpdate = false;
                if (!this.mIsDebugMode || !this.mAppVersion.equals(packageAppVersion)) {
                    this.clearUpdates();
                }

                CodePushUtils.logBundleUrl(binaryJsBundleUrl);
                mIsRunningBinaryVersion = true;
                return binaryJsBundleUrl;
            }
        } catch (NumberFormatException e) {
            throw new CodePushUnknownException("Error in reading binary modified date from package metadata", e);
        }
    }

    public String getServerUrl() {
        return mServerUrl;
    }

    void initializeUpdateAfterRestart() {
        // Reset the state which indicates that
        // the app was just freshly updated.
        mDidUpdate = false;

        JSONObject pendingUpdate = mSettingsManager.getPendingUpdate();
        if (pendingUpdate != null) {
            try {
                boolean updateIsLoading = pendingUpdate.getBoolean(CodePushConstants.PENDING_UPDATE_IS_LOADING_KEY);
                if (updateIsLoading) {
                    // Pending update was initialized, but notifyApplicationReady was not called.
                    // Therefore, deduce that it is a broken update and rollback.
                    CodePushUtils.log("Update did not finish loading the last time, rolling back to a previous version.");
                    mNeedToReportRollback = true;
                    rollbackPackage();
                } else {
                    // There is in fact a new update running for the first
                    // time, so update the local state to ensure the client knows.
                    mDidUpdate = true;

                    // Mark that we tried to initialize the new update, so that if it crashes,
                    // we will know that we need to rollback when the app next starts.
                    mSettingsManager.savePendingUpdate(pendingUpdate.getString(CodePushConstants.PENDING_UPDATE_HASH_KEY),
                            /* isLoading */true);
                }
            } catch (JSONException e) {
                // Should not happen.
                throw new CodePushUnknownException("Unable to read pending update metadata stored in SharedPreferences", e);
            }
        }
    }

    void invalidateCurrentInstance() {
        mCurrentInstance = null;
    }

    boolean isDebugMode() {
        return mIsDebugMode;
    }

    boolean isRunningBinaryVersion() {
        return mIsRunningBinaryVersion;
    }

    boolean needToReportRollback() {
        return mNeedToReportRollback;
    }

    private void rollbackPackage() {
        WritableMap failedPackage = mCodePushUpdateManager.getCurrentPackage();
        mSettingsManager.saveFailedUpdate(failedPackage);
        mCodePushUpdateManager.rollbackPackage();
        mSettingsManager.removePendingUpdate();
    }

    public void setNeedToReportRollback(boolean needToReportRollback) {
        CodePush.mNeedToReportRollback = needToReportRollback;
    }

    /* The below 3 methods are used for running tests.*/
    public static boolean isUsingTestConfiguration() {
        return mTestConfigurationFlag;
    }

    public static void setUsingTestConfiguration(boolean shouldUseTestConfiguration) {
        mTestConfigurationFlag = shouldUseTestConfiguration;
    }

    public void clearUpdates() {
        mCodePushUpdateManager.clearUpdates();
        mSettingsManager.removePendingUpdate();
        mSettingsManager.removeFailedUpdates();
    }

    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactApplicationContext) {
        List<NativeModule> nativeModules = new ArrayList<>();
        mCodePushNativeModule = new CodePushNativeModule(reactApplicationContext, this, mCodePushUpdateManager, mCodePushTelemetryManager, mSettingsManager);
        CodePushDialog dialogModule = new CodePushDialog(reactApplicationContext, mCodePushNativeModule);

        nativeModules.add(mCodePushNativeModule);
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
