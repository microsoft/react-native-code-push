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

    private static boolean testConfigurationFlag = false;
    private boolean didUpdate = false;

    private String assetsBundleFileName;

    private final String FAILED_UPDATES_KEY = "CODE_PUSH_FAILED_UPDATES";
    private final String PENDING_UPDATE_KEY = "CODE_PUSH_PENDING_UPDATE";
    private final String PENDING_UPDATE_HASH_KEY = "hash";
    private final String PENDING_UPDATE_IS_LOADING_KEY = "isLoading";
    private final String ASSETS_BUNDLE_PREFIX = "assets://";
    private final String CODE_PUSH_PREFERENCES = "CodePush";
    private final String DOWNLOAD_PROGRESS_EVENT_NAME = "CodePushDownloadProgress";
    private final String RESOURCES_BUNDLE = "resources.arsc";
    // This needs to be kept in sync with https://github.com/facebook/react-native/blob/master/ReactAndroid/src/main/java/com/facebook/react/devsupport/DevSupportManager.java#L78
    private final String REACT_DEV_BUNDLE_CACHE_FILE_NAME = "ReactNativeDevBundle.js";
    private final String BINARY_MODIFIED_TIME_KEY = "binaryModifiedTime";

    private CodePushPackage codePushPackage;
    private CodePushReactPackage codePushReactPackage;
    private CodePushNativeModule codePushNativeModule;

    // Config properties.
    private String deploymentKey;
    private String appVersion;
    private int buildVersion;
    private final String serverUrl = "https://codepush.azurewebsites.net/";

    private Activity mainActivity;
    private Context applicationContext;

    public CodePush(String deploymentKey, Activity mainActivity) {
        SoLoader.init(mainActivity, false);
        this.deploymentKey = deploymentKey;
        this.codePushPackage = new CodePushPackage(mainActivity.getFilesDir().getAbsolutePath());
        this.mainActivity = mainActivity;
        this.applicationContext = mainActivity.getApplicationContext();
        this.deploymentKey = deploymentKey;

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
                return binaryJsBundleUrl;
            }

            ReadableMap packageMetadata = codePushPackage.getCurrentPackage();
            Long binaryModifiedDateDuringPackageInstall = null;
            String binaryModifiedDateDuringPackageInstallString = CodePushUtils.tryGetString(packageMetadata, BINARY_MODIFIED_TIME_KEY);
            if (binaryModifiedDateDuringPackageInstallString != null) {
                binaryModifiedDateDuringPackageInstall = Long.parseLong(binaryModifiedDateDuringPackageInstallString);
            }

            String pacakgeAppVersion = CodePushUtils.tryGetString(packageMetadata, "appVersion");
            if (binaryModifiedDateDuringPackageInstall != null &&
                    binaryModifiedDateDuringPackageInstall == binaryResourcesModifiedTime &&
                    this.appVersion.equals(pacakgeAppVersion)) {
                CodePushUtils.logBundleUrl(packageFilePath);
                return packageFilePath;
            } else {
                // The binary version is newer.
                didUpdate = false;
                CodePushUtils.logBundleUrl(binaryJsBundleUrl);
                return binaryJsBundleUrl;
            }
        } catch (IOException e) {
            throw new CodePushUnknownException("Error in getting current package bundle path", e);
        } catch (NumberFormatException e) {
            throw new CodePushUnknownException("Error in reading binary modified date from package metadata", e);
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
                    rollbackPackage();
                } else {
                    // Clear the React dev bundle cache so that new updates can be loaded.
                    clearReactDevBundleCache();
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
        SharedPreferences settings = applicationContext.getSharedPreferences(CODE_PUSH_PREFERENCES, 0);
        String failedUpdatesString = settings.getString(FAILED_UPDATES_KEY, null);
        if (failedUpdatesString == null) {
            return false;
        }

        try {
            JSONObject failedUpdates = new JSONObject(failedUpdatesString);
            return failedUpdates.has(packageHash);
        } catch (JSONException e) {
            // Should not happen.
            throw new CodePushUnknownException("Unable to parse failed updates information " +
                    failedUpdatesString + " stored in SharedPreferences", e);
        }
    }

    private boolean isPendingUpdate(String packageHash) {
        JSONObject pendingUpdate = getPendingUpdate();
        
        try {
            boolean updateIsPending = pendingUpdate != null &&
                                      pendingUpdate.getBoolean(PENDING_UPDATE_IS_LOADING_KEY) == false &&
                                      pendingUpdate.getString(PENDING_UPDATE_HASH_KEY).equals(packageHash);
            return updateIsPending;
        }
        catch (JSONException e) {
            throw new CodePushUnknownException("Unable to read pending update metadata in isPendingUpdate.", e);
        }
    }

    private void removePendingUpdate() {
        SharedPreferences settings = applicationContext.getSharedPreferences(CODE_PUSH_PREFERENCES, 0);
        settings.edit().remove(PENDING_UPDATE_KEY).commit();
    }
    
    private void rollbackPackage() {
        try {
            String packageHash = codePushPackage.getCurrentPackageHash();
            saveFailedUpdate(packageHash);
        } catch (IOException e) {
            throw new CodePushUnknownException("Attempted a rollback without having a current downloaded package", e);
        }

        try {
            codePushPackage.rollbackPackage();
        } catch (IOException e) {
            throw new CodePushUnknownException("Error in rolling back package", e);
        }

        removePendingUpdate();
    }

    private void saveFailedUpdate(String packageHash) {
        SharedPreferences settings = applicationContext.getSharedPreferences(CODE_PUSH_PREFERENCES, 0);
        String failedUpdatesString = settings.getString(FAILED_UPDATES_KEY, null);
        JSONObject failedUpdates;
        if (failedUpdatesString == null) {
            failedUpdates = new JSONObject();
        } else {
            try {
                failedUpdates = new JSONObject(failedUpdatesString);
            } catch (JSONException e) {
                // Should not happen.
                throw new CodePushMalformedDataException("Unable to parse failed updates information " +
                        failedUpdatesString + " stored in SharedPreferences", e);
            }
        }
        try {
            failedUpdates.put(packageHash, true);
            settings.edit().putString(FAILED_UPDATES_KEY, failedUpdates.toString()).commit();
        } catch (JSONException e) {
            // Should not happen unless the packageHash is null.
            throw new CodePushUnknownException("Unable to save package hash " +
                    packageHash + " as a failed update", e);
        }
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
            promise.resolve(configMap);
        }

        @ReactMethod
        public void getCurrentPackage(final Promise promise) {
            AsyncTask asyncTask = new AsyncTask() {
                @Override
                protected Void doInBackground(Object... params) {
                    try {
                        WritableMap currentPackage = codePushPackage.getCurrentPackage();

                        Boolean isPendingUpdate = false;

                        if (currentPackage.hasKey(codePushPackage.PACKAGE_HASH_KEY)) {
                            String currentHash = currentPackage.getString(codePushPackage.PACKAGE_HASH_KEY);
                            isPendingUpdate = CodePush.this.isPendingUpdate(currentHash);
                        }

                        currentPackage.putBoolean("isPending", isPendingUpdate);
                        promise.resolve(currentPackage);
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
            try {
                boolean isFirstRun = didUpdate
                        && packageHash != null
                        && packageHash.length() > 0
                        && packageHash.equals(codePushPackage.getCurrentPackageHash());
                promise.resolve(isFirstRun);
            } catch (IOException e) {
                e.printStackTrace();
                promise.reject(e.getMessage());
            }
        }

        @ReactMethod
        public void notifyApplicationReady(Promise promise) {
            removePendingUpdate();
            promise.resolve("");
        }
        
        @ReactMethod
        public void restartApp() {
            loadBundle();
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