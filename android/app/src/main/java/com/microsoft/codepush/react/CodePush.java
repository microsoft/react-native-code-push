package com.microsoft.codepush.react;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.soloader.SoLoader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CodePush {

    private static boolean needToReportRollback = false;
    private static boolean isRunningBinaryVersion = false;
    private static boolean testConfigurationFlag = false;

    private boolean didUpdate = false;

    private String assetsBundleFileName;

    private final String ASSETS_BUNDLE_PREFIX = "assets://";
    private final String BINARY_MODIFIED_TIME_KEY = "binaryModifiedTime";
    private final String CODE_PUSH_PREFERENCES = "CodePush";
    private final String DOWNLOAD_PROGRESS_EVENT_NAME = "CodePushDownloadProgress";
    private final String FAILED_UPDATES_KEY = "CODE_PUSH_FAILED_UPDATES";
    private final String PACKAGE_HASH_KEY = "packageHash";
    private final String PENDING_UPDATE_HASH_KEY = "hash";
    private final String PENDING_UPDATE_IS_LOADING_KEY = "isLoading";
    private final String PENDING_UPDATE_KEY = "CODE_PUSH_PENDING_UPDATE";
    private final String RESOURCES_BUNDLE = "resources.arsc";

    // This needs to be kept in sync with https://github.com/facebook/react-native/blob/master/ReactAndroid/src/main/java/com/facebook/react/devsupport/DevSupportManager.java#L78
    private final String REACT_DEV_BUNDLE_CACHE_FILE_NAME = "ReactNativeDevBundle.js";

    // Helper classes.
    private CodePushNativeModule codePushNativeModule;
    private CodePushPackage codePushPackage;
    private CodePushReactPackage codePushReactPackage;
    private CodePushTelemetryManager codePushTelemetryManager;

    // Config properties.
    private String deploymentKey;
    private String appVersion;
    private int buildVersion;
    private final String serverUrl = "https://codepush.azurewebsites.net/";

    private Activity mainActivity;
    private Context applicationContext;
    private final boolean isDebugMode;

    public CodePush(String deploymentKey, Activity mainActivity) {
        this(deploymentKey, mainActivity, false);
    }

    public CodePush(String deploymentKey, Activity mainActivity, boolean isDebugMode) {
        SoLoader.init(mainActivity, false);
        this.applicationContext = mainActivity.getApplicationContext();
        this.codePushPackage = new CodePushPackage(mainActivity.getFilesDir().getAbsolutePath());
        this.codePushTelemetryManager = new CodePushTelemetryManager(this.applicationContext, CODE_PUSH_PREFERENCES);
        this.deploymentKey = deploymentKey;
        this.isDebugMode = isDebugMode;
        this.mainActivity = mainActivity;

        PackageInfo pInfo = null;
        try {
            pInfo = applicationContext.getPackageManager().getPackageInfo(applicationContext.getPackageName(), 0);
            appVersion = pInfo.versionName;
            buildVersion = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new CodePushUnknownException("Unable to get package info for " + applicationContext.getPackageName(), e);
        }

        initializeUpdateAfterRestart();
    }

    private void clearReactDevBundleCache() {
        File cachedDevBundle = new File(this.applicationContext.getFilesDir(), REACT_DEV_BUNDLE_CACHE_FILE_NAME);
        if (cachedDevBundle.exists()) {
            cachedDevBundle.delete();
        }
    }
    
    public long getBinaryResourcesModifiedTime() {
        ApplicationInfo ai = null;
        ZipFile applicationFile = null;
        try {
            ai = applicationContext.getPackageManager().getApplicationInfo(applicationContext.getPackageName(), 0);
            applicationFile = new ZipFile(ai.sourceDir);
            ZipEntry classesDexEntry = applicationFile.getEntry(RESOURCES_BUNDLE);
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

    public String getBundleUrl(String assetsBundleFileName) {
        this.assetsBundleFileName = assetsBundleFileName;
        String binaryJsBundleUrl = ASSETS_BUNDLE_PREFIX + assetsBundleFileName;
        long binaryResourcesModifiedTime = getBinaryResourcesModifiedTime();

        try {
            String packageFilePath = codePushPackage.getCurrentPackageBundlePath();
            if (packageFilePath == null) {
                // There has not been any downloaded updates.
                CodePushUtils.logBundleUrl(binaryJsBundleUrl);
                isRunningBinaryVersion = true;
                return binaryJsBundleUrl;
            }

            ReadableMap packageMetadata = codePushPackage.getCurrentPackage();
            Long binaryModifiedDateDuringPackageInstall = null;
            String binaryModifiedDateDuringPackageInstallString = CodePushUtils.tryGetString(packageMetadata, BINARY_MODIFIED_TIME_KEY);
            if (binaryModifiedDateDuringPackageInstallString != null) {
                binaryModifiedDateDuringPackageInstall = Long.parseLong(binaryModifiedDateDuringPackageInstallString);
            }

            String packageAppVersion = CodePushUtils.tryGetString(packageMetadata, "appVersion");
            if (binaryModifiedDateDuringPackageInstall != null &&
                    binaryModifiedDateDuringPackageInstall == binaryResourcesModifiedTime &&
                    (this.isUsingTestConfiguration() || this.appVersion.equals(packageAppVersion))) {
                CodePushUtils.logBundleUrl(packageFilePath);
                isRunningBinaryVersion = false;
                return packageFilePath;
            } else {
                // The binary version is newer.
                didUpdate = false;
                if (!this.isDebugMode) {
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

    private JSONArray getFailedUpdates() {
        SharedPreferences settings = applicationContext.getSharedPreferences(CODE_PUSH_PREFERENCES, 0);
        String failedUpdatesString = settings.getString(FAILED_UPDATES_KEY, null);
        if (failedUpdatesString == null) {
            return new JSONArray();
        }

        try {
            JSONArray failedUpdates = new JSONArray(failedUpdatesString);
            return failedUpdates;
        } catch (JSONException e) {
            // Unrecognized data format, clear and replace with expected format.
            JSONArray emptyArray = new JSONArray();
            settings.edit().putString(FAILED_UPDATES_KEY, emptyArray.toString()).commit();
            return emptyArray;
        }
    }

    private JSONObject getPendingUpdate() {
        SharedPreferences settings = applicationContext.getSharedPreferences(CODE_PUSH_PREFERENCES, 0);
        String pendingUpdateString = settings.getString(PENDING_UPDATE_KEY, null);
        if (pendingUpdateString == null) {
            return null;
        }

        try {
            JSONObject pendingUpdate = new JSONObject(pendingUpdateString);
            return pendingUpdate;
        } catch (JSONException e) {
            // Should not happen.
            CodePushUtils.log("Unable to parse pending update metadata " + pendingUpdateString +
                    " stored in SharedPreferences");
            return null;
        }
    }

    public ReactPackage getReactPackage() {
        if (codePushReactPackage == null) {
            codePushReactPackage = new CodePushReactPackage();
        }
        return codePushReactPackage;
    }
    
    private void initializeUpdateAfterRestart() {
        JSONObject pendingUpdate = getPendingUpdate();
        if (pendingUpdate != null) {
            didUpdate = true;
            try {
                boolean updateIsLoading = pendingUpdate.getBoolean(PENDING_UPDATE_IS_LOADING_KEY);
                if (updateIsLoading) {
                    // Pending update was initialized, but notifyApplicationReady was not called.
                    // Therefore, deduce that it is a broken update and rollback.
                    CodePushUtils.log("Update did not finish loading the last time, rolling back to a previous version.");
                    needToReportRollback = true;
                    rollbackPackage();
                } else {
                    // Clear the React dev bundle cache so that new updates can be loaded.
                    if (this.isDebugMode) {
                        clearReactDevBundleCache();
                    }
                    // Mark that we tried to initialize the new update, so that if it crashes,
                    // we will know that we need to rollback when the app next starts.
                    savePendingUpdate(pendingUpdate.getString(PENDING_UPDATE_HASH_KEY),
                            /* isLoading */true);
                }
            } catch (JSONException e) {
                // Should not happen.
                throw new CodePushUnknownException("Unable to read pending update metadata stored in SharedPreferences", e);
            }
        }
    }

    private boolean isFailedHash(String packageHash) {
        JSONArray failedUpdates = getFailedUpdates();
        if (packageHash != null) {
            for (int i = 0; i < failedUpdates.length(); i++) {
                JSONObject failedPackage = null;
                try {
                    failedPackage = failedUpdates.getJSONObject(i);
                    String failedPackageHash = failedPackage.getString(PACKAGE_HASH_KEY);
                    if (packageHash.equals(failedPackageHash)) {
                        return true;
                    }
                } catch (JSONException e) {
                    throw new CodePushUnknownException("Unable to read failedUpdates data stored in SharedPreferences.", e);
                }
            }
        }

        return false;
    }

    private boolean isPendingUpdate(String packageHash) {
        JSONObject pendingUpdate = getPendingUpdate();
        
        try {
            boolean updateIsPending = pendingUpdate != null &&
                                      pendingUpdate.getBoolean(PENDING_UPDATE_IS_LOADING_KEY) == false &&
                                      (packageHash == null || pendingUpdate.getString(PENDING_UPDATE_HASH_KEY).equals(packageHash));
            return updateIsPending;
        }
        catch (JSONException e) {
            throw new CodePushUnknownException("Unable to read pending update metadata in isPendingUpdate.", e);
        }
    }

    private void removeFailedUpdates() {
        SharedPreferences settings = applicationContext.getSharedPreferences(CODE_PUSH_PREFERENCES, 0);
        settings.edit().remove(FAILED_UPDATES_KEY).commit();
    }

    private void removePendingUpdate() {
        SharedPreferences settings = applicationContext.getSharedPreferences(CODE_PUSH_PREFERENCES, 0);
        settings.edit().remove(PENDING_UPDATE_KEY).commit();
    }
    
    private void rollbackPackage() {
        WritableMap failedPackage = codePushPackage.getCurrentPackage();
        saveFailedUpdate(failedPackage);
        codePushPackage.rollbackPackage();
        removePendingUpdate();
    }

    private void saveFailedUpdate(WritableMap failedPackage) {
        SharedPreferences settings = applicationContext.getSharedPreferences(CODE_PUSH_PREFERENCES, 0);
        String failedUpdatesString = settings.getString(FAILED_UPDATES_KEY, null);
        JSONArray failedUpdates;
        if (failedUpdatesString == null) {
            failedUpdates = new JSONArray();
        } else {
            try {
                failedUpdates = new JSONArray(failedUpdatesString);
            } catch (JSONException e) {
                // Should not happen.
                throw new CodePushMalformedDataException("Unable to parse failed updates information " +
                        failedUpdatesString + " stored in SharedPreferences", e);
            }
        }

        JSONObject failedPackageJSON = CodePushUtils.convertReadableToJsonObject(failedPackage);
        failedUpdates.put(failedPackageJSON);
        settings.edit().putString(FAILED_UPDATES_KEY, failedUpdates.toString()).commit();
    }

    private void savePendingUpdate(String packageHash, boolean isLoading) {
        SharedPreferences settings = applicationContext.getSharedPreferences(CODE_PUSH_PREFERENCES, 0);
        JSONObject pendingUpdate = new JSONObject();
        try {
            pendingUpdate.put(PENDING_UPDATE_HASH_KEY, packageHash);
            pendingUpdate.put(PENDING_UPDATE_IS_LOADING_KEY, isLoading);
            settings.edit().putString(PENDING_UPDATE_KEY, pendingUpdate.toString()).commit();
        } catch (JSONException e) {
            // Should not happen.
            throw new CodePushUnknownException("Unable to save pending update.", e);
        }
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
        removePendingUpdate();
        removeFailedUpdates();
    }

    private class CodePushNativeModule extends ReactContextBaseJavaModule {

        private LifecycleEventListener lifecycleEventListener = null;

        private void loadBundle() {
            Intent intent = mainActivity.getIntent();
            mainActivity.finish();
            mainActivity.startActivity(intent);
        }

        @ReactMethod
        public void downloadUpdate(final ReadableMap updatePackage, final Promise promise) {
            AsyncTask asyncTask = new AsyncTask() {
                @Override
                protected Void doInBackground(Object... params) {
                    try {
                        WritableMap mutableUpdatePackage = CodePushUtils.convertReadableMapToWritableMap(updatePackage);
                        mutableUpdatePackage.putString(BINARY_MODIFIED_TIME_KEY, "" + getBinaryResourcesModifiedTime());
                        codePushPackage.downloadPackage(applicationContext, mutableUpdatePackage, new DownloadProgressCallback() {
                            @Override
                            public void call(DownloadProgress downloadProgress) {
                                getReactApplicationContext()
                                        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                        .emit(DOWNLOAD_PROGRESS_EVENT_NAME, downloadProgress.createWritableMap());
                            }
                        });

                        WritableMap newPackage = codePushPackage.getPackage(CodePushUtils.tryGetString(updatePackage, codePushPackage.PACKAGE_HASH_KEY));
                        promise.resolve(newPackage);
                    } catch (IOException e) {
                        e.printStackTrace();
                        promise.reject(e.getMessage());
                    }

                    return null;
                }
            };

            asyncTask.execute();
        }

        @ReactMethod
        public void getConfiguration(Promise promise) {
            WritableNativeMap configMap = new WritableNativeMap();
            configMap.putString("appVersion", appVersion);
            configMap.putInt("buildVersion", buildVersion);
            configMap.putString("deploymentKey", deploymentKey);
            configMap.putString("serverUrl", serverUrl);
            configMap.putString("clientUniqueId",
                    Settings.Secure.getString(mainActivity.getContentResolver(),
                            android.provider.Settings.Secure.ANDROID_ID));
            promise.resolve(configMap);
        }

        @ReactMethod
        public void getCurrentPackage(final Promise promise) {
            AsyncTask asyncTask = new AsyncTask() {
                @Override
                protected Void doInBackground(Object... params) {
                    WritableMap currentPackage = codePushPackage.getCurrentPackage();

                    Boolean isPendingUpdate = false;

                    if (currentPackage.hasKey(codePushPackage.PACKAGE_HASH_KEY)) {
                        String currentHash = currentPackage.getString(codePushPackage.PACKAGE_HASH_KEY);
                        isPendingUpdate = CodePush.this.isPendingUpdate(currentHash);
                    }

                    currentPackage.putBoolean("isPending", isPendingUpdate);
                    promise.resolve(currentPackage);
                    return null;
                }
            };

            asyncTask.execute();
        }

        @ReactMethod
        public void getNewStatusReport(final Promise promise) {

            AsyncTask asyncTask = new AsyncTask() {
                @Override
                protected Void doInBackground(Object... params) {
                    if (needToReportRollback) {
                        needToReportRollback = false;
                        JSONArray failedUpdates = getFailedUpdates();
                        if (failedUpdates != null && failedUpdates.length() > 0) {
                            try {
                                JSONObject lastFailedPackageJSON = failedUpdates.getJSONObject(failedUpdates.length() - 1);
                                WritableMap lastFailedPackage = CodePushUtils.convertJsonObjectToWriteable(lastFailedPackageJSON);
                                WritableMap failedStatusReport = codePushTelemetryManager.getRollbackReport(lastFailedPackage);
                                if (failedStatusReport != null) {
                                    promise.resolve(failedStatusReport);
                                    return null;
                                }
                            } catch (JSONException e) {
                                throw new CodePushUnknownException("Unable to read failed updates information stored in SharedPreferences.", e);
                            }
                        }
                    } else if (didUpdate) {
                        WritableMap currentPackage = codePushPackage.getCurrentPackage();
                        if (currentPackage != null) {
                            WritableMap newPackageStatusReport = codePushTelemetryManager.getUpdateReport(currentPackage);
                            if (newPackageStatusReport != null) {
                                promise.resolve(newPackageStatusReport);
                                return null;
                            }
                        }
                    } else if (isRunningBinaryVersion) {
                        WritableMap newAppVersionStatusReport = codePushTelemetryManager.getBinaryUpdateReport(appVersion);
                        if (newAppVersionStatusReport != null) {
                            promise.resolve(newAppVersionStatusReport);
                            return null;
                        }
                    }

                    promise.resolve("");
                    return null;
                }
            };

            asyncTask.execute();
        }

        @ReactMethod
        public void installUpdate(final ReadableMap updatePackage, final int installMode, final Promise promise) {
            AsyncTask asyncTask = new AsyncTask() {
                @Override
                protected Void doInBackground(Object... params) {
                    try {
                        codePushPackage.installPackage(updatePackage);

                        String pendingHash = CodePushUtils.tryGetString(updatePackage, codePushPackage.PACKAGE_HASH_KEY);
                        if (pendingHash == null) {
                            throw new CodePushUnknownException("Update package to be installed has no hash.");
                        } else {
                            savePendingUpdate(pendingHash, /* isLoading */false);
                        }

                        if (installMode == CodePushInstallMode.ON_NEXT_RESUME.getValue() &&
                                lifecycleEventListener == null) {
                            // Ensure we do not add the listener twice.
                            lifecycleEventListener = new LifecycleEventListener() {
                                @Override
                                public void onHostResume() {
                                    loadBundle();
                                }

                                @Override
                                public void onHostPause() {
                                }

                                @Override
                                public void onHostDestroy() {
                                }
                            };
                            getReactApplicationContext().addLifecycleEventListener(lifecycleEventListener);
                        }

                        promise.resolve("");
                    } catch (IOException e) {
                        e.printStackTrace();
                        promise.reject(e.getMessage());
                    }

                    return null;
                }
            };

            asyncTask.execute();
        }
        
        @ReactMethod
        public void isFailedUpdate(String packageHash, Promise promise) {
            promise.resolve(isFailedHash(packageHash));
        }

        @ReactMethod
        public void isFirstRun(String packageHash, Promise promise) {
            boolean isFirstRun = didUpdate
                    && packageHash != null
                    && packageHash.length() > 0
                    && packageHash.equals(codePushPackage.getCurrentPackageHash());
            promise.resolve(isFirstRun);
        }

        @ReactMethod
        public void notifyApplicationReady(Promise promise) {
            removePendingUpdate();
            promise.resolve("");
        }
        
        @ReactMethod
        public void restartApp(boolean onlyIfUpdateIsPending) {
            // If this is an unconditional restart request, or there
            // is current pending update, then reload the app.
            if (!onlyIfUpdateIsPending || CodePush.this.isPendingUpdate(null)) {
                loadBundle();
            }
        }

        @ReactMethod
        // Replaces the current bundle with the one downloaded from removeBundleUrl.
        // It is only to be used during tests. No-ops if the test configuration flag is not set.
        public void downloadAndReplaceCurrentBundle(String remoteBundleUrl) {
            if (isUsingTestConfiguration()) {
                try {
                    codePushPackage.downloadAndReplaceCurrentBundle(remoteBundleUrl);
                } catch (IOException e) {
                    throw new CodePushUnknownException("Unable to replace current bundle", e);
                }
            }
        }

        @Override
        public Map<String, Object> getConstants() {
            final Map<String, Object> constants = new HashMap<>();
            constants.put("codePushInstallModeImmediate", CodePushInstallMode.IMMEDIATE.getValue());
            constants.put("codePushInstallModeOnNextRestart", CodePushInstallMode.ON_NEXT_RESTART.getValue());
            constants.put("codePushInstallModeOnNextResume", CodePushInstallMode.ON_NEXT_RESUME.getValue());
            return constants;
        }

        public CodePushNativeModule(ReactApplicationContext reactContext) {
            super(reactContext);
        }

        @Override
        public String getName() {
            return "CodePush";
        }
    }

    private class CodePushReactPackage implements ReactPackage {
        @Override
        public List<NativeModule> createNativeModules(ReactApplicationContext reactApplicationContext) {
            List<NativeModule> nativeModules = new ArrayList<>();
            CodePush.this.codePushNativeModule = new CodePushNativeModule(reactApplicationContext);
            CodePushDialog dialogModule = new CodePushDialog(reactApplicationContext, mainActivity);

            nativeModules.add(CodePush.this.codePushNativeModule);
            nativeModules.add(dialogModule);

            return nativeModules;
        }

        @Override
        public List<Class<? extends JavaScriptModule>> createJSModules() {
            return new ArrayList();
        }

        @Override
        public List<ViewManager> createViewManagers(ReactApplicationContext reactApplicationContext) {
            return new ArrayList();
        }
    }
}