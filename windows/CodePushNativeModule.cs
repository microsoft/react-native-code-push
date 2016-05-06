using Newtonsoft.Json.Linq;
using ReactNative;
using ReactNative.Bridge;
using ReactNative.Modules.Core;
using System;
using System.Collections.Generic;
using System.IO;
using System.Reflection;
using System.Threading.Tasks;
using Windows.Web.Http;

namespace CodePush.ReactNative
{
    internal class CodePushNativeModule : ReactContextNativeModuleBase
    {
        private CodePushReactPackage _codePush;
        private MinimumBackgroundListener _minimumBackgroundListener;
        private ReactContext _reactContext;

        public CodePushNativeModule(ReactContext reactContext, CodePushReactPackage codePush) : base(reactContext)
        {
            _reactContext = reactContext;
            _codePush = codePush;
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
                return new Dictionary<string, object>
                {
                    { "codePushInstallModeImmediate", InstallMode.Immediate },
                    { "codePushInstallModeOnNextResume", InstallMode.OnNextResume },
                    { "codePushInstallModeOnNextRestart", InstallMode.OnNextRestart },
                    { "codePushUpdateStateRunning", UpdateState.Running },
                    { "codePushUpdateStatePending", UpdateState.Pending },
                    { "codePushUpdateStateLatest", UpdateState.Lastest },
                };
            }
        }

        public override void Initialize()
        {
            _codePush.InitializeUpdateAfterRestart();
        }

        [ReactMethod]
        public void downloadUpdate(JObject updatePackage, bool notifyProgress, IPromise promise)
        {
            Action downloadAction = async () =>
            {
                try
                {
                    updatePackage[CodePushConstants.BinaryModifiedTimeKey] = "" + await _codePush.GetBinaryResourcesModifiedTime();
                    await _codePush.UpdateManager.DownloadPackage(
                        updatePackage,
                        _codePush.AssetsBundleFileName,
                        new Progress<HttpProgress>(
                            (HttpProgress progress) =>
                            {
                                if (!notifyProgress)
                                {
                                    return;
                                }

                                var downloadProgress = new JObject()
                                {
                                    { "totalBytes", progress.TotalBytesToReceive },
                                    { "receivedBytes", progress.BytesReceived }
                                };

                                _reactContext
                                    .GetJavaScriptModule<RCTDeviceEventEmitter>()
                                    .emit(CodePushConstants.DownloadProgressEventName, downloadProgress);
                            }
                        )
                    );

                    JObject newPackage = await _codePush.UpdateManager.GetPackage((string)updatePackage[CodePushConstants.PackageHashKey]);
                    promise.Resolve(newPackage);
                }
                catch (InvalidDataException e)
                {
                    CodePushUtils.Log(e.ToString());
                    SettingsManager.SaveFailedUpdate(updatePackage);
                    promise.Reject(e);
                }
                catch (Exception e)
                {
                    CodePushUtils.Log(e.ToString());
                    promise.Reject(e);
                }
            };

            Context.RunOnNativeModulesQueueThread(downloadAction);
        }

        [ReactMethod]
        public void getConfiguration(IPromise promise)
        {
            var config = new JObject
            {
                { "appVersion", _codePush.AppVersion },
                { "clientUniqueId", CodePushUtils.GetDeviceId() },
                { "deploymentKey", _codePush.DeploymentKey },
                { "serverUrl", CodePushConstants.CodePushServerUrl }
            };

            // TODO generate binary hash
            // string binaryHash = CodePushUpdateUtils.getHashForBinaryContents(mainActivity, isDebugMode);
            /*if (binaryHash != null)
            {
                configMap.putString(PACKAGE_HASH_KEY, binaryHash);
            }*/
            promise.Resolve(config);
        }

