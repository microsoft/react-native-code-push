// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

#include "pch.h"

#include "CodePushDownloadHandler.h"
#include "CodePushNativeModule.h"
#include "CodePushPackage.h"
#include "CodePushUtils.h"
#include "CodePushUpdateUtils.h"
#include "FileUtils.h"

#include <winrt/Windows.Storage.h>
#include <winrt/Windows.Storage.Streams.h>
#include <winrt/Windows.Foundation.h>

#include <functional>

namespace Microsoft::CodePush::ReactNative
{
    using namespace winrt;
    using namespace Windows::Data::Json;
    using namespace Windows::Foundation;
    using namespace Windows::Storage;
    using namespace Windows::Storage::Streams;

    /*static*/ IAsyncAction CodePushPackage::ClearUpdatesAsync()
    {
        auto codePushFolder{ co_await GetCodePushFolderAsync() };
        codePushFolder.DeleteAsync();
    }

    /*static*/ IAsyncAction CodePushPackage::DownloadPackageAsync(
        JsonObject& updatePackage,
        std::wstring_view expectedBundleFileName,
        std::wstring_view publicKey,
        std::function<void(int64_t, int64_t)> progressCallback)
    {
        auto newUpdateHash{ updatePackage.GetNamedString(L"packageHash") };
        auto codePushFolder{ co_await GetCodePushFolderAsync() };

        auto downloadFile{ co_await codePushFolder.CreateFileAsync(DownloadFileName, CreationCollisionOption::ReplaceExisting) };

        CodePushDownloadHandler downloadHandler{
            downloadFile,
            progressCallback };

        auto isZip{ co_await downloadHandler.Download(updatePackage.GetNamedString(L"downloadUrl")) };

        StorageFolder newUpdateFolder{ co_await codePushFolder.CreateFolderAsync(newUpdateHash, CreationCollisionOption::ReplaceExisting) };
        StorageFile newUpdateMetadataFile{ nullptr };
        auto mutableUpdatePackage{ updatePackage };
        if (isZip)
        {
            auto unzippedFolder{ co_await codePushFolder.CreateFolderAsync(UnzippedFolderName, CreationCollisionOption::ReplaceExisting) };
            co_await FileUtils::UnzipAsync(downloadFile, unzippedFolder);
            downloadFile.DeleteAsync();

            auto isDiffUpdate{ false };

            auto diffManifestFile{ (co_await unzippedFolder.TryGetItemAsync(DiffManifestFileName)).try_as<StorageFile>() };
            if (diffManifestFile != nullptr)
            {
                isDiffUpdate = true;
            }

            if (isDiffUpdate)
            {
                // Copy the current package to the new package.
                auto currentPackageFolder{ co_await GetCurrentPackageFolderAsync() };

                if (currentPackageFolder == nullptr)
                {
                    // Currently running the binary version, copy files from the bundled resources
                    auto newUpdateCodePushFolder{ co_await newUpdateFolder.CreateFolderAsync(CodePushUpdateUtils::ManifestFolderPrefix) };

                    auto binaryAssetsFolder{ co_await CodePushNativeModule::GetBundleAssetsFolderAsync() };
                    auto newUpdateAssetsFolder{ co_await newUpdateCodePushFolder.CreateFolderAsync(CodePushUpdateUtils::AssetsFolderName) };
                    CodePushUpdateUtils::CopyEntriesInFolderAsync(binaryAssetsFolder, newUpdateAssetsFolder);

                    auto binaryBundleFile{ co_await CodePushNativeModule::GetBinaryBundleAsync() };
                    co_await binaryBundleFile.CopyAsync(newUpdateCodePushFolder);
                }
                else
                {
                    // Copy the contents of the current package to the new package. (how are conflicts resolved?)
                    co_await CodePushUpdateUtils::CopyEntriesInFolderAsync(currentPackageFolder, newUpdateFolder);
                }

                auto manifestContent{ co_await FileIO::ReadTextAsync(diffManifestFile, UnicodeEncoding::Utf8) };
                auto manifestJson{ JsonObject::Parse(manifestContent) };
                auto deletedFiles{ manifestJson.TryLookup(L"deletedFiles") };
                auto deletedFilesArray{ deletedFiles.try_as<JsonArray>() };

                if (deletedFilesArray != nullptr)
                {
                    for (const auto& deletedFileName : deletedFilesArray)
                    {
                        auto fileToDelete{ (co_await newUpdateFolder.TryGetItemAsync(deletedFileName.GetString())).try_as<StorageFile>() };
                        if (fileToDelete != nullptr)
                        {
                            co_await fileToDelete.DeleteAsync();
                        }
                    }
                }

                co_await diffManifestFile.DeleteAsync();
            }

            co_await CodePushUpdateUtils::CopyEntriesInFolderAsync(unzippedFolder, newUpdateFolder);
            co_await unzippedFolder.DeleteAsync();

            auto relativeBundlePath{ co_await FileUtils::FindFilePathAsync(newUpdateFolder, expectedBundleFileName) };
            if (!relativeBundlePath.empty())
            {
                mutableUpdatePackage.Insert(RelativeBundlePathKey, JsonValue::CreateStringValue(relativeBundlePath));
            }
            else
            {
                auto errorMessage{ L"Error: Unable to find JS bundle in downloaded package." };
                hresult_error error{ E_INVALIDARG, errorMessage };
                CodePushUtils::Log(error);
                throw error;
            }

            auto newUpdateMetadata{ co_await newUpdateFolder.TryGetItemAsync(UpdateMetadataFileName) };
            if (newUpdateMetadata != nullptr)
            {
                co_await newUpdateMetadata.DeleteAsync();
            }

            CodePushUtils::Log((isDiffUpdate) ? L"Applying diff update." : L"Applying full update.");
            auto isSignatureVerificationEnabled{ !publicKey.empty() };

            auto signatureFile{ co_await CodePushUpdateUtils::GetSignatureFileAsync(newUpdateFolder) };
            auto isSignatureAppearedInBundle{ signatureFile != nullptr };

            if (isSignatureVerificationEnabled)
            {
                if (isSignatureAppearedInBundle)
                {
                    auto errorMessage{ L"Error: Signature Verification is not currently supported." };
                    hresult_error error{ E_NOTIMPL, errorMessage };
                    CodePushUtils::Log(error);
                    throw error;
                }
                else
                {
                    auto errorMessage{ L"Error! Public key was provided but there is no JWT signature within app bundle to verify " \
                                L"Possible reasons, why that might happen: \n" \
                                L"1. You've been released CodePush bundle update using a version of the CodePush CLI that does not support code signing.\n" \
                                L"2. You've been released CodePush bundle update without providing --privateKeyPath option." };
                    hresult_error error{ E_FAIL, errorMessage };
                    CodePushUtils::Log(error);
                    throw error;
                }
            }
            else
            {
                bool needToVerifyHash;
                if (isSignatureAppearedInBundle)
                {
                    CodePushUtils::Log(L"Warning! JWT signature exists in codepush update but code integrity check couldn't be performed" \
                        L" because there is no public key configured. " \
                        L"Please ensure that a public key is properly configured within your application.");
                    needToVerifyHash = true;
                }
                else
                {
                    needToVerifyHash = isDiffUpdate;
                }

                if (needToVerifyHash)
                {
                    auto errorMessage{ L"Error: package content verification is not currently supported." };
                    hresult_error error{ E_NOTIMPL, errorMessage };
                    CodePushUtils::Log(error);
                }
            }
        }
        else
        {
            co_await downloadFile.MoveAsync(newUpdateFolder, UpdateBundleFileName, NameCollisionOption::ReplaceExisting);
        }

        newUpdateMetadataFile = co_await newUpdateFolder.CreateFileAsync(UpdateMetadataFileName, CreationCollisionOption::ReplaceExisting);

        auto packageJsonString{ mutableUpdatePackage.Stringify() };
        co_await FileIO::WriteTextAsync(newUpdateMetadataFile, packageJsonString);

        co_return;
    }

