using Newtonsoft.Json.Linq;
using PCLStorage;
using System;
using System.Diagnostics;
using System.IO;
using System.Management;
using System.Threading.Tasks;

namespace CodePush.ReactNative
{
    internal partial class CodePushUtils
    {
        class ApplicationInfo
        {
            public ApplicationInfo()
            {
                var info = FileVersionInfo.GetVersionInfo(Environment.GetCommandLineArgs()[0]);
                Version = $"{info.FileMajorPart}.{info.FileMinorPart}.{info.FileBuildPart}";
                CompanyName = string.IsNullOrEmpty(info.CompanyName) ? info.ProductName : info.CompanyName;
                ProductName = info.ProductName;
            }

            public string Version { private set; get; }
            public string CompanyName { private set; get; }
            public string ProductName { private set; get; }
        }

        static ApplicationInfo applicationInfo = new ApplicationInfo();
        static string _bundlePath;

        internal static string GetFileBundlePrefix()
        {
            // For Windows desktop application the prefix of a bundle is the full path to the folder where
            // bundle should be installed.
            //
            // Desktop application can be installed at any location, the most popular are:
            // - User local data folder, in this case the bundle can be stored in the same location and be
            // unique for the user.
            // - Program Files folder, in this case the application is unique for the system and will be shared
            // amoung all users of the system, the bundle should be unique for the system as well.
            // Commonly, user has no write access to Program Files folder or at least admin privileges have to been requested.
            // In this case the bundle will be stored in ProgramData folder as it is recommended by MS.

            if (!string.IsNullOrEmpty(_bundlePath))
            {
                return _bundlePath;
            }

            _bundlePath = GetAppFolder();

            if (!HasWriteAccessToFolder(_bundlePath))
            {
                _bundlePath = $"{Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.CommonApplicationData), applicationInfo.CompanyName, applicationInfo.ProductName, applicationInfo.Version)}\\";
            }

            return _bundlePath;
        }

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

        static string GetDeviceIdImpl()
        {
            var mbId = GetSerialNumber();
            var procId = GetProcId();

            if (string.IsNullOrEmpty(procId))
            {
                procId = GetMAC();
            }

            return procId + '-' + mbId;
        }

        static string GetSerialNumber()
        {
            var mos = new ManagementObjectSearcher("SELECT * FROM Win32_BaseBoard");
            var moc = mos.Get();
            var mbId = String.Empty;

            foreach (ManagementObject mo in moc)
            {
                mbId = (string)mo["SerialNumber"];
                if (!string.IsNullOrEmpty(mbId))
                    break;
            }

            return mbId;
        }

        static string GetProcId()
        {
            var mos = new ManagementObjectSearcher("Select * From Win32_processor");
            var moc = mos.Get();
            var procId = string.Empty;

            foreach (ManagementObject mo in moc)
            {
                procId = (string)mo["ProcessorID"];
                if (!string.IsNullOrEmpty(procId))
                    break;
            }
            return procId;
        }

        static string GetMAC()
        {
            var mos = new ManagementObjectSearcher("Select * From Win32_NetworkAdapterConfiguration");
            var moc = mos.Get();
            var mac = string.Empty;

            foreach (ManagementObject mo in moc)
            {
                try
                {
                    if ((bool)mo["IPEnabled"])
                    {
                        mac = (string)mo["MacAddress"];
                        break;
                    }
                }
                catch (Exception)
                {
                }
            }

            return mac;
        }

        static bool HasWriteAccessToFolder(string path)
        {
            try
            {
                File.Open(Path.Combine(path, ".security-check"), FileMode.OpenOrCreate).Close();
                return true;
            }
            catch
            {
                return false;
            }
        }
    }
}
