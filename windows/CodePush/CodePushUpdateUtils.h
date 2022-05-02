#pragma once

#include "winrt/Windows.Data.Json.h"
#include "winrt/Windows.Foundation.h"
#include "winrt/Windows.Storage.h"

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

#include <string_view>

namespace Microsoft::CodePush::ReactNative
{
    struct CodePushUpdateUtils
    {
        static constexpr std::wstring_view AssetsFolderName{ L"assets" };
        static constexpr std::wstring_view BinaryHashKey{ L"CodePushBinaryHash" };
        static constexpr std::wstring_view ManifestFolderPrefix{ L"CodePush" };
        static constexpr std::wstring_view BundleJWTFile{ L".codepushrelease" };

        static winrt::Windows::Foundation::IAsyncOperation<bool> CopyEntriesInFolderAsync(
            winrt::Windows::Storage::StorageFolder& sourceFolder,
            winrt::Windows::Storage::StorageFolder& destFolder);

        static winrt::Windows::Foundation::IAsyncOperation<winrt::hstring> ModifiedDateStringOfFileAsync(const winrt::Windows::Storage::StorageFile& file);

        static winrt::Windows::Foundation::IAsyncOperation<winrt::Windows::Storage::StorageFile> GetSignatureFileAsync(
            const winrt::Windows::Storage::StorageFolder& rootFolder);

    private:
        /*
         Ignore list for hashing
         */
        static constexpr std::wstring_view IgnoreMacOSX{ L"__MACOSX/" };
        static constexpr std::wstring_view IgnoreDSStore{ L".DS_Store" };
        static constexpr std::wstring_view IgnoreCodePushMetadata{ L".codepushrelease" };
    };
}
