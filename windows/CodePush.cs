using System;
using System.Collections.Generic;
using ReactNative.Bridge;
using ReactNative.Modules.Core;
using ReactNative.UIManager;
using Windows.ApplicationModel;
using Windows.Storage;
using System.IO;
using System.Threading.Tasks;
using Newtonsoft.Json.Linq;
using Newtonsoft.Json;
using System.Reflection;
using Windows.Web.Http;
using Windows.Storage.FileProperties;
using ReactNative;

namespace CodePush.ReactNative
{
    public class CodePushModule : IReactPackage
    {
        private static bool needToReportRollback = false;
        private static bool isRunningBinaryVersion = false;
        private static bool testConfigurationFlag = false;

        private bool didUpdate = false;

        private string assetsBundleFileName;

        private static readonly string ASSETS_BUNDLE_PREFIX = "ms-appx:///ReactAssets/";
        private static readonly string BINARY_MODIFIED_TIME_KEY = "binaryModifiedTime";
        private readonly string CODE_PUSH_PREFERENCES = "CodePush";
        private static readonly string DEFAULT_JS_BUNDLE_NAME = "index.windows.bundle";
        private readonly string DOWNLOAD_PROGRESS_EVENT_NAME = "CodePushDownloadProgress";
        private readonly string FAILED_UPDATES_KEY = "CODE_PUSH_FAILED_UPDATES";
        private static readonly string FILE_BUNDLE_PREFIX = "ms-appdata:///local";
        private readonly string PACKAGE_HASH_KEY = "packageHash";
        private readonly string PENDING_UPDATE_HASH_KEY = "hash";
        private readonly string PENDING_UPDATE_IS_LOADING_KEY = "isLoading";
        private readonly string PENDING_UPDATE_KEY = "CODE_PUSH_PENDING_UPDATE";

        // This needs to be kept in sync with https://github.com/facebook/react-native/blob/master/ReactAndroid/src/main/java/com/facebook/react/devsupport/DevSupportManager.java#L78
        private readonly string REACT_DEV_BUNDLE_CACHE_FILE_NAME = "ReactNativeDevBundle.js";

        // Helper classes.
        private CodePushNativeModule codePushNativeModule;
        private CodePushPackage codePushPackage;

        // Config properties.
        private string appVersion;
        private string deploymentKey;
        private readonly string serverUrl = "https://codepush.azurewebsites.net/";

        private ReactPage mainPage;

        private static CodePushModule currentInstance;

        public CodePushModule(string deploymentKey, ReactPage mainPage)
        {
            codePushPackage = new CodePushPackage();
            // TODO implement telemetryManager 
            // this.codePushTelemetryManager = new CodePushTelemetryManager(this.applicationContext, CODE_PUSH_PREFERENCES);
            this.deploymentKey = deploymentKey;
            this.mainPage = mainPage;
            appVersion = Package.Current.Id.Version.Major + "." + Package.Current.Id.Version.Minor + "." + Package.Current.Id.Version.Build;
            InitializeUpdateAfterRestart();
            if (currentInstance != null)
            {
                CodePushUtils.log("More than one CodePush instance has been initialized. Please use the instance method codePush.getBundleUrlInternal() to get the correct bundleURL for a particular instance.");
            }

            currentInstance = this;
        }

        private async Task ClearReactDevBundleCache()
        {
            StorageFile devBundleCacheFile = null;
            try
            {
                devBundleCacheFile = await ApplicationData.Current.LocalFolder.GetFileAsync(REACT_DEV_BUNDLE_CACHE_FILE_NAME);
            }
            catch (FileNotFoundException)
            {
            }

            if (devBundleCacheFile != null)
            {
                await devBundleCacheFile.DeleteAsync();
            }
        }

        private async Task<long> GetBinaryResourcesModifiedTime()
        {
            StorageFile assetJSBundleFile = await StorageFile.GetFileFromApplicationUriAsync(new Uri(ASSETS_BUNDLE_PREFIX + assetsBundleFileName));
            BasicProperties fileProperties = await assetJSBundleFile.GetBasicPropertiesAsync();
            return fileProperties.DateModified.ToUnixTimeMilliseconds();
        }

        public string GetJavaScriptBundleFile()
        {
            return GetJavaScriptBundleFile(DEFAULT_JS_BUNDLE_NAME);
        }

        public string GetJavaScriptBundleFile(string assetsBundleFileName)
        {
            if (currentInstance == null)
            {
                throw new CodePushNotInitializedException("A CodePush instance has not been created yet. Have you added it to your app's list of ReactPackages?");
            }

            return currentInstance.GetJavaScriptBundleFileAsync(assetsBundleFileName).Result;
        }

