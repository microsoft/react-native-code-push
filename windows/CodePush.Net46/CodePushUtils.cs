using Newtonsoft.Json.Linq;
using PCLStorage;
using System;
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
    }
}
