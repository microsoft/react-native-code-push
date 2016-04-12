using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using System.IO;
using System.IO.Compression;
using System.Threading;
using System.Threading.Tasks;
using Windows.Storage;
using Windows.Storage.Streams;
using Windows.Web.Http;

namespace ReactNative.CodePush
{
    class CodePushPackage
    {
        private readonly string CODE_PUSH_FOLDER_PREFIX = "CodePush";
        private readonly string CURRENT_PACKAGE_KEY = "currentPackage";
        private readonly string DIFF_MANIFEST_FILE_NAME = "hotcodepush.json";
        private readonly string DOWNLOAD_FILE_NAME = "download.zip";
        private readonly string DOWNLOAD_URL_KEY = "downloadUrl";
        private readonly string PACKAGE_FILE_NAME = "app.json";
        private readonly string PACKAGE_HASH_KEY = "packageHash";
        private readonly string PREVIOUS_PACKAGE_KEY = "previousPackage";
        private readonly string RELATIVE_BUNDLE_PATH_KEY = "bundlePath";
        private readonly string STATUS_FILE = "codepush.json";
        private readonly string UNZIPPED_FOLDER_NAME = "unzipped";

        public CodePushPackage()
        {
        }

        private async Task<StorageFile> GetDownloadFile()
        {
            StorageFolder codePushFolder = await GetCodePushFolder();
            return await codePushFolder.CreateFileAsync(DOWNLOAD_FILE_NAME, CreationCollisionOption.OpenIfExists);
        }

        private async Task<StorageFolder> GetUnzippedFolder()
        {
            StorageFolder codePushFolder = await GetCodePushFolder();
            return await codePushFolder.CreateFolderAsync(UNZIPPED_FOLDER_NAME, CreationCollisionOption.OpenIfExists);
        }

        private async Task<StorageFolder> GetCodePushFolder()
        {
            return await ApplicationData.Current.LocalFolder.CreateFolderAsync(CODE_PUSH_FOLDER_PREFIX, CreationCollisionOption.OpenIfExists);
        }

        private async Task<StorageFile> GetStatusFile()
        {
            StorageFolder codePushFolder = await GetCodePushFolder();
            return await codePushFolder.CreateFileAsync(STATUS_FILE, CreationCollisionOption.OpenIfExists);
        }

        public async Task<JObject> GetCurrentPackageInfo()
        {
            StorageFile statusFile = await GetStatusFile();
            try
            {
                return await CodePushUtils.GetJObjectFromFile(statusFile);
            }
            catch (Exception e)
            {
                throw new CodePushUnknownException("Error getting current package info", e);
            }
        }

        public async Task UpdateCurrentPackageInfo(JObject packageInfo)
        {
            try
            {
                await FileIO.WriteTextAsync(await GetStatusFile(), JsonConvert.SerializeObject(packageInfo));
            }
            catch (IOException e)
            {
                throw new CodePushUnknownException("Error updating current package info", e);
            }
        }

        public async Task<StorageFolder> GetCurrentPackageFolder()
        {
            JObject info = await GetCurrentPackageInfo();
            string packageHash = (string)info[CURRENT_PACKAGE_KEY];
            if (packageHash == null)
            {
                return null;
            }

            return await GetPackageFolder(packageHash, false);
        }


        public async Task<StorageFile> GetCurrentPackageBundle(string bundleFileName)
        {
            StorageFolder packageFolder = await GetCurrentPackageFolder();
            if (packageFolder == null)
            {
                return null;
            }

            JObject currentPackage = await GetCurrentPackage();
            string relativeBundlePath = (string)currentPackage[RELATIVE_BUNDLE_PATH_KEY];
            if (relativeBundlePath == null)
            {
                return await packageFolder.GetFileAsync(bundleFileName);
            }
            else
            {
                return await packageFolder.GetFileAsync(relativeBundlePath);
            }
        }

