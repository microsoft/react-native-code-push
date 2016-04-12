using Newtonsoft.Json.Linq;
using System;
using System.Diagnostics;
using System.Threading.Tasks;
using Windows.Storage;
using Windows.Storage.Streams;
using Windows.System.Profile;

namespace CodePush.ReactNative
{
    class CodePushUtils
    {
        public static readonly string REACT_NATIVE_LOG_CATEGORY = "ReactNative";

        public async static Task<JObject> GetJObjectFromFile(StorageFile file)
        {
            string jsonString = await FileIO.ReadTextAsync(file);
            if (jsonString.Length == 0)
            {
                return new JObject();
            }

            return JObject.Parse(jsonString);
        }

        public static void log(string message)
        {
            Debug.WriteLine("[CodePush] " + message, REACT_NATIVE_LOG_CATEGORY);
        }
        
        public static void logBundleUrl(string path)
        {
            log("Loading JS bundle from \"" + path + "\"");
        }

        public static string GetDeviceId()
        {
            HardwareToken token = HardwareIdentification.GetPackageSpecificToken(null);
            IBuffer hardwareId = token.Id;
            DataReader dataReader = DataReader.FromBuffer(hardwareId);

            byte[] bytes = new byte[hardwareId.Length];
            dataReader.ReadBytes(bytes);

            return BitConverter.ToString(bytes);
        }
    }
}
