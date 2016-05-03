package com.microsoft.codepush.react;

import com.facebook.react.ReactActivity;
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
import com.facebook.react.uimanager.ReactChoreographer;
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
import android.view.Choreographer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CodePush implements ReactPackage {
    private static boolean needToReportRollback = false;
    private static boolean isRunningBinaryVersion = false;
    private static boolean testConfigurationFlag = false;

    private boolean didUpdate = false;

    private String assetsBundleFileName;

    private static final String ASSETS_BUNDLE_PREFIX = "assets://";
    private static final String BINARY_MODIFIED_TIME_KEY = "binaryModifiedTime";
    private final String CODE_PUSH_PREFERENCES = "CodePush";
    private static final String DEFAULT_JS_BUNDLE_NAME = "index.android.bundle";
    private final String DOWNLOAD_PROGRESS_EVENT_NAME = "CodePushDownloadProgress";
    private final String FAILED_UPDATES_KEY = "CODE_PUSH_FAILED_UPDATES";
    private final String PACKAGE_HASH_KEY = "packageHash";
    private final String PENDING_UPDATE_HASH_KEY = "hash";
    private final String PENDING_UPDATE_IS_LOADING_KEY = "isLoading";
    private final String PENDING_UPDATE_KEY = "CODE_PUSH_PENDING_UPDATE";
    private final String RESOURCES_BUNDLE = "resources.arsc";

    // Helper classes.
    private CodePushNativeModule codePushNativeModule;
    private CodePushPackage codePushPackage;
    private CodePushTelemetryManager codePushTelemetryManager;

    // Config properties.
    private String appVersion;
    private int buildVersion;
    private String deploymentKey;
    private final String serverUrl = "https://codepush.azurewebsites.net/";

    private Activity mainActivity;
    private Context applicationContext;
    private final boolean isDebugMode;

    private static CodePush currentInstance;

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

        try {
            PackageInfo pInfo = applicationContext.getPackageManager().getPackageInfo(applicationContext.getPackageName(), 0);
            appVersion = pInfo.versionName;
            buildVersion = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new CodePushUnknownException("Unable to get package info for " + applicationContext.getPackageName(), e);
        }

        if (currentInstance != null) {
            CodePushUtils.log("More than one CodePush instance has been initialized. Please use the instance method codePush.getBundleUrlInternal() to get the correct bundleURL for a particular instance.");
        }

        currentInstance = this;

        clearDebugCacheIfNeeded();
        initializeUpdateAfterRestart();
    }

    private void clearDebugCacheIfNeeded() {
        if (isDebugMode && isPendingUpdate(null)) {
            // This needs to be kept in sync with https://github.com/facebook/react-native/blob/master/ReactAndroid/src/main/java/com/facebook/react/devsupport/DevSupportManager.java#L78
            File cachedDevBundle = new File(applicationContext.getFilesDir(), "ReactNativeDevBundle.js");
            if (cachedDevBundle.exists()) {
                cachedDevBundle.delete();
            }
        }
    }

    private long getBinaryResourcesModifiedTime() {
        ZipFile applicationFile = null;
        try {
            ApplicationInfo ai = applicationContext.getPackageManager().getApplicationInfo(applicationContext.getPackageName(), 0);
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

    public static String getBundleUrl() {
        return getBundleUrl(DEFAULT_JS_BUNDLE_NAME);
    }

    public static String getBundleUrl(String assetsBundleFileName) {
        if (currentInstance == null) {
            throw new CodePushNotInitializedException("A CodePush instance has not been created yet. Have you added it to your app's list of ReactPackages?");
        }

        return currentInstance.getBundleUrlInternal(assetsBundleFileName);
    }

    public String getBundleUrlInternal(String assetsBundleFileName) {
        this.assetsBundleFileName = assetsBundleFileName;
        String binaryJsBundleUrl = ASSETS_BUNDLE_PREFIX + assetsBundleFileName;
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
            String binaryModifiedDateDuringPackageInstallString = CodePushUtils.tryGetString(packageMetadata, BINARY_MODIFIED_TIME_KEY);
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

    private JSONArray getFailedUpdates() {
        SharedPreferences settings = applicationContext.getSharedPreferences(CODE_PUSH_PREFERENCES, 0);
        String failedUpdatesString = settings.getString(FAILED_UPDATES_KEY, null);
        if (failedUpdatesString == null) {
            return new JSONArray();
        }

        try {
            return new JSONArray(failedUpdatesString);
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
            return new JSONObject(pendingUpdateString);
        } catch (JSONException e) {
            // Should not happen.
            CodePushUtils.log("Unable to parse pending update metadata " + pendingUpdateString +
                    " stored in SharedPreferences");
            return null;
        }
    }

    private void initializeUpdateAfterRestart() {
        // Reset the state which indicates that
        // the app was just freshly updated.
        didUpdate = false;

        JSONObject pendingUpdate = getPendingUpdate();
        if (pendingUpdate != null) {
            try {
                boolean updateIsLoading = pendingUpdate.getBoolean(PENDING_UPDATE_IS_LOADING_KEY);
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
                try {
                    JSONObject failedPackage = failedUpdates.getJSONObject(i);
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
            return pendingUpdate != null &&
                   !pendingUpdate.getBoolean(PENDING_UPDATE_IS_LOADING_KEY) &&
                   (packageHash == null || pendingUpdate.getString(PENDING_UPDATE_HASH_KEY).equals(packageHash));
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

    private void saveFailedUpdate(ReadableMap failedPackage) {
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
        private int minimumBackgroundDuration = 0;

        public CodePushNativeModule(ReactApplicationContext reactContext) {
            super(reactContext);
        }

        @Override
        public Map<String, Object> getConstants() {
            final Map<String, Object> constants = new HashMap<>();

            constants.put("codePushInstallModeImmediate", CodePushInstallMode.IMMEDIATE.getValue());
            constants.put("codePushInstallModeOnNextRestart", CodePushInstallMode.ON_NEXT_RESTART.getValue());
            constants.put("codePushInstallModeOnNextResume", CodePushInstallMode.ON_NEXT_RESUME.getValue());

            constants.put("codePushUpdateStateRunning", CodePushUpdateState.RUNNING.getValue());
            constants.put("codePushUpdateStatePending", CodePushUpdateState.PENDING.getValue());
            constants.put("codePushUpdateStateLatest", CodePushUpdateState.LATEST.getValue());

            return constants;
        }

        @Override
        public String getName() {
            return "CodePush";
        }

        private void loadBundleLegacy() {
            Intent intent = mainActivity.getIntent();
            mainActivity.finish();
            mainActivity.startActivity(intent);

            currentInstance = null;
        }

        private void loadBundle() {
            CodePush.this.clearDebugCacheIfNeeded();

            try {
                // #1) Get the private ReactInstanceManager, which is what includes
                //     the logic to reload the current React context.
                Field instanceManagerField = ReactActivity.class.getDeclaredField("mReactInstanceManager");
                instanceManagerField.setAccessible(true); // Make a private field accessible
                final Object instanceManager = instanceManagerField.get(mainActivity);

                // #2) Update the locally stored JS bundle file path
                String latestJSBundleFile = CodePush.this.getBundleUrlInternal(CodePush.this.assetsBundleFileName);
                Field jsBundleField = instanceManager.getClass().getDeclaredField("mJSBundleFile");
                jsBundleField.setAccessible(true);
                jsBundleField.set(instanceManager, latestJSBundleFile);

                // #3) Get the context creation method and fire it on the UI thread (which RN enforces)
                final Method recreateMethod = instanceManager.getClass().getMethod("recreateReactContextInBackground");
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            recreateMethod.invoke(instanceManager);
                            initializeUpdateAfterRestart();
                        }
                        catch (Exception e) {
                            // The recreation method threw an unknown exception
                            // so just simply fallback to restarting the Activity
                            loadBundleLegacy();
                        }
                    }
                });
            }
            catch (Exception e) {
                // Our reflection logic failed somewhere
                // so fall back to restarting the Activity
                loadBundleLegacy();
            }
        }

        @ReactMethod
        public void downloadUpdate(final ReadableMap updatePackage, final boolean notifyProgress, final Promise promise) {
            AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        WritableMap mutableUpdatePackage = CodePushUtils.convertReadableMapToWritableMap(updatePackage);
                        mutableUpdatePackage.putString(BINARY_MODIFIED_TIME_KEY, "" + getBinaryResourcesModifiedTime());
                        codePushPackage.downloadPackage(mutableUpdatePackage, CodePush.this.assetsBundleFileName, new DownloadProgressCallback() {
                            private boolean hasScheduledNextFrame = false;
                            private DownloadProgress latestDownloadProgress = null;

                            @Override
                            public void call(DownloadProgress downloadProgress) {
                                if (!notifyProgress) {
                                    return;
                                }

                                latestDownloadProgress = downloadProgress;
                                // If the download is completed, synchronously send the last event.
                                if (latestDownloadProgress.isCompleted()) {
                                    dispatchDownloadProgressEvent();
                                    return;
                                }

                                if (hasScheduledNextFrame) {
                                    return;
                                }

                                hasScheduledNextFrame = true;
                                getReactApplicationContext().runOnUiQueueThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ReactChoreographer.getInstance().postFrameCallback(ReactChoreographer.CallbackType.TIMERS_EVENTS, new Choreographer.FrameCallback() {
                                            @Override
                                            public void doFrame(long frameTimeNanos) {
                                                if (!latestDownloadProgress.isCompleted()) {
                                                    dispatchDownloadProgressEvent();
                                                }

                                                hasScheduledNextFrame = false;
                                            }
                                        });
                                    }
                                });
                            }

                            public void dispatchDownloadProgressEvent() {
                                getReactApplicationContext()
                                        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                        .emit(DOWNLOAD_PROGRESS_EVENT_NAME, latestDownloadProgress.createWritableMap());
                            }
                        });

                        WritableMap newPackage = codePushPackage.getPackage(CodePushUtils.tryGetString(updatePackage, PACKAGE_HASH_KEY));
                        promise.resolve(newPackage);
                    } catch (IOException e) {
                        e.printStackTrace();
                        promise.reject(e);
                    } catch (CodePushInvalidUpdateException e) {
                        e.printStackTrace();
                        saveFailedUpdate(updatePackage);
                        promise.reject(e);
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
            String binaryHash = CodePushUpdateUtils.getHashForBinaryContents(mainActivity, isDebugMode);
            if (binaryHash != null) {
                // binaryHash will be null if the React Native assets were not bundled into the APK
                // (e.g. in Debug builds)
                configMap.putString(PACKAGE_HASH_KEY, binaryHash);
            }

            promise.resolve(configMap);
        }

        @ReactMethod
        public void getUpdateMetadata(final int updateState, final Promise promise) {
            AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    WritableMap currentPackage = codePushPackage.getCurrentPackage();

                    if (currentPackage == null) {
                        promise.resolve("");
                        return null;
                    }

                    Boolean currentUpdateIsPending = false;

                    if (currentPackage.hasKey(PACKAGE_HASH_KEY)) {
                        String currentHash = currentPackage.getString(PACKAGE_HASH_KEY);
                        currentUpdateIsPending = CodePush.this.isPendingUpdate(currentHash);
                    }

                    if (updateState == CodePushUpdateState.PENDING.getValue() && !currentUpdateIsPending) {
                        // The caller wanted a pending update
                        // but there isn't currently one.
                        promise.resolve("");
                    } else if (updateState == CodePushUpdateState.RUNNING.getValue() && currentUpdateIsPending) {
                        // The caller wants the running update, but the current
                        // one is pending, so we need to grab the previous.
                        promise.resolve(codePushPackage.getPreviousPackage());
                    } else {
                        // The current package satisfies the request:
                        // 1) Caller wanted a pending, and there is a pending update
                        // 2) Caller wanted the running update, and there isn't a pending
                        // 3) Caller wants the latest update, regardless if it's pending or not
                        if (isRunningBinaryVersion) {
                            // This only matters in Debug builds. Since we do not clear "outdated" updates,
                            // we need to indicate to the JS side that somehow we have a current update on
                            // disk that is not actually running.
                            currentPackage.putBoolean("_isDebugOnly", true);
                        }

                        // Enable differentiating pending vs. non-pending updates
                        currentPackage.putBoolean("isPending", currentUpdateIsPending);
                        promise.resolve(currentPackage);
                    }

                    return null;
                }
            };

            asyncTask.execute();
        }

        @ReactMethod
        public void getNewStatusReport(final Promise promise) {
            AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    if (needToReportRollback) {
                        needToReportRollback = false;
                        JSONArray failedUpdates = getFailedUpdates();
                        if (failedUpdates != null && failedUpdates.length() > 0) {
                            try {
                                JSONObject lastFailedPackageJSON = failedUpdates.getJSONObject(failedUpdates.length() - 1);
                                WritableMap lastFailedPackage = CodePushUtils.convertJsonObjectToWritable(lastFailedPackageJSON);
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
        public void installUpdate(final ReadableMap updatePackage, final int installMode, final int minimumBackgroundDuration, final Promise promise) {
            AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    codePushPackage.installPackage(updatePackage, isPendingUpdate(null));

                    String pendingHash = CodePushUtils.tryGetString(updatePackage, PACKAGE_HASH_KEY);
                    if (pendingHash == null) {
                        throw new CodePushUnknownException("Update package to be installed has no hash.");
                    } else {
                        savePendingUpdate(pendingHash, /* isLoading */false);
                    }

                    if (installMode == CodePushInstallMode.ON_NEXT_RESUME.getValue()) {
                        // Store the minimum duration on the native module as an instance
                        // variable instead of relying on a closure below, so that any
                        // subsequent resume-based installs could override it.
                        CodePushNativeModule.this.minimumBackgroundDuration = minimumBackgroundDuration;

                        if (lifecycleEventListener == null) {
                            // Ensure we do not add the listener twice.
                            lifecycleEventListener = new LifecycleEventListener() {
                                private Date lastPausedDate = null;

                                @Override
                                public void onHostResume() {
                                    // Determine how long the app was in the background and ensure
                                    // that it meets the minimum duration amount of time.
                                    long durationInBackground = 0;
                                    if (lastPausedDate != null) {
                                        durationInBackground = (new Date().getTime() - lastPausedDate.getTime()) / 1000;
                                    }

                                    if (durationInBackground >= CodePushNativeModule.this.minimumBackgroundDuration) {
                                        loadBundle();
                                    }
                                }

                                @Override
                                public void onHostPause() {
                                    // Save the current time so that when the app is later
                                    // resumed, we can detect how long it was in the background.
                                    lastPausedDate = new Date();
                                }

                                @Override
                                public void onHostDestroy() {
                                }
                            };

                            getReactApplicationContext().addLifecycleEventListener(lifecycleEventListener);
                        }
                    }

                    promise.resolve("");

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
                    codePushPackage.downloadAndReplaceCurrentBundle(remoteBundleUrl, assetsBundleFileName);
                } catch (IOException e) {
                    throw new CodePushUnknownException("Unable to replace current bundle", e);
                }
            }
        }
    }

    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactApplicationContext) {
        List<NativeModule> nativeModules = new ArrayList<>();
        this.codePushNativeModule = new CodePushNativeModule(reactApplicationContext);
        CodePushDialog dialogModule = new CodePushDialog(reactApplicationContext, mainActivity);

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