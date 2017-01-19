using Newtonsoft.Json.Linq;
using System;
using System.Diagnostics;
using System.Threading.Tasks;
using Windows.ApplicationModel;
using Windows.Storage;
using Windows.Storage.Streams;
using Windows.System.Profile;

namespace CodePush.ReactNative
{
    internal class CodePushUtils
    {
        internal async static Task<JObject> GetJObjectFromFileAsync(StorageFile file)
        {
            string jsonString = await FileIO.ReadTextAsync(file).AsTask().ConfigureAwait(false);
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

        internal static string GetDeviceId()
        {
            HardwareToken token = HardwareIdentification.GetPackageSpecificToken(null);
            IBuffer hardwareId = token.Id;
            var dataReader = DataReader.FromBuffer(hardwareId);

            var bytes = new byte[hardwareId.Length];
            dataReader.ReadBytes(bytes);

            return BitConverter.ToString(bytes);
        }

        internal static string GetAppVersion()
        {
            //TODO: remove after check: 1.0.0
            return Package.Current.Id.Version.Major + "." + Package.Current.Id.Version.Minor + "." + Package.Current.Id.Version.Build;
        }

        internal static string GetAppFolder()
        {
            //TODO: remove after check:
            return ApplicationData.Current.LocalFolder.Path;
        }
    }
}
