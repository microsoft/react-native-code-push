using System;

namespace CodePush.ReactNative
{
    internal class CodePushInvalidUpdateException : Exception
    {
        internal CodePushInvalidUpdateException(string message)
            : base(message)
        {
        }
    }
}
