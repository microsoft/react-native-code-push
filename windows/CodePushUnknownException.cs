using System;

namespace CodePush.ReactNative
{
    class CodePushUnknownException : Exception
    {
        internal CodePushUnknownException(string message, Exception inner)
            : base(message, inner)
        {
        }
    }
}