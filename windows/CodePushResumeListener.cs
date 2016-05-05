using ReactNative.Bridge;
using System;

namespace CodePush.ReactNative
{
    internal class CodePushResumeListener : ILifecycleEventListener
    {
        private DateTime? _lastSuspendDate = null;
        private CodePushNativeModule _codePushNativeModule;

        internal int MinimumBackgroundDuration { get; set; }

        internal CodePushResumeListener(CodePushNativeModule codePushNativeModule, int minimumBackgroundDuration)
        {
            _codePushNativeModule = codePushNativeModule;
            MinimumBackgroundDuration = minimumBackgroundDuration;
        }

        public void OnDestroy()
        {
        }

        public void OnResume()
        {
            if (_lastSuspendDate != null)
            {
                // Determine how long the app was in the background and ensure
                // that it meets the minimum duration amount of time.
                double durationInBackground = (new DateTime() - (DateTime)_lastSuspendDate).TotalSeconds;
                if (durationInBackground >= MinimumBackgroundDuration)
                {
                    Action loadBundleAction = async () =>
                    {
                        await _codePushNativeModule.LoadBundle();
                    };

                    _codePushNativeModule.Context.RunOnNativeModulesQueueThread(loadBundleAction);
                }
            }
        }

        public void OnSuspend()
        {
            // Save the current time so that when the app is later
            // resumed, we can detect how long it was in the background.
            _lastSuspendDate = new DateTime();
        }
    }
}
