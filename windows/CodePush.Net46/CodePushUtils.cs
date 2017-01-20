using Newtonsoft.Json.Linq;
using PCLStorage;
using System;
using System.Diagnostics;
using System.IO;
using System.Management;
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

            //It's quite long operation, will cache it

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
            return FileVersionInfo.GetVersionInfo(Environment.GetCommandLineArgs()[0]).ProductVersion;
        }

        internal static string GetAppFolder()
        {
            return FileSystem.Current.LocalStorage.Path;
        }
    }
}
