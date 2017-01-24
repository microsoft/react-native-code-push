using System;

namespace CodePush.Net46.Adapters.Http
{
    public enum HttpProgressStage
    {
        None = 0,
        DetectingProxy = 10,
        ResolvingName = 20,
        ConnectingToServer = 30,
        NegotiatingSsl = 40,
        SendingHeaders = 50,
        SendingContent = 60,
        WaitingForResponse = 70,
        ReceivingHeaders = 80,
        ReceivingContent = 90
    }

    public struct HttpProgress
    {
        public UInt64 BytesReceived;
        public UInt64 BytesSent;
        public UInt32 Retries;
        public HttpProgressStage Stage;
        public UInt64? TotalBytesToReceive;
        public UInt64? TotalBytesToSend;
    }
}