    /*static*/ IAsyncOperation<StorageFolder> CodePushPackage::GetCodePushFolderAsync()
    {
        auto localStorage{ CodePushNativeModule::GetLocalStorageFolder() };
        auto codePushFolder{ co_await localStorage.CreateFolderAsync(L"CodePush", CreationCollisionOption::OpenIfExists) };
        co_return codePushFolder;
    }

    /*static*/ IAsyncOperation<JsonObject> CodePushPackage::GetCurrentPackageAsync()
    {
        auto packageHash{ co_await GetCurrentPackageHashAsync() };
        if (packageHash.empty())
        {
            co_return nullptr;
        }
        co_return co_await GetPackageAsync(packageHash);
    }

    /*static*/ IAsyncOperation<StorageFile> CodePushPackage::GetCurrentPackageBundleAsync()
    {
        auto packageFolder{ co_await GetCurrentPackageFolderAsync() };
        if (packageFolder == nullptr)
        {
            co_return nullptr;
        }

        auto currentPackage{ co_await GetCurrentPackageAsync() };
        if (currentPackage == nullptr)
        {
            co_return nullptr;
        }

        auto relativeBundlePath{ currentPackage.GetNamedString(RelativeBundlePathKey, L"") };
        if (!relativeBundlePath.empty())
        {
            auto currentBundle{ (co_await packageFolder.TryGetItemAsync(relativeBundlePath)).try_as<StorageFile>() };
            co_return currentBundle;
        }

        co_return nullptr;
    }