        public async Task<StorageFolder> GetPackageFolder(string packageHash, bool createIfNotExists)
        {
            StorageFolder codePushFolder = await GetCodePushFolder();
            try
            {
                packageHash = shortenPackageHash(packageHash);
                if (createIfNotExists)
                {
                    return await codePushFolder.CreateFolderAsync(packageHash, CreationCollisionOption.OpenIfExists);
                }
                else
                {
                    return await codePushFolder.GetFolderAsync(packageHash);
                }
            }
            catch (FileNotFoundException)
            {
                return null;
            }
        }

        public async Task<string> GetCurrentPackageHash()
        {
            JObject metadata = await GetCurrentPackage();
            return (string)metadata[PACKAGE_HASH_KEY];
        }

        public async Task<string> GetPreviousPackageHash()
        {
            JObject info = await GetCurrentPackageInfo();
            string previousPackageShortHash = (string)info[PREVIOUS_PACKAGE_KEY];
            if (previousPackageShortHash == null)
            {
                return null;
            }

            JObject previousPackageMetadata = await GetPackage(previousPackageShortHash);
            if (previousPackageMetadata == null)
            {
                return null;
            }
            else
            {
                return (string)previousPackageMetadata[PACKAGE_HASH_KEY];
            }
        }

        public async Task<JObject> GetCurrentPackage()
        {
            StorageFolder currentPackageFolder = await GetCurrentPackageFolder();
            if (currentPackageFolder == null)
            {
                return null;
            }

            try
            {
                StorageFile packageFile = await currentPackageFolder.GetFileAsync(PACKAGE_FILE_NAME);
                return await CodePushUtils.GetJObjectFromFile(packageFile);
            }
            catch (IOException)
            {
                // Should not happen unless the update metadata was somehow deleted.
                return null;
            }
        }

        public async Task<JObject> GetPackage(string packageHash)
        {
            StorageFolder packageFolder = await GetPackageFolder(packageHash, false);
            if (packageFolder == null)
            {
                return null;
            }

            try
            {
                StorageFile packageFile = await packageFolder.GetFileAsync(PACKAGE_FILE_NAME);
                return await CodePushUtils.GetJObjectFromFile(packageFile);
            }
            catch (IOException)
            {
                return null;
            }
        }

        public string shortenPackageHash(string packageHash)
        {
            return packageHash.Substring(0, 8);
        }