        public async Task<string> GetJavaScriptBundleFileAsync(string assetsBundleFileName)
        {
            this.assetsBundleFileName = assetsBundleFileName;
            string binaryJsBundleUrl = ASSETS_BUNDLE_PREFIX + assetsBundleFileName;
            long binaryResourcesModifiedTime = await GetBinaryResourcesModifiedTime();
            StorageFile packageFile = await codePushPackage.GetCurrentPackageBundle(this.assetsBundleFileName);
            if (packageFile == null)
            {
                // There has not been any downloaded updates.
                CodePushUtils.logBundleUrl(binaryJsBundleUrl);
                isRunningBinaryVersion = true;
                return binaryJsBundleUrl;
            }

            JObject packageMetadata = await codePushPackage.GetCurrentPackage();
            long? binaryModifiedDateDuringPackageInstall = null;
            string binaryModifiedDateDuringPackageInstallString = (string)packageMetadata[BINARY_MODIFIED_TIME_KEY];
            if (binaryModifiedDateDuringPackageInstallString != null)
            {
                binaryModifiedDateDuringPackageInstall = long.Parse(binaryModifiedDateDuringPackageInstallString);
            }

            string packageAppVersion = (string)packageMetadata["appVersion"];

            // TODO: test configuration
            if (binaryModifiedDateDuringPackageInstall != null &&
                    binaryModifiedDateDuringPackageInstall == binaryResourcesModifiedTime &&
                    (IsUsingTestConfiguration() || appVersion.Equals(packageAppVersion)))
            {
                CodePushUtils.logBundleUrl(packageFile.Path);
                isRunningBinaryVersion = false;
                return FILE_BUNDLE_PREFIX + packageFile.Path.Replace(ApplicationData.Current.LocalFolder.Path, "").Replace("\\", "/");
            }
            else
            {
                // The binary version is newer.
                didUpdate = false;
                if (!mainPage.UseDeveloperSupport || !appVersion.Equals(packageAppVersion))
                {
                    await ClearUpdates();
                }

                CodePushUtils.logBundleUrl(binaryJsBundleUrl);
                isRunningBinaryVersion = true;
                return binaryJsBundleUrl;
            }
        }

        private ApplicationDataContainer GetCodePushSettings()
        {
            return ApplicationData.Current.LocalSettings.CreateContainer(CODE_PUSH_PREFERENCES, ApplicationDataCreateDisposition.Always);
        }

        private JArray GetFailedUpdates()
        {
            ApplicationDataContainer settings = GetCodePushSettings();
            string failedUpdatesString = (string)settings.Values[FAILED_UPDATES_KEY];
            if (failedUpdatesString == null)
            {
                return new JArray();
            }

            try
            {
                return JArray.Parse(failedUpdatesString);
            }
            catch (Exception)
            {
                JArray emptyArray = new JArray();
                settings.Values[FAILED_UPDATES_KEY] = JsonConvert.SerializeObject(emptyArray);
                return emptyArray;
            }
        }

        private JObject GetPendingUpdate()
        {
            ApplicationDataContainer settings = GetCodePushSettings();
            string pendingUpdateString = (string)settings.Values[PENDING_UPDATE_KEY];
            if (pendingUpdateString == null)
            {
                return null;
            }

            try
            {
                return JObject.Parse(pendingUpdateString);
            }
            catch (Exception)
            {
                // Should not happen.
                CodePushUtils.log("Unable to parse pending update metadata " + pendingUpdateString +
                        " stored in SharedPreferences");
                return null;
            }
        }

        private void InitializeUpdateAfterRestart()
        {
            JObject pendingUpdate = GetPendingUpdate();
            if (pendingUpdate != null)
            {
                didUpdate = true;
                bool updateIsLoading = (bool)pendingUpdate[PENDING_UPDATE_IS_LOADING_KEY];
                if (updateIsLoading)
                {
                    // Pending update was initialized, but notifyApplicationReady was not called.
                    // Therefore, deduce that it is a broken update and rollback.
                    CodePushUtils.log("Update did not finish loading the last time, rolling back to a previous version.");
                    needToReportRollback = true;
                    RollbackPackage().Wait();
                }
                else
                {
                    // Clear the React dev bundle cache so that new updates can be loaded.
                    if (mainPage.UseDeveloperSupport)
                    {
                        ClearReactDevBundleCache().Wait();
                    }
                    // Mark that we tried to initialize the new update, so that if it crashes,
                    // we will know that we need to rollback when the app next starts.
                    SavePendingUpdate((string)pendingUpdate[PENDING_UPDATE_HASH_KEY], /* isLoading */true);
                }
            }
        }