    /*static*/ IAsyncOperation<StorageFolder> CodePushPackage::GetCurrentPackageFolderAsync()
    {
        auto info{ co_await GetCurrentPackageInfoAsync() };
        if (info == nullptr)
        {
            return nullptr;
        }

        auto packageHash{ info.GetNamedString(L"currentPackage", L"") };
        if (packageHash.empty())
        {
            return nullptr;
        }

        auto codePushFolder{ co_await GetCodePushFolderAsync() };
        auto packageFolder{ (co_await codePushFolder.TryGetItemAsync(packageHash)).try_as<StorageFolder>() };
        co_return packageFolder;
    }

    /*static*/ IAsyncOperation<hstring> CodePushPackage::GetCurrentPackageHashAsync()
    {
        auto info{ co_await GetCurrentPackageInfoAsync() };
        if (info == nullptr)
        {
            co_return L"";
        }
        auto currentPackage{ info.TryLookup(L"currentPackage") };
        if (currentPackage == nullptr)
        {
            co_return L"";
        }
        co_return currentPackage.GetString();
    }

    /*static*/ IAsyncOperation<JsonObject> CodePushPackage::GetCurrentPackageInfoAsync()
    {
        try
        {
            auto statusFile{ co_await GetStatusFileAsync() };
            if (statusFile == nullptr)
            {
                co_return JsonObject{};
            }
            auto content{ co_await FileIO::ReadTextAsync(statusFile) };
            JsonObject json;
            auto success{ JsonObject::TryParse(content, json) };
            if (!success)
            {
                co_return nullptr;
            }
            co_return json;
        }
        catch (...)
        {
            // Either the file does not exist or does not contain valid JSON
            co_return nullptr;
        }
        co_return nullptr;
    }

    /*static*/ IAsyncOperation<JsonObject> CodePushPackage::GetPreviousPackageAsync()
    {
        auto packageHash{ co_await GetPreviousPackageHashAsync() };
        if (packageHash.empty())
        {
            co_return nullptr;
        }
        co_return co_await GetPackageAsync(packageHash);
    }

    /*static*/ IAsyncOperation<hstring> CodePushPackage::GetPreviousPackageHashAsync()
    {
        auto info{ co_await GetCurrentPackageInfoAsync() };
        if (info == nullptr)
        {
            co_return L"";
        }
        auto previousHash{ info.TryLookup(L"previousPackage") };
        if (previousHash == nullptr)
        {
            co_return L"";
        }
        co_return previousHash.GetString();
    }

    /*static*/ IAsyncOperation<JsonObject> CodePushPackage::GetPackageAsync(std::wstring_view packageHash)
    {
        auto updateDirectory{ co_await GetPackageFolderAsync(packageHash) };
        if (updateDirectory != nullptr)
        {
            auto updateMetadataFile{ (co_await updateDirectory.TryGetItemAsync(UpdateMetadataFileName)).try_as<StorageFile>() };
            if (updateMetadataFile != nullptr)
            {
                auto updateMetadataString{ co_await FileIO::ReadTextAsync(updateMetadataFile) };
                JsonObject updateMetadata;
                auto success{ JsonObject::TryParse(updateMetadataString, updateMetadata) };
                if (success)
                {
                    co_return updateMetadata;
                }
            }
        }
        co_return nullptr;
    }

