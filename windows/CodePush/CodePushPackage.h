// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

#pragma once

#include <winrt/Windows.Data.Json.h>
#include <functional>

namespace Microsoft::CodePush::ReactNative
{
	struct CodePushPackage
	{
		static constexpr std::wstring_view DiffManifestFileName{ L"hotcodepush.json" };
		static constexpr std::wstring_view DownloadFileName{ L"download.zip" };
		static constexpr std::wstring_view RelativeBundlePathKey{ L"bundlePath" };
		static constexpr std::wstring_view StatusFile{ L"codepush.json" };
		static constexpr std::wstring_view UpdateBundleFileName{ L"app.jsbundle" };
		static constexpr std::wstring_view UpdateMetadataFileName{ L"app.json" };
		static constexpr std::wstring_view UnzippedFolderName{ L"unzipped" };

		static winrt::Windows::Foundation::IAsyncAction ClearUpdatesAsync();

		static winrt::Windows::Foundation::IAsyncAction DownloadPackageAsync(
			winrt::Windows::Data::Json::JsonObject& updatePackage,
			std::wstring_view expectedBundleFileName,
			std::wstring_view publicKey,
			std::function<void(int64_t, int64_t)> progressCallback);

		static winrt::Windows::Foundation::IAsyncOperation<winrt::Windows::Data::Json::JsonObject> GetCurrentPackageAsync();
		static winrt::Windows::Foundation::IAsyncOperation<winrt::Windows::Data::Json::JsonObject> GetPreviousPackageAsync();
		static winrt::Windows::Foundation::IAsyncOperation<winrt::Windows::Storage::StorageFolder> GetCurrentPackageFolderAsync();
		static winrt::Windows::Foundation::IAsyncOperation<winrt::Windows::Storage::StorageFile> GetCurrentPackageBundleAsync();
		static winrt::Windows::Foundation::IAsyncOperation<winrt::hstring> GetCurrentPackageHashAsync();

		static winrt::Windows::Foundation::IAsyncOperation<winrt::Windows::Data::Json::JsonObject> GetPackageAsync(std::wstring_view packageHash);

		static winrt::Windows::Foundation::IAsyncOperation<bool> InstallPackageAsync(winrt::Windows::Data::Json::JsonObject updatePackage, bool removePendingUpdate);

		static winrt::Windows::Foundation::IAsyncAction RollbackPackage();

	private:
		static winrt::Windows::Foundation::IAsyncOperation<winrt::Windows::Storage::StorageFolder> GetCodePushFolderAsync();
		static winrt::Windows::Foundation::IAsyncOperation<winrt::Windows::Data::Json::JsonObject> GetCurrentPackageInfoAsync();
		static winrt::Windows::Foundation::IAsyncOperation<winrt::Windows::Storage::StorageFolder> GetPackageFolderAsync(std::wstring_view packageHash);
		static winrt::Windows::Foundation::IAsyncOperation<winrt::hstring> GetPreviousPackageHashAsync();
		static winrt::Windows::Foundation::IAsyncOperation<winrt::Windows::Storage::StorageFile> GetStatusFileAsync();
		static winrt::Windows::Foundation::IAsyncOperation<bool> UpdateCurrentPackageInfoAsync(winrt::Windows::Data::Json::JsonObject packageInfo);
	};
}
