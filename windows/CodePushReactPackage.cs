using Newtonsoft.Json.Linq;
using Newtonsoft.Json;
using ReactNative;
using ReactNative.Bridge;
using ReactNative.Modules.Core;
using ReactNative.UIManager;
using System;
using System.Collections.Generic;
using System.Reflection;
using System.Threading.Tasks;
using Windows.ApplicationModel;
using Windows.Storage;
using Windows.Web.Http;

namespace CodePush.ReactNative
{
    public sealed class CodePushReactPackage : IReactPackage
    {
        private static CodePushReactPackage CurrentInstance;
        private static bool NeedToReportRollback = false;

        private CodePushNativeModule _codePushNativeModule;

        internal string AppVersion { get; private set; }
        internal string DeploymentKey { get; private set; }
        internal string AssetsBundleFileName { get; private set; }
        internal bool DidUpdate { get; private set; }
        internal bool IsRunningBinaryVersion { get; set; }
        internal ReactPage MainPage { get; private set; }
        internal UpdateManager UpdateManager { get; private set; }

        public CodePushReactPackage(string deploymentKey, ReactPage mainPage)
        {
            AppVersion = Package.Current.Id.Version.Major + "." + Package.Current.Id.Version.Minor + "." + Package.Current.Id.Version.Build;
            DeploymentKey = deploymentKey;
            MainPage = mainPage;
            UpdateManager = new UpdateManager();
            IsRunningBinaryVersion = false;
            // TODO implement telemetryManager 
            // _codePushTelemetryManager = new CodePushTelemetryManager(this.applicationContext, CODE_PUSH_PREFERENCES);

            if (CurrentInstance != null)
            {
                CodePushUtils.Log("More than one CodePush instance has been initialized. Please use the instance method codePush.getBundleUrlInternal() to get the correct bundleURL for a particular instance.");
            }

            CurrentInstance = this;
        }

        #region Public methods
        public IReadOnlyList<Type> CreateJavaScriptModulesConfig()
        {
            return new List<Type>();
        }

        public IReadOnlyList<INativeModule> CreateNativeModules(ReactContext reactContext)
        {
            return new List<INativeModule>
            {
                 new CodePushNativeModule(reactContext, this)
            };
        }

        public IReadOnlyList<IViewManager> CreateViewManagers(ReactContext reactContext)
        {
            return new List<IViewManager>();
        }

        public string GetJavaScriptBundleFile()
        {
            return GetJavaScriptBundleFile(CodePushConstants.DefaultJsBundleName);
        }

        public string GetJavaScriptBundleFile(string assetsBundleFileName)
        {
            if (CurrentInstance == null)
            {
                throw new InvalidOperationException("A CodePush instance has not been created yet. Have you added it to your app's list of ReactPackages?");
            }

            return CurrentInstance.GetJavaScriptBundleFileAsync(assetsBundleFileName).Result;
        }

