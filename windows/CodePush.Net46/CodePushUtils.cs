using Newtonsoft.Json.Linq;
using PCLStorage;
using System;
using System.Management;
using System.Threading.Tasks;

namespace CodePush.ReactNative
{
    internal partial class CodePushUtils
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
    }
}
