package com.microsoft.codepush.react;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Choreographer;

import com.facebook.react.ReactActivity;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.ReactChoreographer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CodePushNativeModule extends ReactContextBaseJavaModule {
    private String mBinaryContentsHash = null;
    private String mClientUniqueId = null;
    private LifecycleEventListener mLifecycleEventListener = null;
    private int mMinimumBackgroundDuration = 0;

    private CodePush mCodePush;
    private SettingsManager mSettingsManager;
    private CodePushTelemetryManager mTelemetryManager;
    private CodePushUpdateManager mUpdateManager;

    private static final String REACT_APPLICATION_CLASS_NAME = "com.facebook.react.ReactApplication";
    private static final String REACT_NATIVE_HOST_CLASS_NAME = "com.facebook.react.ReactNativeHost";

    public CodePushNativeModule(ReactApplicationContext reactContext, CodePush codePush, CodePushUpdateManager codePushUpdateManager, CodePushTelemetryManager codePushTelemetryManager, SettingsManager settingsManager) {
        super(reactContext);

        mCodePush = codePush;
        mSettingsManager = settingsManager;
        mTelemetryManager = codePushTelemetryManager;
        mUpdateManager = codePushUpdateManager;

        // Initialize module state while we have a reference to the current context.
        mBinaryContentsHash = CodePushUpdateUtils.getHashForBinaryContents(reactContext, mCodePush.isDebugMode());
        mClientUniqueId = Settings.Secure.getString(reactContext.getContentResolver(), Settings.Secure.ANDROID_ID);
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
        CodePushUtils.log("Error - doing loadbundlelegacy");

        final Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            // The currentActivity can be null if it is backgrounded / destroyed, so we simply
            // no-op to prevent any null pointer exceptions.
            return;
        }
        mCodePush.invalidateCurrentInstance();

        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                currentActivity.recreate();
            }
        });
    }

    // Use reflection to find and set the appropriate fields on ReactInstanceManager. See #556 for a proposal for a less brittle way
    // to approach this.
    private void setJSBundle(ReactInstanceManager instanceManager, String latestJSBundleFile) throws NoSuchFieldException, IllegalAccessException {
        try {
            Field bundleLoaderField = instanceManager.getClass().getDeclaredField("mBundleLoader");
            Class<?> jsBundleLoaderClass = Class.forName("com.facebook.react.cxxbridge.JSBundleLoader");
            Method createFileLoaderMethod = null;

            Method[] methods = jsBundleLoaderClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals("createFileLoader")) {
                    createFileLoaderMethod = method;
                    break;
                }
            }

            if (createFileLoaderMethod == null) {
                throw new NoSuchMethodException("Could not find a recognized 'createFileLoader' method");
            }

            int numParameters = createFileLoaderMethod.getGenericParameterTypes().length;
            Object latestJSBundleLoader;

            if (numParameters == 1) {
                CodePushUtils.log("RN >= 0.34");
                // RN >= v0.34
                latestJSBundleLoader = createFileLoaderMethod.invoke(jsBundleLoaderClass, latestJSBundleFile);
            } else if (numParameters == 2) {
                CodePushUtils.log("RN < 0.34");

                // RN >= v0.31 && RN < v0.34
                latestJSBundleLoader = createFileLoaderMethod.invoke(jsBundleLoaderClass, getReactApplicationContext(), latestJSBundleFile);
            } else {
                CodePushUtils.log("RN < 0.34");

                throw new NoSuchMethodException("Could not find a recognized 'createFileLoader' method");
            }

            bundleLoaderField.setAccessible(true);
            bundleLoaderField.set(instanceManager, latestJSBundleLoader);
        } catch (Exception e) {
            CodePushUtils.log("Exception 2");

            // RN < v0.31
            Field jsBundleField = instanceManager.getClass().getDeclaredField("mJSBundleFile");
            jsBundleField.setAccessible(true);
            jsBundleField.set(instanceManager, latestJSBundleFile);
        }
    }

    private void loadBundle() {
        mCodePush.clearDebugCacheIfNeeded();
        try {
            CodePushUtils.log("Loading bundle");
            // #1) Get the ReactInstanceManager instance, which is what includes the
            //     logic to reload the current React context.
            final ReactInstanceManager instanceManager = resolveInstanceManager();
            if (instanceManager == null) {
                CodePushUtils.log("Instance manager is null");

                return;
            }

            String latestJSBundleFile = mCodePush.getJSBundleFileInternal(mCodePush.getAssetsBundleFileName());

            // #2) Update the locally stored JS bundle file path
            setJSBundle(instanceManager, latestJSBundleFile);

            // #3) Get the context creation method and fire it on the UI thread (which RN enforces)
            final Method recreateMethod = instanceManager.getClass().getMethod("recreateReactContextInBackground");
            if (recreateMethod == null) {
                CodePushUtils.log("recreate method is null");
            }

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        CodePushUtils.log("Executing recreate method");

                        recreateMethod.invoke(instanceManager);
                        mCodePush.initializeUpdateAfterRestart();
                    } catch (Exception e) {
                        CodePushUtils.log("Recreate method failed");

                        // The recreation method threw an unknown exception
                        // so just simply fallback to restarting the Activity (if it exists)
                        loadBundleLegacy();
                    }
                }
            });

        } catch (Exception e) {
            CodePushUtils.log("Exception");

            // Our reflection logic failed somewhere
            // so fall back to restarting the Activity (if it exists)
            loadBundleLegacy();
        }
    }

    // Use reflection to find the ReactInstanceManager. See #556 for a proposal for a less brittle way to approach this.
    private ReactInstanceManager resolveInstanceManager() throws NoSuchFieldException, IllegalAccessException {
        ReactInstanceManager instanceManager = CodePush.getReactInstanceManager();
        if (instanceManager != null) {
            return instanceManager;
        }

        final Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            return null;
        }
        try {
            // In RN >=0.29, the "mReactInstanceManager" field yields a null value, so we try
            // to get the instance manager via the ReactNativeHost, which only exists in 0.29.
            Method getApplicationMethod = ReactActivity.class.getMethod("getApplication");
            Object reactApplication = getApplicationMethod.invoke(currentActivity);
            Class<?> reactApplicationClass = tryGetClass(REACT_APPLICATION_CLASS_NAME);
            Method getReactNativeHostMethod = reactApplicationClass.getMethod("getReactNativeHost");
            Object reactNativeHost = getReactNativeHostMethod.invoke(reactApplication);
            Class<?> reactNativeHostClass = tryGetClass(REACT_NATIVE_HOST_CLASS_NAME);
            Method getReactInstanceManagerMethod = reactNativeHostClass.getMethod("getReactInstanceManager");
            instanceManager = (ReactInstanceManager)getReactInstanceManagerMethod.invoke(reactNativeHost);
        } catch (Exception e) {
            // The React Native version might be older than 0.29, or the activity does not
            // extend ReactActivity, so we try to get the instance manager via the
            // "mReactInstanceManager" field.
            Class instanceManagerHolderClass = currentActivity instanceof ReactActivity
                    ? ReactActivity.class
                    : currentActivity.getClass();
            Field instanceManagerField = instanceManagerHolderClass.getDeclaredField("mReactInstanceManager");
            instanceManagerField.setAccessible(true);
            instanceManager = (ReactInstanceManager)instanceManagerField.get(currentActivity);
        }
        return instanceManager;
    }

    private Class tryGetClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @ReactMethod
    public void downloadUpdate(final ReadableMap updatePackage, final boolean notifyProgress, final Promise promise) {
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    JSONObject mutableUpdatePackage = CodePushUtils.convertReadableToJsonObject(updatePackage);
                    CodePushUtils.setJSONValueForKey(mutableUpdatePackage, CodePushConstants.BINARY_MODIFIED_TIME_KEY, "" + mCodePush.getBinaryResourcesModifiedTime());
                    mUpdateManager.downloadPackage(mutableUpdatePackage, mCodePush.getAssetsBundleFileName(), new DownloadProgressCallback() {
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
                                    .emit(CodePushConstants.DOWNLOAD_PROGRESS_EVENT_NAME, latestDownloadProgress.createWritableMap());
                        }
                    });

                    JSONObject newPackage = mUpdateManager.getPackage(CodePushUtils.tryGetString(updatePackage, CodePushConstants.PACKAGE_HASH_KEY));
                    promise.resolve(CodePushUtils.convertJsonObjectToWritable(newPackage));
                } catch (IOException e) {
                    e.printStackTrace();
                    promise.reject(e);
                } catch (CodePushInvalidUpdateException e) {
                    e.printStackTrace();
                    mSettingsManager.saveFailedUpdate(CodePushUtils.convertReadableToJsonObject(updatePackage));
                    promise.reject(e);
                }

                return null;
            }
        };

        CodePushUtils.log("Executing download background task");
        asyncTask.execute();
    }

    @ReactMethod
    public void getConfiguration(Promise promise) {
        WritableMap configMap =  Arguments.createMap();
        configMap.putString("appVersion", mCodePush.getAppVersion());
        configMap.putString("clientUniqueId", mClientUniqueId);
        configMap.putString("deploymentKey", mCodePush.getDeploymentKey());
        configMap.putString("serverUrl", mCodePush.getServerUrl());

        // The binary hash may be null in debug builds
        if (mBinaryContentsHash != null) {
            configMap.putString(CodePushConstants.PACKAGE_HASH_KEY, mBinaryContentsHash);
        }

        promise.resolve(configMap);
    }

    @ReactMethod
    public void getUpdateMetadata(final int updateState, final Promise promise) {
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                JSONObject currentPackage = mUpdateManager.getCurrentPackage();

                if (currentPackage == null) {
                    promise.resolve("");
                    return null;
                }

                Boolean currentUpdateIsPending = false;

                if (currentPackage.has(CodePushConstants.PACKAGE_HASH_KEY)) {
                    String currentHash = currentPackage.optString(CodePushConstants.PACKAGE_HASH_KEY, null);
                    currentUpdateIsPending = mSettingsManager.isPendingUpdate(currentHash);
                }

                if (updateState == CodePushUpdateState.PENDING.getValue() && !currentUpdateIsPending) {
                    // The caller wanted a pending update
                    // but there isn't currently one.
                    promise.resolve("");
                } else if (updateState == CodePushUpdateState.RUNNING.getValue() && currentUpdateIsPending) {
                    // The caller wants the running update, but the current
                    // one is pending, so we need to grab the previous.
                    JSONObject previousPackage = mUpdateManager.getPreviousPackage();

                    if (previousPackage == null) {
                        promise.resolve("");
                        return null;
                    }

                    promise.resolve(CodePushUtils.convertJsonObjectToWritable(previousPackage));
                } else {
                    // The current package satisfies the request:
                    // 1) Caller wanted a pending, and there is a pending update
                    // 2) Caller wanted the running update, and there isn't a pending
                    // 3) Caller wants the latest update, regardless if it's pending or not
                    if (mCodePush.isRunningBinaryVersion()) {
                        // This only matters in Debug builds. Since we do not clear "outdated" updates,
                        // we need to indicate to the JS side that somehow we have a current update on
                        // disk that is not actually running.
                        CodePushUtils.setJSONValueForKey(currentPackage, "_isDebugOnly", true);
                    }

                    // Enable differentiating pending vs. non-pending updates
                    CodePushUtils.setJSONValueForKey(currentPackage, "isPending", currentUpdateIsPending);
                    promise.resolve(CodePushUtils.convertJsonObjectToWritable(currentPackage));
                }

                return null;
            }
        };

        CodePushUtils.log("Executing metadata background task");
        asyncTask.execute();
    }

    @ReactMethod
    public void getNewStatusReport(final Promise promise) {
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if (mCodePush.needToReportRollback()) {
                    mCodePush.setNeedToReportRollback(false);
                    JSONArray failedUpdates = mSettingsManager.getFailedUpdates();
                    if (failedUpdates != null && failedUpdates.length() > 0) {
                        try {
                            JSONObject lastFailedPackageJSON = failedUpdates.getJSONObject(failedUpdates.length() - 1);
                            WritableMap lastFailedPackage = CodePushUtils.convertJsonObjectToWritable(lastFailedPackageJSON);
                            WritableMap failedStatusReport = mTelemetryManager.getRollbackReport(lastFailedPackage);
                            if (failedStatusReport != null) {
                                promise.resolve(failedStatusReport);
                                return null;
                            }
                        } catch (JSONException e) {
                            throw new CodePushUnknownException("Unable to read failed updates information stored in SharedPreferences.", e);
                        }
                    }
                } else if (mCodePush.didUpdate()) {
                    JSONObject currentPackage = mUpdateManager.getCurrentPackage();
                    if (currentPackage != null) {
                        WritableMap newPackageStatusReport = mTelemetryManager.getUpdateReport(CodePushUtils.convertJsonObjectToWritable(currentPackage));
                        if (newPackageStatusReport != null) {
                            promise.resolve(newPackageStatusReport);
                            return null;
                        }
                    }
                } else if (mCodePush.isRunningBinaryVersion()) {
                    WritableMap newAppVersionStatusReport = mTelemetryManager.getBinaryUpdateReport(mCodePush.getAppVersion());
                    if (newAppVersionStatusReport != null) {
                        promise.resolve(newAppVersionStatusReport);
                        return null;
                    }
                } else {
                    WritableMap retryStatusReport = mTelemetryManager.getRetryStatusReport();
                    if (retryStatusReport != null) {
                        promise.resolve(retryStatusReport);
                        return null;
                    }
                }

                promise.resolve("");
                return null;
            }
        };

        CodePushUtils.log("Executing report status background task");
        asyncTask.execute();
    }

    @ReactMethod
    public void installUpdate(final ReadableMap updatePackage, final int installMode, final int minimumBackgroundDuration, final Promise promise) {
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mUpdateManager.installPackage(CodePushUtils.convertReadableToJsonObject(updatePackage), mSettingsManager.isPendingUpdate(null));

                String pendingHash = CodePushUtils.tryGetString(updatePackage, CodePushConstants.PACKAGE_HASH_KEY);
                if (pendingHash == null) {
                    throw new CodePushUnknownException("Update package to be installed has no hash.");
                } else {
                    mSettingsManager.savePendingUpdate(pendingHash, /* isLoading */false);
                }

                /**
                 * From LifeCycleEventListener.java:
                 * Called either when the host activity receives a resume event or if the native module that implements this is
                 * initialized while the host activity is already resumed. Always called for the most current activity.
                 *
                 * Not sure, but looks like this might have changed. It seems like the RESUME might be triggered after we kill the thread.
                 * This means:
                 * 1. We are calling loadBundle() twice for IMMEDIATE
                 * 2. We might be restarting at the wrong moment for ON_NEXT_RESUME
                 * 3. React Native might be queuing more stuff onto the UI thread after it's already been killed
                 *
                 * Does this make sense? I'm not sure if "the native module that implements this (LifeCycleEventListener) is initialized
                 * while the host activity is already resumed". TODO: Look at the actual changes to LifeCycleEventListener, and what implements it.
                 *
                 * Observed behavior:
                 * 1. Rollbacks when we leave the resume handler uncommented (and app crash?)
                 * 2. No rollback when we comment it out
                 * 3. Stack trace either way
                 */
                if (installMode == CodePushInstallMode.ON_NEXT_RESUME.getValue() ||
                    // We also add the resume listener if the installMode is IMMEDIATE, because
                    // if the current activity is backgrounded, we want to reload the bundle when
                    // it comes back into the foreground.
                    installMode == CodePushInstallMode.IMMEDIATE.getValue()) {

                    // Store the minimum duration on the native module as an instance
                    // variable instead of relying on a closure below, so that any
                    // subsequent resume-based installs could override it.
                    CodePushNativeModule.this.mMinimumBackgroundDuration = minimumBackgroundDuration;

                    if (mLifecycleEventListener == null) {
                        // Ensure we do not add the listener twice.
                        mLifecycleEventListener = new LifecycleEventListener() {
                            private Date lastPausedDate = null;

                            @Override
                            public void onHostResume() {
                                CodePushUtils.log("Resume handler");
                                // As of RN 36, the resume handler fires immediately if the app is in
                                // the foreground, so explicitly wait for it to be backgrounded first
                                if (lastPausedDate != null) {
                                    CodePushUtils.log("Real resume");
                                    long durationInBackground = (new Date().getTime() - lastPausedDate.getTime()) / 1000;
                                    if (installMode == CodePushInstallMode.IMMEDIATE.getValue()
                                            || durationInBackground >= CodePushNativeModule.this.mMinimumBackgroundDuration) {
                                        CodePushUtils.log("Loading bundle on resume");
                                        loadBundle();
                                    }
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

                        getReactApplicationContext().addLifecycleEventListener(mLifecycleEventListener);
                    }
                }

                promise.resolve("");

                return null;
            }
        };

        CodePushUtils.log("Executing install background task");
        asyncTask.execute();
    }

    @ReactMethod
    public void isFailedUpdate(String packageHash, Promise promise) {
        promise.resolve(mSettingsManager.isFailedHash(packageHash));
    }

    @ReactMethod
    public void isFirstRun(String packageHash, Promise promise) {
        boolean isFirstRun = mCodePush.didUpdate()
                && packageHash != null
                && packageHash.length() > 0
                && packageHash.equals(mUpdateManager.getCurrentPackageHash());
        promise.resolve(isFirstRun);
    }

    @ReactMethod
    public void notifyApplicationReady(Promise promise) {
        mSettingsManager.removePendingUpdate();
        promise.resolve("");
    }

    @ReactMethod
    public void recordStatusReported(ReadableMap statusReport) {
        mTelemetryManager.recordStatusReported(statusReport);
    }

    @ReactMethod
    public void restartApp(boolean onlyIfUpdateIsPending, Promise promise) {
        // If this is an unconditional restart request, or there
        // is current pending update, then reload the app.
        CodePushUtils.log("Restart attempt");
        if (!onlyIfUpdateIsPending || mSettingsManager.isPendingUpdate(null)) {
            CodePushUtils.log("On restart");
            loadBundle();
            promise.resolve(true);
            return;
        }

        promise.resolve(false);
    }

    @ReactMethod
    public void saveStatusReportForRetry(ReadableMap statusReport) {
        mTelemetryManager.saveStatusReportForRetry(statusReport);
    }

    @ReactMethod
    // Replaces the current bundle with the one downloaded from removeBundleUrl.
    // It is only to be used during tests. No-ops if the test configuration flag is not set.
    public void downloadAndReplaceCurrentBundle(String remoteBundleUrl) {
        if (mCodePush.isUsingTestConfiguration()) {
            try {
                mUpdateManager.downloadAndReplaceCurrentBundle(remoteBundleUrl, mCodePush.getAssetsBundleFileName());
            } catch (IOException e) {
                throw new CodePushUnknownException("Unable to replace current bundle", e);
            }
        }
    }
}
