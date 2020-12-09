// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

#include "pch.h"

#include "winrt/Windows.Data.Json.h"
#include "winrt/Windows.Foundation.h"
#include "winrt/Windows.Storage.h"
#include "winrt/Windows.Storage.Streams.h"
#include "winrt/Windows.Web.Http.h"
#include "winrt/Windows.Web.Http.Headers.h"

#include "CodePushDownloadHandler.h"

namespace Microsoft::CodePush::ReactNative
{
    using namespace winrt;
    using namespace Windows::Data::Json;
    using namespace Windows::Foundation;
    using namespace Windows::Storage;
    using namespace Windows::Storage::Streams;
    using namespace Windows::Web::Http;

    CodePushDownloadHandler::CodePushDownloadHandler(
        StorageFile downloadFile,
        std::function<void(int64_t, int64_t)> progressCallback) :
        receivedContentLength(0),
        expectedContentLength(0),
        progressCallback(progressCallback),
        downloadFile(downloadFile) {}

    IAsyncOperation<bool> CodePushDownloadHandler::Download(std::wstring_view url)
    {
        HttpClient client;

        auto headers{ client.DefaultRequestHeaders() };
        headers.Append(L"Accept-Encoding", L"identity");

        HttpRequestMessage reqm{ HttpMethod::Get(), Uri(url) };
        auto resm{ co_await client.SendRequestAsync(reqm, HttpCompletionOption::ResponseHeadersRead) };
        expectedContentLength = resm.Content().Headers().ContentLength().GetInt64();
        auto inputStream{ co_await resm.Content().ReadAsInputStreamAsync() };
        auto outputStream{ co_await downloadFile.OpenAsync(FileAccessMode::ReadWrite) };

        uint8_t header[4] = {};

        for (;;)
        {
            auto outputBuffer{ co_await inputStream.ReadAsync(Buffer{ BufferSize }, BufferSize, InputStreamOptions::None) };

            if (outputBuffer.Length() == 0)
            {
                break;
            }
            co_await outputStream.WriteAsync(outputBuffer);

            if (receivedContentLength < ARRAYSIZE(header))
            {
                for (uint32_t i{ static_cast<uint32_t>(receivedContentLength) }; i < min(ARRAYSIZE(header), outputBuffer.Length()); i++)
                {
                    header[i] = outputBuffer.data()[i];
                }
            }

            receivedContentLength += outputBuffer.Length();

            progressCallback(/*expectedContentLength*/ expectedContentLength, /*receivedContentLength*/ receivedContentLength);
        }

        bool isZip{ header[0] == 'P' && header[1] == 'K' && header[2] == 3 && header[3] == 4 };
        co_return isZip;
    }
}
