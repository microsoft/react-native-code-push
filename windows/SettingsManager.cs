using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using Windows.Storage;

namespace CodePush.ReactNative
{
    internal class SettingsManager
    {
        private static ApplicationDataContainer GetCodePushSettings()
        {
            return ApplicationData.Current.LocalSettings.CreateContainer(CodePushConstants.CodePushPreferences, ApplicationDataCreateDisposition.Always);
        }

        public static JArray GetFailedUpdates()
        {
            ApplicationDataContainer settings = GetCodePushSettings();
            var failedUpdatesString = (string)settings.Values[CodePushConstants.FailedUpdatesKey];
            if (failedUpdatesString == null)
            {
                return new JArray();
            }

            try
            {
                return JArray.Parse(failedUpdatesString);
            }
            catch (Exception)
            {
                var emptyArray = new JArray();
                settings.Values[CodePushConstants.FailedUpdatesKey] = JsonConvert.SerializeObject(emptyArray);
                return emptyArray;
            }
        }

        internal static JObject GetPendingUpdate()
        {
            ApplicationDataContainer settings = GetCodePushSettings();
            var pendingUpdateString = (string)settings.Values[CodePushConstants.PendingUpdateKey];
            if (pendingUpdateString == null)
            {
                return null;
            }

            try
            {
                return JObject.Parse(pendingUpdateString);
            }
            catch (Exception)
            {
                // Should not happen.
                CodePushUtils.Log("Unable to parse pending update metadata " + pendingUpdateString +
                        " stored in SharedPreferences");
                return null;
            }
        }

        internal static void RemoveFailedUpdates()
        {
            ApplicationDataContainer settings = GetCodePushSettings();
            settings.Values.Remove(CodePushConstants.FailedUpdatesKey);
        }

        internal static void RemovePendingUpdate()
        {
            ApplicationDataContainer settings = GetCodePushSettings();
            settings.Values.Remove(CodePushConstants.PendingUpdateKey);
        }

        internal static void SaveFailedUpdate(JObject failedPackage)
        {
            ApplicationDataContainer settings = GetCodePushSettings();
            var failedUpdatesString = (string)settings.Values[CodePushConstants.FailedUpdatesKey];
            JArray failedUpdates;
            if (failedUpdatesString == null)
            {
                failedUpdates = new JArray();
            }
            else
            {
                failedUpdates = JArray.Parse(failedUpdatesString);
            }

            failedUpdates.Add(failedPackage);
            settings.Values[CodePushConstants.FailedUpdatesKey] = JsonConvert.SerializeObject(failedUpdates);
        }

        internal static void SavePendingUpdate(string packageHash, bool isLoading)
        {
            ApplicationDataContainer settings = GetCodePushSettings();
            var pendingUpdate = new JObject();
            pendingUpdate[CodePushConstants.PendingUpdateHashKey] = packageHash;
            pendingUpdate[CodePushConstants.PendingUpdateIsLoadingKey] = isLoading;
            settings.Values[CodePushConstants.PendingUpdateKey] = JsonConvert.SerializeObject(pendingUpdate);
        }

    }
}