        [ReactMethod]
        public void getUpdateMetadata(UpdateState updateState, IPromise promise)
        {
            Action getCurrentPackageAction = async () =>
            {
                JObject currentPackage = await _codePush.UpdateManager.GetCurrentPackage();
                if (currentPackage == null)
                {
                    promise.Resolve("");
                    return;
                }

                var currentUpdateIsPending = false;

                if (currentPackage[CodePushConstants.PackageHashKey] != null)
                {
                    var currentHash = (string)currentPackage[CodePushConstants.PackageHashKey];
                    currentUpdateIsPending = SettingsManager.IsPendingUpdate(currentHash);
                }

                if (updateState == UpdateState.Pending && !currentUpdateIsPending)
                {
                    // The caller wanted a pending update
                    // but there isn't currently one.
                    promise.Resolve("");
                }
                else if (updateState == UpdateState.Running && currentUpdateIsPending)
                {
                    // The caller wants the running update, but the current
                    // one is pending, so we need to grab the previous.
                    promise.Resolve(await _codePush.UpdateManager.GetPreviousPackage());
                }
                else
                {
                    // The current package satisfies the request:
                    // 1) Caller wanted a pending, and there is a pending update
                    // 2) Caller wanted the running update, and there isn't a pending
                    // 3) Caller wants the latest update, regardless if it's pending or not
                    if (_codePush.IsRunningBinaryVersion)
                    {
                        // This only matters in Debug builds. Since we do not clear "outdated" updates,
                        // we need to indicate to the JS side that somehow we have a current update on
                        // disk that is not actually running.
                        currentPackage["_isDebugOnly"] = true;
                    }

                    // Enable differentiating pending vs. non-pending updates
                    currentPackage["isPending"] = currentUpdateIsPending;
                    promise.Resolve(currentPackage);
                }
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
        public void installUpdate(JObject updatePackage, InstallMode installMode, int minimumBackgroundDuration, IPromise promise)
        {
            Action installUpdateAction = async () =>
            {
                await _codePush.UpdateManager.InstallPackage(updatePackage, SettingsManager.IsPendingUpdate(null));
                var pendingHash = (string)updatePackage[CodePushConstants.PackageHashKey];
                SettingsManager.SavePendingUpdate(pendingHash, /* isLoading */false);
                if (installMode == InstallMode.OnNextResume)
                {
                    if (_minimumBackgroundListener == null)
                    {
                        // Ensure we do not add the listener twice.
                        Action loadBundleAction = () =>
                        {
                            Context.RunOnNativeModulesQueueThread(async () =>
                            {
                                await LoadBundle();
                            });
                        };
                        
                        _minimumBackgroundListener = new MinimumBackgroundListener(loadBundleAction, minimumBackgroundDuration);
                        _reactContext.AddLifecycleEventListener(_minimumBackgroundListener);
                    }
                    else
                    {
                        _minimumBackgroundListener.MinimumBackgroundDuration = minimumBackgroundDuration;
                    }
                }

                promise.Resolve("");
            };

            Context.RunOnNativeModulesQueueThread(installUpdateAction);
        }

        [ReactMethod]
        public void isFailedUpdate(string packageHash, IPromise promise)
        {
            promise.Resolve(SettingsManager.IsFailedHash(packageHash));
        }

        [ReactMethod]
        public void isFirstRun(string packageHash, IPromise promise)
        {
            Action isFirstRunAction = async () =>
            {
                bool isFirstRun = _codePush.DidUpdate
                    && packageHash != null
                    && packageHash.Length > 0
                    && packageHash.Equals(await _codePush.UpdateManager.GetCurrentPackageHash());
                promise.Resolve(isFirstRun);
            };

            Context.RunOnNativeModulesQueueThread(isFirstRunAction);
        }

        [ReactMethod]
        public void notifyApplicationReady(IPromise promise)
        {
            SettingsManager.RemovePendingUpdate();
            promise.Resolve("");
        }

        [ReactMethod]
        public void restartApp(bool onlyIfUpdateIsPending)
        {
            Action restartAppAction = async () =>
            {
                // If this is an unconditional restart request, or there
                // is current pending update, then reload the app.
                if (!onlyIfUpdateIsPending || SettingsManager.IsPendingUpdate(null))
                {
                    await LoadBundle();
                }
            };

            Context.RunOnNativeModulesQueueThread(restartAppAction);
        }
        
        internal async Task LoadBundle()
        {
            // #1) Get the private ReactInstanceManager, which is what includes
            //     the logic to reload the current React context.
            FieldInfo info = typeof(ReactPage)
                .GetField("_reactInstanceManager", BindingFlags.NonPublic | BindingFlags.Instance);

            var reactInstanceManager = (ReactInstanceManager)typeof(ReactPage)
                .GetField("_reactInstanceManager", BindingFlags.NonPublic | BindingFlags.Instance)
                .GetValue(_codePush.MainPage);

            // #2) Update the locally stored JS bundle file path
            Type reactInstanceManagerType = typeof(ReactInstanceManager);
            string latestJSBundleFile = await _codePush.GetJavaScriptBundleFileAsync(_codePush.AssetsBundleFileName);
            reactInstanceManagerType
                .GetField("_jsBundleFile", BindingFlags.NonPublic | BindingFlags.Instance)
                .SetValue(reactInstanceManager, latestJSBundleFile);

            // #3) Get the context creation method and fire it on the UI thread (which RN enforces)
            Context.RunOnDispatcherQueueThread(reactInstanceManager.RecreateReactContextInBackground);
        }
    }
}