        private bool IsFailedHash(string packageHash)
        {
            JArray failedUpdates = GetFailedUpdates();
            if (packageHash != null)
            {
                foreach (JObject failedPackage in failedUpdates)
                {
                    string failedPackageHash = (string)failedPackage[PACKAGE_HASH_KEY];
                    if (packageHash.Equals(failedPackageHash))
                    {
                        return true;
                    }
                }
            }

            return false;
        }

        private bool IsPendingUpdate(string packageHash)
        {
            JObject pendingUpdate = GetPendingUpdate();
            return pendingUpdate != null &&
                    !(bool)pendingUpdate[PENDING_UPDATE_IS_LOADING_KEY] &&
                    (packageHash == null || ((string)pendingUpdate[PENDING_UPDATE_HASH_KEY]).Equals(packageHash));
        }

        private void RemoveFailedUpdates()
        {
            ApplicationDataContainer settings = GetCodePushSettings();
            settings.Values.Remove(FAILED_UPDATES_KEY);
        }

        private void RemovePendingUpdate()
        {
            ApplicationDataContainer settings = GetCodePushSettings();
            settings.Values.Remove(PENDING_UPDATE_KEY);
        }

        private async Task RollbackPackage()
        {
            JObject failedPackage = await codePushPackage.GetCurrentPackage();
            SaveFailedUpdate(failedPackage);
            await codePushPackage.RollbackPackage();
            RemovePendingUpdate();
        }

        private void SaveFailedUpdate(JObject failedPackage)
        {
            ApplicationDataContainer settings = GetCodePushSettings();
            string failedUpdatesString = (string)settings.Values[FAILED_UPDATES_KEY];
            JArray failedUpdates;
            if (failedUpdatesString == null)
            {
                failedUpdates = new JArray();
            }
            else
            {
                failedUpdates = JArray.Parse(failedUpdatesString);
            }

            failedUpdates.Add(failedPackage);
            settings.Values[FAILED_UPDATES_KEY] = JsonConvert.SerializeObject(failedUpdates);
        }

        private void SavePendingUpdate(string packageHash, bool isLoading)
        {
            ApplicationDataContainer settings = GetCodePushSettings();
            JObject pendingUpdate = new JObject();
            pendingUpdate[PENDING_UPDATE_HASH_KEY] = packageHash;
            pendingUpdate[PENDING_UPDATE_IS_LOADING_KEY] = isLoading;
            settings.Values[PENDING_UPDATE_KEY] = JsonConvert.SerializeObject(pendingUpdate);
        }

        public static bool IsUsingTestConfiguration()
        {
            return testConfigurationFlag;
        }

        public static void SetUsingTestConfiguration(bool shouldUseTestConfiguration)
        {
            testConfigurationFlag = shouldUseTestConfiguration;
        }

        public async Task ClearUpdates()
        {
            await codePushPackage.ClearUpdates();
            RemovePendingUpdate();
            RemoveFailedUpdates();
        }

        private class CodePushNativeModule : ReactContextNativeModuleBase
        {
            private ILifecycleEventListener lifecycleEventListener = null;
            private int minimumBackgroundDuration = 0;
            private ReactContext reactContext;

            private class CodePushResumeListener : ILifecycleEventListener
            {
                private DateTime? lastPausedDate = null;
                private CodePushNativeModule codePushNativeModule;

                public CodePushResumeListener(CodePushNativeModule codePushNativeModule)
                {
                    this.codePushNativeModule = codePushNativeModule;
                }

                public void OnDestroy()
                {
                }

                public void OnResume()
                {
                    if (lastPausedDate != null)
                    {
                        // Determine how long the app was in the background and ensure
                        // that it meets the minimum duration amount of time.
                        double durationInBackground = (new DateTime() - (DateTime)lastPausedDate).TotalSeconds;
                        if (durationInBackground >= codePushNativeModule.minimumBackgroundDuration)
                        {
                            Action loadBundleAction = async () =>
                            {
                                await codePushNativeModule.LoadBundle();
                            };

                            codePushNativeModule.Context.RunOnNativeModulesQueueThread(loadBundleAction);
                        }
                    }
                }

                public void OnSuspend()
                {
                    // Save the current time so that when the app is later
                    // resumed, we can detect how long it was in the background.
                    lastPausedDate = new DateTime();
                }
            }

