using System;

namespace CodePush.ReactNative
{
    class CodePushUnknownException : Exception
    {
        public CodePushUnknownException(string message, Exception inner)
            : base(message, inner)
        {
        }
    }
}