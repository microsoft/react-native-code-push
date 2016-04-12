using System;

namespace ReactNative.CodePush
{
    class CodePushUnknownException : Exception
    {
        public CodePushUnknownException(string message, Exception inner)
            : base(message, inner)
        {
        }
    }
}