        public async Task DownloadPackage(JObject updatePackage, string expectedBundleFileName, Progress<HttpProgress> downloadProgress)
        {
            StorageFolder codePushFolder = await GetCodePushFolder();
            string newUpdateHash = (string)updatePackage[PACKAGE_HASH_KEY];
            StorageFolder newUpdateFolder = await GetPackageFolder(newUpdateHash, false);
            if (newUpdateFolder != null)
            {
                // This removes any stale data in newPackageFolderPath that could have been left
                // uncleared due to a crash or error during the download or install process.
                await newUpdateFolder.DeleteAsync();
            }

            newUpdateFolder = await GetPackageFolder(newUpdateHash, true);
            StorageFile newUpdateMetadataFile = await newUpdateFolder.CreateFileAsync(PACKAGE_FILE_NAME);
            string downloadUrlString = (string)updatePackage[DOWNLOAD_URL_KEY];
            StorageFile downloadFile = await GetDownloadFile();
            Uri downloadUri = new Uri(downloadUrlString);

            // Download the file and send progress event asynchronously
            HttpRequestMessage request = new HttpRequestMessage(HttpMethod.Get, downloadUri);
            HttpClient client = new HttpClient();
            CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
            HttpResponseMessage response = await client.SendRequestAsync(request).AsTask(cancellationTokenSource.Token, downloadProgress);
            IInputStream inputStream = await response.Content.ReadAsInputStreamAsync();
            Stream downloadFileStream = await downloadFile.OpenStreamForWriteAsync();
            await RandomAccessStream.CopyAndCloseAsync(inputStream, downloadFileStream.AsOutputStream());
            try
            {
                // Unzip the downloaded file and then delete the zip
                StorageFolder unzippedFolder = await GetUnzippedFolder();
                ZipFile.ExtractToDirectory(downloadFile.Path, unzippedFolder.Path);
                await downloadFile.DeleteAsync();

                // Merge contents with current update based on the manifest
                StorageFile diffManifestFile = null;
                try
                {
                    diffManifestFile = await unzippedFolder.GetFileAsync(DIFF_MANIFEST_FILE_NAME);
                }
                catch (FileNotFoundException)
                {
                    // There is no diff manifest, so this is not a diff update.
                }

                if (diffManifestFile != null)
                {
                    StorageFolder currentPackageFolder = await GetCurrentPackageFolder();
                    if (currentPackageFolder == null)
                    {
                        throw new CodePushInvalidUpdateException("Received a diff update, but there is no current version to diff against.");
                    }

                    await CodePushUpdateUtils.CopyNecessaryFilesFromCurrentPackage(diffManifestFile, currentPackageFolder, newUpdateFolder);
                    await diffManifestFile.DeleteAsync();
                }

                await FileUtils.MergeDirectories(unzippedFolder, newUpdateFolder);
                await unzippedFolder.DeleteAsync();

                // For zip updates, we need to find the relative path to the jsBundle and save it in the
                // metadata so that we can find and run it easily the next time.
                string relativeBundlePath = await CodePushUpdateUtils.FindJSBundleInUpdateContents(newUpdateFolder, expectedBundleFileName);
                if (relativeBundlePath == null)
                {
                    throw new CodePushInvalidUpdateException("Update is invalid - A JS bundle file named \"" + expectedBundleFileName + "\" could not be found within the downloaded contents. Please check that you are releasing your CodePush updates using the exact same JS bundle file name that was shipped with your app's binary.");
                }
                else
                {
                    if (diffManifestFile != null)
                    {
                        // TODO verify hash for diff update
                        // CodePushUpdateUtils.verifyHashForDiffUpdate(newUpdateFolderPath, newUpdateHash);
                    }

                    updatePackage[RELATIVE_BUNDLE_PATH_KEY] = relativeBundlePath;
                }
            }
            catch (InvalidDataException)
            {
                // Downloaded file is not a zip, assume it is a jsbundle
                await downloadFile.RenameAsync(expectedBundleFileName);
                await downloadFile.MoveAsync(newUpdateFolder);
            }

            // Save metadata to the folder
            await FileIO.WriteTextAsync(newUpdateMetadataFile, JsonConvert.SerializeObject(updatePackage));
        }

        public async Task InstallPackage(JObject updatePackage, bool removePendingUpdate)
        {
            string packageHash = (string)updatePackage[PACKAGE_HASH_KEY];
            JObject info = await GetCurrentPackageInfo();
            if (removePendingUpdate)
            {
                StorageFolder currentPackageFolder = await GetCurrentPackageFolder();
                if (currentPackageFolder != null)
                {
                    await currentPackageFolder.DeleteAsync();
                }
            }
            else
            {
                string previousPackageHash = await GetPreviousPackageHash();
                if (previousPackageHash != null && !previousPackageHash.Equals(packageHash))
                {
                    StorageFolder previousPackageFolder = await GetPackageFolder(previousPackageHash, false);
                    if (previousPackageFolder != null)
                    {
                        await previousPackageFolder.DeleteAsync();
                    }
                }

                info[PREVIOUS_PACKAGE_KEY] = info[CURRENT_PACKAGE_KEY];
            }

            info[CURRENT_PACKAGE_KEY] = packageHash;
            await UpdateCurrentPackageInfo(info);
        }

        public async Task RollbackPackage()
        {
            JObject info = await GetCurrentPackageInfo();
            StorageFolder currentPackageFolder = await GetCurrentPackageFolder();
            if (currentPackageFolder != null)
            {
                await currentPackageFolder.DeleteAsync();
            }

            info[CURRENT_PACKAGE_KEY] = info[PREVIOUS_PACKAGE_KEY];
            info[PREVIOUS_PACKAGE_KEY] = null;
            await UpdateCurrentPackageInfo(info);
        }

        public void DownloadAndReplaceCurrentBundle(string remoteBundleUrl, string bundleFileName)
        {
            // TODO implement this method (only used in tests)
        }
        
        public async Task ClearUpdates()
        {
            await (await GetStatusFile()).DeleteAsync();
            await (await GetCodePushFolder()).DeleteAsync();
        }
    }
}
