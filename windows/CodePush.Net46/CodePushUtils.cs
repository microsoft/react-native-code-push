using Newtonsoft.Json.Linq;
using PCLStorage;
using System;
using System.Diagnostics;
using System.IO;
using System.Management;
using System.Reflection;
using System.Threading.Tasks;

namespace CodePush.ReactNative
{
    internal class CodePushUtils
    {
        internal async static Task<JObject> GetJObjectFromFileAsync(IFile file)
        {
            string jsonString = await file.ReadAllTextAsync().ConfigureAwait(false);
            if (jsonString.Length == 0)
            {
                return new JObject();
            }

            try
            {
                return JObject.Parse(jsonString);
            }
            catch (Exception)
            {
                return null;
            }

        }

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
        }

        internal static string GetAppVersion()
        {
            var version = FileVersionInfo.GetVersionInfo(Environment.GetCommandLineArgs()[0]);
            return $"{version.FileMajorPart}.{version.FileMinorPart}.{version.FileBuildPart}";
        }

        internal static string GetAppFolder()
        {
            return AppDomain.CurrentDomain.BaseDirectory;
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
