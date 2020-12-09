// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

#include "pch.h"

#include "miniz/miniz.h"
#include "winrt/Windows.Foundation.h"
#include "winrt/Windows.Storage.h"
#include "winrt/Windows.Storage.Search.h"
#include "winrt/Windows.Storage.Streams.h"

#include <string_view>
#include <stack>

#include "CodePushNativeModule.h"
#include "FileUtils.h"

namespace Microsoft::CodePush::ReactNative
{
    using namespace winrt;
    using namespace Windows::Foundation;
    using namespace Windows::Storage;
    using namespace Windows::Storage::Search;
    using namespace Windows::Storage::Streams;

    /*static*/ IAsyncOperation<StorageFile> FileUtils::CreateFileFromPathAsync(StorageFolder rootFolder, const std::filesystem::path& relativePath)
    {
        auto relPath{ relativePath };

        std::stack<std::string> pathParts;
        pathParts.push(relPath.filename().string());
        while (relPath.has_parent_path())
        {
            relPath = relPath.parent_path();
            pathParts.push(relPath.filename().string());
        }

        while (pathParts.size() > 1)
        {
            auto itemName{ pathParts.top() };
            rootFolder = co_await rootFolder.CreateFolderAsync(to_hstring(itemName), CreationCollisionOption::OpenIfExists);
            pathParts.pop();
        }
        auto fileName{ pathParts.top() };
        auto file{ co_await rootFolder.CreateFileAsync(to_hstring(fileName), CreationCollisionOption::ReplaceExisting) };
        co_return file;
    }

    /*static*/ IAsyncOperation<hstring> FileUtils::FindFilePathAsync(const StorageFolder& rootFolder, std::wstring_view fileName)
    {
        try
        {
            std::vector<hstring> fileTypeFilter{};
            fileTypeFilter.push_back(L".bundle");
            QueryOptions queryOptions{ CommonFileQuery::OrderByName, fileTypeFilter };
            queryOptions.IndexerOption(IndexerOption::DoNotUseIndexer);
            queryOptions.ApplicationSearchFilter(L"System.FileName: " + fileName);
            auto queryResult{ rootFolder.CreateFileQueryWithOptions(queryOptions) };
            auto files{ co_await queryResult.GetFilesAsync() };

            if (files.Size() > 0)
            {
                auto result{ files.GetAt(0) };
                std::wstring_view bundlePath{ result.Path() };
                hstring filePathSub{ bundlePath.substr(rootFolder.Path().size() + 1) };
                co_return filePathSub;
            }

            co_return L"";
        }
        catch (...)
        {
            throw;
        }
    }

    /*static*/ IAsyncAction FileUtils::UnzipAsync(const StorageFile& zipFile, const StorageFolder& destination)
    {
        std::string zipName{ to_string(zipFile.Path()) };

        mz_bool status;
        mz_zip_archive zip_archive;
        mz_zip_zero_struct(&zip_archive);

        status = mz_zip_reader_init_file(&zip_archive, zipName.c_str(), 0);
        assert(status);
        auto numFiles{ mz_zip_reader_get_num_files(&zip_archive) };

        for (mz_uint i = 0; i < numFiles; i++)
        {
            mz_zip_archive_file_stat file_stat;
            status = mz_zip_reader_file_stat(&zip_archive, i, &file_stat);
            assert(status);
            if (!mz_zip_reader_is_file_a_directory(&zip_archive, i))
            {
                auto fileName{ file_stat.m_filename };
                auto filePath{ std::filesystem::path(fileName) };
                auto filePathName{ filePath.filename() };
                auto filePathNameString{ filePathName.string() };

                auto entryFile{ co_await CreateFileFromPathAsync(destination, filePath) };
                
                auto stream{ co_await entryFile.OpenAsync(FileAccessMode::ReadWrite) };
                auto os{ stream.GetOutputStreamAt(0) };
                DataWriter dw{ os };

                const auto arrBufSize = 8 * 1024;
                std::array<uint8_t, arrBufSize> arrBuf;

                mz_zip_reader_extract_iter_state* pState = mz_zip_reader_extract_iter_new(&zip_archive, i, 0);
                while (size_t bytesRead{ mz_zip_reader_extract_iter_read(pState, static_cast<void*>(arrBuf.data()), arrBuf.size()) })
                {
                    array_view<const uint8_t> view{ arrBuf.data(), arrBuf.data() + bytesRead };
                    dw.WriteBytes(view);
                }
                status = mz_zip_reader_extract_iter_free(pState);
                assert(status);

                co_await dw.StoreAsync();
                co_await dw.FlushAsync();

                dw.Close();
                os.Close();
                stream.Close();
            }
        }

        status = mz_zip_reader_end(&zip_archive);
        assert(status);
    }
}
