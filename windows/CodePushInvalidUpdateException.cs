using System;

namespace ReactNative.CodePush
{
    class CodePushInvalidUpdateException : Exception
    {
        public CodePushInvalidUpdateException(string message)
            : base(message)
        {
        }
    }
}
