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

            return procId + "-" + mbId;
        }
    }
}
