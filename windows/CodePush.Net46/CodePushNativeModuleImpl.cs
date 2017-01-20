using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace CodePush.ReactNative
{
    internal class CodePushNativeModuleImpl
    {
        //TODO: implement
        /*
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
        }*/
    }
}