        public async Task<string> GetJavaScriptBundleFileAsync(string assetsBundleFileName)
        {
            AssetsBundleFileName = assetsBundleFileName;
            string binaryJsBundleUrl = CodePushConstants.AssetsBundlePrefix + assetsBundleFileName;
            var binaryResourcesModifiedTime = await GetBinaryResourcesModifiedTimeAsync().ConfigureAwait(false);
            var packageFile = await UpdateManager.GetCurrentPackageBundleAsync(AssetsBundleFileName).ConfigureAwait(false);
            if (packageFile == null)
            {
                // There has not been any downloaded updates.
                CodePushUtils.LogBundleUrl(binaryJsBundleUrl);
                IsRunningBinaryVersion = true;
                return binaryJsBundleUrl;
            }

            var packageMetadata = await UpdateManager.GetCurrentPackageAsync().ConfigureAwait(false);
            long? binaryModifiedDateDuringPackageInstall = null;
            var binaryModifiedDateDuringPackageInstallString = (string)packageMetadata[CodePushConstants.BinaryModifiedTimeKey];
            if (binaryModifiedDateDuringPackageInstallString != null)
            {
                binaryModifiedDateDuringPackageInstall = long.Parse(binaryModifiedDateDuringPackageInstallString);
            }

            var packageAppVersion = (string)packageMetadata["appVersion"];

            if (binaryModifiedDateDuringPackageInstall != null &&
                    binaryModifiedDateDuringPackageInstall == binaryResourcesModifiedTime &&
                    AppVersion.Equals(packageAppVersion))
            {
                CodePushUtils.LogBundleUrl(packageFile.Path);
                IsRunningBinaryVersion = false;
                return CodePushConstants.FileBundlePrefix + packageFile.Path.Replace(ApplicationData.Current.LocalFolder.Path, "").Replace("\\", "/");
            }
            else
            {
                // The binary version is newer.
                DidUpdate = false;
                if (!MainPage.UseDeveloperSupport || !AppVersion.Equals(packageAppVersion))
                {
                    await ClearUpdatesAsync().ConfigureAwait(false);
                }

                CodePushUtils.LogBundleUrl(binaryJsBundleUrl);
                IsRunningBinaryVersion = true;
                return binaryJsBundleUrl;
            }
        }

        #endregion

        #region Internal methods

        internal async Task<long> GetBinaryResourcesModifiedTimeAsync()
        {
            var assetJSBundleFile = await StorageFile.GetFileFromApplicationUriAsync(new Uri(CodePushConstants.AssetsBundlePrefix + AssetsBundleFileName)).AsTask().ConfigureAwait(false);
            var fileProperties = await assetJSBundleFile.GetBasicPropertiesAsync().AsTask().ConfigureAwait(false);
            return fileProperties.DateModified.ToUnixTimeMilliseconds();
        }
        
        internal void InitializeUpdateAfterRestart()
        {
            JObject pendingUpdate = SettingsManager.GetPendingUpdate();
            if (pendingUpdate != null)
            {
                var updateIsLoading = (bool)pendingUpdate[CodePushConstants.PendingUpdateIsLoadingKey];
                if (updateIsLoading)
                {
                    // Pending update was initialized, but notifyApplicationReady was not called.
                    // Therefore, deduce that it is a broken update and rollback.
                    CodePushUtils.Log("Update did not finish loading the last time, rolling back to a previous version.");
                    NeedToReportRollback = true;
                    RollbackPackageAsync().Wait();
                }
                else
                {
                    DidUpdate = true;
                    // Clear the React dev bundle cache so that new updates can be loaded.
                    if (MainPage.UseDeveloperSupport)
                    {
                        ClearReactDevBundleCacheAsync().Wait();
                    }
                    // Mark that we tried to initialize the new update, so that if it crashes,
                    // we will know that we need to rollback when the app next starts.
                    SettingsManager.SavePendingUpdate((string)pendingUpdate[CodePushConstants.PendingUpdateHashKey], /* isLoading */true);
                }
            }
        }

        internal async Task ClearUpdatesAsync()
        {
            await UpdateManager.ClearUpdatesAsync().ConfigureAwait(false);
            SettingsManager.RemovePendingUpdate();
            SettingsManager.RemoveFailedUpdates();
        }

        #endregion

        #region Private methods

        private async Task ClearReactDevBundleCacheAsync()
        {
            var devBundleCacheFile = (StorageFile)await ApplicationData.Current.LocalFolder.TryGetItemAsync(CodePushConstants.ReactDevBundleCacheFileName).AsTask().ConfigureAwait(false);
            if (devBundleCacheFile != null)
            {
                await devBundleCacheFile.DeleteAsync().AsTask().ConfigureAwait(false);
            }
        }

        private async Task RollbackPackageAsync()
        {
            JObject failedPackage = await UpdateManager.GetCurrentPackageAsync().ConfigureAwait(false);
            SettingsManager.SaveFailedUpdate(failedPackage);
            await UpdateManager.RollbackPackageAsync().ConfigureAwait(false);
            SettingsManager.RemovePendingUpdate();
        }

        #endregion
    }
}