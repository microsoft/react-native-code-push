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
    internal partial class CodePushUtils
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
    }
}
