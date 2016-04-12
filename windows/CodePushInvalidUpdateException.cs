using System;

namespace CodePush.ReactNative
{
    class CodePushInvalidUpdateException : Exception
    {
        public CodePushInvalidUpdateException(string message)
            : base(message)
        {
        }
    }
}
