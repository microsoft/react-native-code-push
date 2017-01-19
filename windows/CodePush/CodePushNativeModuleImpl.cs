using Newtonsoft.Json.Linq;
using ReactNative.Bridge;
using ReactNative.Modules.Core;
using System;
using System.Threading.Tasks;
using Windows.Web.Http;

namespace CodePush.ReactNative
{
    internal class CodePushNativeModuleImpl
    {

        public static async Task downloadUpdateImplAsync(JObject updatePackage, bool notifyProgress, CodePushReactPackage codePush, ReactContext reactContext)
        {
            await codePush.UpdateManager.DownloadPackageAsync(
                updatePackage,
                codePush.AssetsBundleFileName,
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

                        reactContext
                            .GetJavaScriptModule<RCTDeviceEventEmitter>()
                            .emit(CodePushConstants.DownloadProgressEventName, downloadProgress);
                    }
                )
            ).ConfigureAwait(false);
        }

    }
}