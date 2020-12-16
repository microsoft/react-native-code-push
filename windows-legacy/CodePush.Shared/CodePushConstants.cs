namespace CodePush.ReactNative
{
    internal class CodePushConstants
    {
        internal const string BinaryModifiedTimeKey = "binaryModifiedTime";
        internal const string CodePushServerUrl = "https://codepush.appcenter.ms/";
        internal const string CodePushFolderPrefix = "CodePush";
        internal const string CodePushPreferences = "CodePush";
        internal const string CurrentPackageKey = "currentPackage";
        internal const string DefaultJsBundleName = "index.windows.bundle";
        internal const string DiffManifestFileName = "hotcodepush.json";
        internal const string DownloadFileName = "download.zip";
        internal const string DownloadProgressEventName = "CodePushDownloadProgress";
        internal const string DownloadUrlKey = "downloadUrl";
        internal const string FailedUpdatesKey = "CODE_PUSH_FAILED_UPDATES";
        internal const string PackageFileName = "app.json";
        internal const string PackageHashKey = "packageHash";
        internal const string PendingUpdateHashKey = "hash";
        internal const string PendingUpdateKey = "CODE_PUSH_PENDING_UPDATE";
        internal const string PendingUpdateIsLoadingKey = "isLoading";
        internal const string PreviousPackageKey = "previousPackage";
        // This needs to be kept in sync with https://github.com/ReactWindows/react-native-windows/blob/master/ReactWindows/ReactNative/DevSupport/DevSupportManager.cs#L22
        internal const string ReactDevBundleCacheFileName = "ReactNativeDevBundle.js";
        internal const string ReactNativeLogCategory = "ReactNative";
        internal const string RelativeBundlePathKey = "bundlePath";
        internal const string StatusFileName = "codepush.json";
        internal const string UnzippedFolderName = "unzipped";
#if WINDOWS_UWP
        internal const string AssetsBundlePrefix = "ms-appx:///ReactAssets/";
        internal const string FileBundlePrefix = "ms-appdata:///local/";
#else
        internal const string AssetsBundlePrefix = "ReactAssets/";
#endif
    }
}
