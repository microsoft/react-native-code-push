using System;

namespace CodePush.ReactNative
{
    class CodePushNotInitializedException : Exception
    {
        public CodePushNotInitializedException(string message)
            : base(message)
        {
        }
    }
}