            // TODO get rid of this
            private CodePushModule codePush;

            public CodePushNativeModule(ReactContext reactContext, CodePushModule codePush) : base(reactContext)
            {
                this.reactContext = reactContext;
                this.codePush = codePush;
            }

            public override string Name
            {
                get
                {
                    return "CodePush";
                }
            }

            public override IReadOnlyDictionary<string, object> Constants
            {
                get
                {
                    Dictionary<string, object> constants = new Dictionary<string, object>();
                    constants["codePushInstallModeImmediate"] = CodePushInstallMode.IMMEDIATE;
                    constants["codePushInstallModeOnNextRestart"] = CodePushInstallMode.ON_NEXT_RESTART;
                    constants["codePushInstallModeOnNextResume"] = CodePushInstallMode.ON_NEXT_RESUME;
                    return constants;
                }
            }

            public override void Initialize()
            {
                codePush.InitializeUpdateAfterRestart();
            }

            private async Task LoadBundle()
            {
                // #1) Get the private ReactInstanceManager, which is what includes
                //     the logic to reload the current React context.
                FieldInfo info = typeof(ReactPage)
                    .GetField("_reactInstanceManager", BindingFlags.NonPublic | BindingFlags.Instance);

                ReactInstanceManager reactInstanceManager = (ReactInstanceManager)typeof(ReactPage)
                    .GetField("_reactInstanceManager", BindingFlags.NonPublic | BindingFlags.Instance)
                    .GetValue(codePush.mainPage);

                // #2) Update the locally stored JS bundle file path
                Type reactInstanceManagerType = typeof(ReactInstanceManager);
                string latestJSBundleFile = await codePush.GetJavaScriptBundleFileAsync(codePush.assetsBundleFileName);
                reactInstanceManagerType
                    .GetField("_jsBundleFile", BindingFlags.NonPublic | BindingFlags.Instance)
                    .SetValue(reactInstanceManager, latestJSBundleFile);

                // #3) Get the context creation method and fire it on the UI thread (which RN enforces)
                Action recreateReactContextAction = () =>
                {
                    reactInstanceManager.RecreateReactContextInBackground();
                };
                Context.RunOnDispatcherQueueThread(recreateReactContextAction);
            }

            [ReactMethod]
            public void downloadUpdate(JObject updatePackage, IPromise promise)
            {
                Action downloadAction = async () =>
                {
                    try
                    {
                        updatePackage[BINARY_MODIFIED_TIME_KEY] = "" + await codePush.GetBinaryResourcesModifiedTime();
                        await codePush.codePushPackage.DownloadPackage(
                            updatePackage,
                            codePush.assetsBundleFileName,
                            new Progress<HttpProgress>(
                                (HttpProgress progress) =>
                                {
                                    JObject downloadProgress = new JObject();
                                    downloadProgress["totalBytes"] = progress.TotalBytesToReceive;
                                    downloadProgress["receivedBytes"] = progress.BytesReceived;
                                    reactContext
                                        .GetJavaScriptModule<RCTDeviceEventEmitter>()
                                        .emit(codePush.DOWNLOAD_PROGRESS_EVENT_NAME, downloadProgress);
                                }
                            )
                        );

                        JObject newPackage = await codePush.codePushPackage.GetPackage((string)updatePackage[codePush.PACKAGE_HASH_KEY]);
                        promise.Resolve(newPackage);
                    }
                    catch (CodePushInvalidUpdateException e)
                    {
                        CodePushUtils.log(e.ToString());
                        codePush.SaveFailedUpdate(updatePackage);
                        promise.Reject(e);
                    }
                    catch (Exception e)
                    {
                        CodePushUtils.log(e.ToString());
                        promise.Reject(e);
                    }
                };

                Context.RunOnNativeModulesQueueThread(downloadAction);
            }

            [ReactMethod]
            public void getConfiguration(IPromise promise)
            {
                JObject config = new JObject();
                config["appVersion"] = codePush.appVersion;
                config["deploymentKey"] = codePush.deploymentKey;
                config["serverUrl"] = codePush.serverUrl;
                config["clientUniqueId"] = CodePushUtils.GetDeviceId();
                // TODO generate binary hash
                // string binaryHash = CodePushUpdateUtils.getHashForBinaryContents(mainActivity, isDebugMode);
                /*if (binaryHash != null)
                {
                    // binaryHash will be null if the React Native assets were not bundled into the APK
                    // (e.g. in Debug builds)
                    configMap.putString(PACKAGE_HASH_KEY, binaryHash);
                }*/

                promise.Resolve(config);
            }

