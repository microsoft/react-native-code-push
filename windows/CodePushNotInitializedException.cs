using System;

namespace ReactNative.CodePush
{
    public class CodePushNotInitializedException : Exception
    {
        public CodePushNotInitializedException(string message)
            : base(message)
        {
        }
    }
}