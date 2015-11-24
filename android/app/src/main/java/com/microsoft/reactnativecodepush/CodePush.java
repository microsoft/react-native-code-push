package com.microsoft.reactnativecodepush;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class CodePush {

    private String deploymentKey;

    private boolean resumablePendingUpdateAvailable = false;
    private boolean didUpdate = false;
    private Timer timer;
    private boolean usingTestFolder = false;

    private String assetsBundleFileName;

    private final String FAILED_UPDATES_KEY = "CODE_PUSH_FAILED_UPDATES";
    private final String PENDING_UPDATE_KEY = "CODE_PUSH_PENDING_UPDATE";
    private final String PENDING_UPDATE_HASH_KEY = "hash";
    private final String PENDING_UPDATE_ROLLBACK_TIMEOUT_KEY = "rollbackTimeout";
    private final String ASSETS_BUNDLE_PREFIX = "assets://";
    private final String CODE_PUSH_PREFERENCES = "CodePush";
    private final String DOWNLOAD_PROGRESS_EVENT_NAME = "CodePushDownloadProgress";

    private CodePushPackage codePushPackage;
    private CodePushReactPackage codePushReactPackage;
    private CodePushNativeModule codePushNativeModule;
    private CodePushConfig codePushConfig;

    private Activity mainActivity;
    private Context applicationContext;

    public CodePush(String deploymentKey, Activity mainActivity) {
        SoLoader.init(mainActivity, false);
        this.deploymentKey = deploymentKey;
        this.codePushPackage = new CodePushPackage(mainActivity.getFilesDir().getAbsolutePath());
        this.mainActivity = mainActivity;
        this.applicationContext = mainActivity.getApplicationContext();
        this.codePushConfig = new CodePushConfig(deploymentKey, this.applicationContext);
        checkForPendingUpdate(/*needsRestart*/ false);
    }

    public ReactPackage getReactPackage() {
        if (codePushReactPackage == null) {
            codePushReactPackage = new CodePushReactPackage();
        }
        return codePushReactPackage;
    }

    public String getDocumentsDirectory() {
        return codePushPackage.getDocumentsDirectory();
    }

    public String getBundleUrl(String assetsBundleFileName) {
        this.assetsBundleFileName = assetsBundleFileName;
        String binaryJsBundleUrl = ASSETS_BUNDLE_PREFIX + assetsBundleFileName;
        try {
            String packageFile = codePushPackage.getCurrentPackageBundlePath();
            if (packageFile == null) {
                // There has not been any downloaded updates.
                return binaryJsBundleUrl;
            }

            return packageFile;
        } catch (IOException e) {
            throw new CodePushUnknownException("Error in getting current package bundle path", e);
        }
    }

    private void cancelRollbackTimer() {
        if(timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void checkForPendingUpdate(boolean needsRestart) {
        SharedPreferences settings = applicationContext.getSharedPreferences(CODE_PUSH_PREFERENCES, 0);
        String pendingUpdateString = settings.getString(PENDING_UPDATE_KEY, null);

        if (pendingUpdateString != null) {
            try {
                JSONObject pendingUpdateJSON = new JSONObject(pendingUpdateString);
                String pendingHash = pendingUpdateJSON.getString(PENDING_UPDATE_HASH_KEY);
                String currentHash = codePushPackage.getCurrentPackageHash();
                if (pendingHash.equals(currentHash)) {
                    int rollbackTimeout = pendingUpdateJSON.getInt(PENDING_UPDATE_ROLLBACK_TIMEOUT_KEY);
                    initializeUpdateWithRollbackTimeout(rollbackTimeout, needsRestart);

                    settings.edit().remove(PENDING_UPDATE_KEY).commit();
                }
            } catch (JSONException e) {
                // Should not happen.
                throw new CodePushUnknownException("Unable to parse pending update metadata " +
                        pendingUpdateString + " stored in SharedPreferences", e);
            } catch (IOException e) {
                // There is no current package hash.
                throw new CodePushUnknownException("Should not register a pending update without a saving a current package", e);
            }
        }
    }

    private void checkForPendingUpdateDuringResume() {
        if (resumablePendingUpdateAvailable) {
            checkForPendingUpdate(/*needsRestart*/ true);
        }
    }

    private WritableMap constantsToExport() {
        // Export the values of the CodePushInstallMode enum
        // so that the script-side can easily stay in sync
        WritableMap map = new WritableNativeMap();
        map.putInt("codePushInstallModeImmediate", CodePushInstallMode.IMMEDIATE.getValue());
        map.putInt("codePushInstallModeOnNextRestart", CodePushInstallMode.ON_NEXT_RESTART.getValue());
        map.putInt("codePushInstallModeOnNextResume", CodePushInstallMode.ON_NEXT_RESUME.getValue());
        return map;
    }

    private void initializeUpdateWithRollbackTimeout(int rollbackTimeout, boolean needsRestart) {
        didUpdate = true;

        if (needsRestart) {
            codePushNativeModule.loadBundle();
        }

        if (0 != rollbackTimeout) {
            startRollbackTimer(rollbackTimeout);
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

        codePushNativeModule.loadBundle();
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

    private void savePendingUpdate(String packageHash, int rollbackTimeout) {
        SharedPreferences settings = applicationContext.getSharedPreferences(CODE_PUSH_PREFERENCES, 0);
        JSONObject pendingUpdate = new JSONObject();
        try {
            pendingUpdate.put(PENDING_UPDATE_HASH_KEY, packageHash);
            pendingUpdate.put(PENDING_UPDATE_ROLLBACK_TIMEOUT_KEY, rollbackTimeout);
            settings.edit().putString(PENDING_UPDATE_KEY, pendingUpdate.toString()).commit();
        } catch (JSONException e) {
            // Should not happen.
            throw new CodePushUnknownException("Unable to save pending update.", e);
        }

    }

    private void startRollbackTimer(int rollbackTimeout) {
        timer = new Timer();
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.MILLISECOND, rollbackTimeout);
        Date timeout = c.getTime();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                rollbackPackage();
            }
        }, timeout);
    }

    private class CodePushNativeModule extends ReactContextBaseJavaModule {

        private void loadBundle() {
            String assetsBundleFileUrl = CodePush.this.getBundleUrl(CodePush.this.assetsBundleFileName);
            Intent intent = mainActivity.getIntent();
            mainActivity.finish();
            mainActivity.startActivity(intent);
        }

        @ReactMethod
        public void installUpdate(ReadableMap updatePackage, int rollbackTimeout, int installMode,
                                  Callback resolve, Callback reject) {
            try {
                codePushPackage.installPackage(updatePackage);
                if (installMode != CodePushInstallMode.IMMEDIATE.getValue()) {
                    resumablePendingUpdateAvailable = installMode == CodePushInstallMode.ON_NEXT_RESUME.getValue();
                    String pendingHash = CodePushUtils.tryGetString(updatePackage, codePushPackage.PACKAGE_HASH_KEY);
                    if (pendingHash == null) {
                        throw new CodePushUnknownException("Update package to be installed has no hash.");
                    } else {
                        savePendingUpdate(pendingHash, rollbackTimeout);
                    }
                }
                resolve.invoke("");
            } catch (IOException e) {
                e.printStackTrace();
                reject.invoke(e.getMessage());
            }
        }

        @ReactMethod
        public void downloadUpdate(final ReadableMap updatePackage, final Callback resolve, final Callback reject) {
            try {
                codePushPackage.downloadPackage(applicationContext, updatePackage, new DownloadProgressCallback() {
                    @Override
                    public void call(DownloadProgress downloadProgress) {
                        getReactApplicationContext()
                                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                .emit(DOWNLOAD_PROGRESS_EVENT_NAME, downloadProgress.createWritableMap());
                    }
                });

                WritableMap newPackage = codePushPackage.getPackage(CodePushUtils.tryGetString(updatePackage, codePushPackage.PACKAGE_HASH_KEY));
                resolve.invoke(newPackage);
            } catch (IOException e) {
                e.printStackTrace();
                reject.invoke(e.getMessage());
            }
        }

        @ReactMethod
        public void getConfiguration(Callback resolve, Callback reject) {
            resolve.invoke(codePushConfig.getConfiguration());
        }

        @ReactMethod
        public void getCurrentPackage(Callback resolve, Callback reject) {
            try {
                resolve.invoke(codePushPackage.getCurrentPackage());
            } catch (IOException e) {
                e.printStackTrace();
                reject.invoke(e.getMessage());
            }
        }

        @ReactMethod
        public void isFailedUpdate(String packageHash, Callback resolve, Callback reject) {
            resolve.invoke(isFailedHash(packageHash));
        }

        @ReactMethod
        public void isFirstRun(String packageHash, Callback resolve, Callback reject) {
            try {
                boolean isFirstRun = didUpdate
                        && packageHash != null
                        && packageHash.length() > 0
                        && packageHash.equals(codePushPackage.getCurrentPackageHash());
                resolve.invoke(isFirstRun);
            } catch (IOException e) {
                e.printStackTrace();
                reject.invoke(e.getMessage());
            }
        }

        @ReactMethod
        public void notifyApplicationReady(Callback resolve, Callback reject) {
            cancelRollbackTimer();
            resolve.invoke("");
        }

        @ReactMethod
        public void setUsingTestFolder(boolean shouldUseTestFolder) {
            usingTestFolder = shouldUseTestFolder;
        }

        @ReactMethod
        public void restartImmediateUpdate(int rollbackTimeout) {
            initializeUpdateWithRollbackTimeout(rollbackTimeout, /*needsRestart*/ true);
        }

        @ReactMethod
        public void restartPendingUpdate() {
            checkForPendingUpdate(/*needsRestart*/ true);
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

            reactApplicationContext.addLifecycleEventListener(new LifecycleEventListener() {
                @Override
                public void onHostResume() {
                    CodePush.this.checkForPendingUpdateDuringResume();
                }

                @Override
                public void onHostPause() {
                }

                @Override
                public void onHostDestroy() {
                }
            });

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