            [ReactMethod]
            public void getCurrentPackage(IPromise promise)
            {
                Action getCurrentPackageAction = async () =>
                {
                    JObject currentPackage = await codePush.codePushPackage.GetCurrentPackage();
                    if (currentPackage == null)
                    {
                        promise.Resolve("");
                        return;
                    }

                    if (isRunningBinaryVersion)
                    {
                        currentPackage["_isDebugOnly"] = true;
                    }

                    bool isPendingUpdate = false;
                    string currentHash = (string)currentPackage[codePush.PACKAGE_HASH_KEY];
                    if (currentHash != null)
                    {
                        isPendingUpdate = codePush.IsPendingUpdate(currentHash);
                    }

                    currentPackage["isPending"] = isPendingUpdate;
                    promise.Resolve(currentPackage);
                };

                Context.RunOnNativeModulesQueueThread(getCurrentPackageAction);
            }


            [ReactMethod]
            public void getNewStatusReport(IPromise promise)
            {
                // TODO implement this
                promise.Resolve("");
            }

            [ReactMethod]
            public void installUpdate(JObject updatePackage, int installMode, int minimumBackgroundDuration, IPromise promise)
            {
                Action installUpdateAction = async () =>
                {
                    await codePush.codePushPackage.InstallPackage(updatePackage, codePush.IsPendingUpdate(null));
                    string pendingHash = (string)updatePackage[codePush.PACKAGE_HASH_KEY];
                    codePush.SavePendingUpdate(pendingHash, /* isLoading */false);
                    if (installMode == (int)CodePushInstallMode.ON_NEXT_RESUME)
                    {
                        // Store the minimum duration on the native module as an instance
                        // variable instead of relying on a closure below, so that any
                        // subsequent resume-based installs could override it.
                        codePush.codePushNativeModule.minimumBackgroundDuration = minimumBackgroundDuration;

                        if (lifecycleEventListener == null)
                        {
                            // Ensure we do not add the listener twice.
                            lifecycleEventListener = new CodePushResumeListener(this);
                            reactContext.AddLifecycleEventListener(lifecycleEventListener);
                        }
                    }

                    promise.Resolve("");
                };

                Context.RunOnNativeModulesQueueThread(installUpdateAction);
            }

            [ReactMethod]
            public void isFailedUpdate(string packageHash, IPromise promise)
            {
                promise.Resolve(codePush.IsFailedHash(packageHash));
            }

            [ReactMethod]
            public void isFirstRun(string packageHash, IPromise promise)
            {
                Action isFirstRunAction = async () =>
                {
                    bool isFirstRun = codePush.didUpdate
                        && packageHash != null
                        && packageHash.Length > 0
                        && packageHash.Equals(await codePush.codePushPackage.GetCurrentPackageHash());
                    promise.Resolve(isFirstRun);
                };

                Context.RunOnNativeModulesQueueThread(isFirstRunAction);
            }

            [ReactMethod]
            public void notifyApplicationReady(IPromise promise)
            {
                codePush.RemovePendingUpdate();
                promise.Resolve("");
            }

            [ReactMethod]
            public void restartApp(bool onlyIfUpdateIsPending)
            {
                Action restartAppAction = async () =>
                {
                    // If this is an unconditional restart request, or there
                    // is current pending update, then reload the app.
                    if (!onlyIfUpdateIsPending || codePush.IsPendingUpdate(null))
                    {
                        await LoadBundle();
                    }
                };

                Context.RunOnNativeModulesQueueThread(restartAppAction);
            }

            [ReactMethod]
            // Replaces the current bundle with the one downloaded from removeBundleUrl.
            // It is only to be used during tests. No-ops if the test configuration flag is not set.
            public void downloadAndReplaceCurrentBundle(String remoteBundleUrl)
            {
                // TODO implement this
            }
        }

        public IReadOnlyList<Type> CreateJavaScriptModulesConfig()
        {
            return new List<Type>();
        }

        public IReadOnlyList<INativeModule> CreateNativeModules(ReactContext reactContext)
        {
            List<INativeModule> nativeModules = new List<INativeModule>();
            codePushNativeModule = new CodePushNativeModule(reactContext, this);
            //CodePushDialog dialogModule = new CodePushDialog(reactApplicationContext, mainActivity);

            nativeModules.Add(codePushNativeModule);
            //nativeModules.add(dialogModule);

            return nativeModules;
        }

        public IReadOnlyList<IViewManager> CreateViewManagers(ReactContext reactContext)
        {
            return new List<IViewManager>();
        }
    }
}