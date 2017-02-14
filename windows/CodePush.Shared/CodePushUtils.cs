using System;
using System.Diagnostics;
#if WINDOWS_UWP
using Windows.ApplicationModel;
using Windows.Storage;
#else
using System.IO;
#endif

namespace CodePush.ReactNative
{
    internal partial class CodePushUtils
    {
        internal static void Log(string message)
        {
            Debug.WriteLine("[CodePush] " + message, CodePushConstants.ReactNativeLogCategory);
        }

        internal static void LogBundleUrl(string path)
        {
            Log("Loading JS bundle from \"" + path + "\"");
        }

        static string _deviceId = String.Empty;

        internal static string GetDeviceId()
        {
            //It's quite long operation, cache it
            if (!String.IsNullOrEmpty(_deviceId))
                return _deviceId;

            _deviceId = GetDeviceIdImpl();
            return _deviceId;
        }

        internal static string GetAppVersion()
        {
#if WINDOWS_UWP
            return Package.Current.Id.Version.Major + "." + Package.Current.Id.Version.Minor + "." + Package.Current.Id.Version.Build;
#else
            var version = FileVersionInfo.GetVersionInfo(Environment.GetCommandLineArgs()[0]);
            return $"{version.FileMajorPart}.{version.FileMinorPart}.{version.FileBuildPart}";
#endif
        }

        internal static string GetAppFolder()
        {
#if WINDOWS_UWP
            return ApplicationData.Current.LocalFolder.Path;
#else
            return AppDomain.CurrentDomain.BaseDirectory;
#endif
        }

        internal static string GetAssetsBundlePrefix()
        {
#if WINDOWS_UWP
            return CodePushConstants.AssetsBundlePrefix;
#else
            return Path.Combine(GetAppFolder(), CodePushConstants.AssetsBundlePrefix);
#endif
        }

        internal static string GetFileBundlePrefix()
        {
#if WINDOWS_UWP
            return CodePushConstants.FileBundlePrefix;
#else
            return GetAppFolder();
#endif
        }

    }
}
