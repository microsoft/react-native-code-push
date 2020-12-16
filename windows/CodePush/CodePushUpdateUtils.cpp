// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

#include "pch.h"

#include "winrt/Windows.ApplicationModel.h"
#include "winrt/Windows.Foundation.h"
#include "winrt/Windows.Foundation.Collections.h"
#include "winrt/Windows.Security.Cryptography.h"
#include "winrt/Windows.Security.Cryptography.Core.h"
#include "winrt/Windows.Storage.h"
#include "winrt/Windows.Storage.FileProperties.h"
#include "winrt/Windows.Storage.Streams.h"

#include <algorithm>
#include <string_view>
#include <vector>

#include "CodePushUpdateUtils.h"
#include "CodePushUtils.h"
#include "CodePushNativeModule.h"

namespace Microsoft::CodePush::ReactNative
{
    using namespace winrt;
    using namespace Windows::Foundation;
    using namespace Windows::Foundation::Collections;
    using namespace Windows::Storage;
    using namespace Windows::Security::Cryptography;
    using namespace Windows::Security::Cryptography::Core;

    /*static*/ IAsyncOperation<bool> CodePushUpdateUtils::CopyEntriesInFolderAsync(StorageFolder& sourceFolder, StorageFolder& destFolder)
    {
        auto entries{ co_await sourceFolder.GetItemsAsync() };
        for (const auto& entry : entries)
        {
            if (entry.IsOfType(StorageItemTypes::File))
            {
                auto file{ entry.try_as<StorageFile>() };
                co_await file.CopyAsync(destFolder, file.Name(), NameCollisionOption::ReplaceExisting);
            }
            else if (entry.IsOfType(StorageItemTypes::Folder))
            {
                auto folder{ entry.try_as<StorageFolder>() };
                auto folderCopy{ co_await destFolder.CreateFolderAsync(folder.Name(), CreationCollisionOption::ReplaceExisting) };
                auto result{ co_await CopyEntriesInFolderAsync(folder, folderCopy) };
                if (!result)
                {
                    co_return false;
                }
            }
        }

        co_return true;
    }

    /*static*/ IAsyncOperation<StorageFile> CodePushUpdateUtils::GetSignatureFileAsync(const StorageFolder& rootFolder)
    {
        auto manifestFolder{ (co_await rootFolder.TryGetItemAsync(ManifestFolderPrefix)).try_as<StorageFolder>() };
        if (manifestFolder != nullptr)
        {
            auto bundleJWTFile{ (co_await rootFolder.TryGetItemAsync(BundleJWTFile)).try_as<StorageFile>() };
            if (bundleJWTFile != nullptr)
            {
                co_return bundleJWTFile;
            }
        }
        co_return nullptr;
    }
    
    /*static*/ IAsyncOperation<hstring> CodePushUpdateUtils::ModifiedDateStringOfFileAsync(const StorageFile& file)
    {
        if (file != nullptr)
        {
            auto basicProperties{ co_await file.GetBasicPropertiesAsync() };
            auto modifiedDate{ basicProperties.DateModified() };
            auto mtime{ clock::to_time_t(modifiedDate) };
            auto modifiedDateString{ to_hstring(mtime) };
            co_return modifiedDateString;
        }
        else
        {
            co_return L"";
        }
    }
}