    /*static*/ IAsyncOperation<StorageFolder> CodePushPackage::GetPackageFolderAsync(std::wstring_view packageHash)
    {
        auto codePushFolder{ co_await GetCodePushFolderAsync() };
        co_return (co_await codePushFolder.TryGetItemAsync(packageHash)).try_as<StorageFolder>();
    }

    /*static*/ IAsyncOperation<bool> CodePushPackage::InstallPackageAsync(JsonObject updatePackage, bool removePendingUpdate)
    {
        auto packageHash{ updatePackage.GetNamedString(L"packageHash") };
        auto info{ co_await GetCurrentPackageInfoAsync() };
        if (info == nullptr)
        {
            co_return false;
        }

        if (info.HasKey(L"currentPackage") && packageHash == info.GetNamedString(L"currentPackage"))
        {
            // The current package is already the one being installed, so we should no-op.
            co_return true;
        }

        if (removePendingUpdate)
        {
            auto currentPackageFolder{ co_await GetCurrentPackageFolderAsync() };
            if (currentPackageFolder != nullptr)
            {
                try
                {
                    co_await currentPackageFolder.DeleteAsync();
                }
                catch (...)
                {
                    CodePushUtils::Log(L"Error deleting pending package.");
                }
            }
        }
        else
        {
            auto previousPackageHash{ co_await GetPreviousPackageHashAsync() };
            if (!previousPackageHash.empty() && previousPackageHash != packageHash)
            {
                auto previousPackageFolder{ co_await GetPackageFolderAsync(previousPackageHash) };
                try
                {
                    co_await previousPackageFolder.DeleteAsync();
                }
                catch (...)
                {
                    CodePushUtils::Log(L"Error deleting old package.");
                }
            }

            IJsonValue currentPackage;
            if (info.HasKey(L"currentPackage"))
            {
                currentPackage = info.Lookup(L"currentPackage");
            }
            else
            {
                currentPackage = JsonValue::CreateStringValue(L"");
            }
            info.Insert(L"previousPackage", currentPackage);
        }

        info.Insert(L"currentPackage", JsonValue::CreateStringValue(packageHash));
        co_return co_await UpdateCurrentPackageInfoAsync(info);
    }

    /*static*/ IAsyncAction CodePushPackage::RollbackPackage()
    {
        auto info{ co_await GetCurrentPackageInfoAsync() };
        if (info == nullptr)
        {
            CodePushUtils::Log(L"Error getting current package info.");
            co_return;
        }

        auto currentPackageFolder{ co_await GetCurrentPackageFolderAsync() };
        if (currentPackageFolder == nullptr)
        {
            CodePushUtils::Log(L"Error getting package folder path.");
        }

        try
        {
            co_await currentPackageFolder.DeleteAsync();
        }
        catch (...)
        {
            CodePushUtils::Log(L"Error deleting current package contents.");
        }

        info.Insert(L"currentPackage", info.TryLookup(L"previousPackage"));
        info.Remove(L"previousPackage");

        co_await UpdateCurrentPackageInfoAsync(info);
    }

    /*static*/ IAsyncOperation<StorageFile> CodePushPackage::GetStatusFileAsync()
    {
        auto codePushFolder{ co_await GetCodePushFolderAsync() };
        co_return (co_await codePushFolder.TryGetItemAsync(CodePushPackage::StatusFile)).try_as<StorageFile>();
    }

    /*static*/ IAsyncOperation<bool> CodePushPackage::UpdateCurrentPackageInfoAsync(JsonObject packageInfo)
    {
        auto packageInfoString{ packageInfo.Stringify() };
        auto infoFile{ co_await GetStatusFileAsync() };
        if (infoFile == nullptr)
        {
            auto codePushFolder{ co_await GetCodePushFolderAsync() };
            infoFile = co_await codePushFolder.CreateFileAsync(CodePushPackage::StatusFile);
        }
        co_await FileIO::WriteTextAsync(infoFile, packageInfoString);
        co_return true;
    }
}
