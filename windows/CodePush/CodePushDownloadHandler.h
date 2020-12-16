// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

#pragma once

#include "winrt/Windows.Storage.h"
#include "winrt/Windows.Foundation.h"
#include "winrt/Windows.Data.Json.h"
#include <string_view>
#include <functional>

namespace Microsoft::CodePush::ReactNative
{
	struct CodePushDownloadHandler
	{
		winrt::Windows::Storage::StorageFile downloadFile;
		int64_t expectedContentLength;
		int64_t receivedContentLength;
		std::function<void(int64_t, int64_t)> progressCallback;
		std::wstring_view downloadUrl;

		CodePushDownloadHandler(
			winrt::Windows::Storage::StorageFile downloadFile,
			std::function<void(int64_t, int64_t)> progressCallback);

		// Returns true if the downloaded file is a zip file
		winrt::Windows::Foundation::IAsyncOperation<bool> Download(std::wstring_view url);

	private:
		static constexpr uint32_t BufferSize{ 256 * 1024 };
	};
}
