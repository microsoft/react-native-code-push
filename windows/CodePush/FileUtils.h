// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

#pragma once

#include "winrt/Windows.Storage.h"
#include "winrt/Windows.Foundation.h"

#include <filesystem>
#include <string_view>

namespace Microsoft::CodePush::ReactNative
{
	struct FileUtils
	{
		static winrt::Windows::Foundation::IAsyncOperation<winrt::Windows::Storage::StorageFile> CreateFileFromPathAsync(
			winrt::Windows::Storage::StorageFolder rootFolder, 
			const std::filesystem::path& relativePath);

		static winrt::Windows::Foundation::IAsyncOperation<winrt::hstring> FindFilePathAsync(
			const winrt::Windows::Storage::StorageFolder& rootFolder, 
			std::wstring_view fileName);

		static winrt::Windows::Foundation::IAsyncAction UnzipAsync(
			const winrt::Windows::Storage::StorageFile& zipFile, 
			const winrt::Windows::Storage::StorageFolder& destination);
	};
}
