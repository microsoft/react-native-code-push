using System;
using System.Diagnostics;
#if WINDOWS_UWP
using Windows.ApplicationModel;
using Windows.Storage;
using Windows.Storage.Streams;
using Windows.System.Profile;
#else
using System.IO;
using System.Management;
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
            if (!String.IsNullOrEmpty(_deviceId))
                return _deviceId;
#if WINDOWS_UWP
            HardwareToken token = HardwareIdentification.GetPackageSpecificToken(null);
            IBuffer hardwareId = token.Id;
            var dataReader = DataReader.FromBuffer(hardwareId);

            var bytes = new byte[hardwareId.Length];
            dataReader.ReadBytes(bytes);

            return BitConverter.ToString(bytes);
#else

            //It's quite long operation, cache it

            ManagementObjectSearcher mos = new ManagementObjectSearcher("SELECT * FROM Win32_BaseBoard");
            ManagementObjectCollection moc = mos.Get();
            string mbId = String.Empty;
            foreach (ManagementObject mo in moc)
            {
                mbId = (string)mo["SerialNumber"];
                break;
            }

            ManagementObjectCollection mbsList = null;
            ManagementObjectSearcher mbs = new ManagementObjectSearcher("Select * From Win32_processor");
            mbsList = mbs.Get();
            string procId = string.Empty;
            foreach (ManagementObject mo in mbsList)
            {
                procId = mo["ProcessorID"].ToString();
                break;
            }

            _deviceId = procId + "-" + mbId;
            return _deviceId;
#endif
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
