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

namespace CodePush.ReactNative
{
    internal class UpdateManager
    {
        #region Internal methods

        internal async Task ClearUpdatesAsync()
        {
            await (await GetCodePushFolderAsync()).DeleteAsync();
        }

        internal async Task DownloadPackageAsync(JObject updatePackage, string expectedBundleFileName, Progress<HttpProgress> downloadProgress)
        {
            // Using its hash, get the folder where the new update will be saved 
            StorageFolder codePushFolder = await GetCodePushFolderAsync();
            var newUpdateHash = (string)updatePackage[CodePushConstants.PackageHashKey];
            StorageFolder newUpdateFolder = await GetPackageFolderAsync(newUpdateHash, false);
            if (newUpdateFolder != null)
            {
                // This removes any stale data in newPackageFolderPath that could have been left
                // uncleared due to a crash or error during the download or install process.
                await newUpdateFolder.DeleteAsync();
            }

            newUpdateFolder = await GetPackageFolderAsync(newUpdateHash, true);
            StorageFile newUpdateMetadataFile = await newUpdateFolder.CreateFileAsync(CodePushConstants.PackageFileName);
            var downloadUrlString = (string)updatePackage[CodePushConstants.DownloadUrlKey];
            StorageFile downloadFile = await GetDownloadFileAsync();
            var downloadUri = new Uri(downloadUrlString);

            // Download the file and send progress event asynchronously
            var request = new HttpRequestMessage(HttpMethod.Get, downloadUri);
            var client = new HttpClient();
            var cancellationTokenSource = new CancellationTokenSource();
            using (HttpResponseMessage response = await client.SendRequestAsync(request).AsTask(cancellationTokenSource.Token, downloadProgress))
            using (IInputStream inputStream = await response.Content.ReadAsInputStreamAsync())
            using (IRandomAccessStream downloadFileStream = await downloadFile.OpenAsync(FileAccessMode.ReadWrite))
            {
                await RandomAccessStream.CopyAsync(inputStream, downloadFileStream);
            }

            try
            {
                // Unzip the downloaded file and then delete the zip
                StorageFolder unzippedFolder = await GetUnzippedFolderAsync();
                ZipFile.ExtractToDirectory(downloadFile.Path, unzippedFolder.Path);
                await downloadFile.DeleteAsync();

                // Merge contents with current update based on the manifest
                StorageFile diffManifestFile = (StorageFile)await unzippedFolder.TryGetItemAsync(CodePushConstants.DiffManifestFileName);
                if (diffManifestFile != null)
                {
                    StorageFolder currentPackageFolder = await GetCurrentPackageFolderAsync();
                    if (currentPackageFolder == null)
                    {
                        throw new InvalidDataException("Received a diff update, but there is no current version to diff against.");
                    }

                    await UpdateUtils.CopyNecessaryFilesFromCurrentPackageAsync(diffManifestFile, currentPackageFolder, newUpdateFolder);
                    await diffManifestFile.DeleteAsync();
                }

                await FileUtils.MergeFoldersAsync(unzippedFolder, newUpdateFolder);
                await unzippedFolder.DeleteAsync();

                // For zip updates, we need to find the relative path to the jsBundle and save it in the
                // metadata so that we can find and run it easily the next time.
                string relativeBundlePath = await UpdateUtils.FindJSBundleInUpdateContentsAsync(newUpdateFolder, expectedBundleFileName);
                if (relativeBundlePath == null)
                {
                    throw new InvalidDataException("Update is invalid - A JS bundle file named \"" + expectedBundleFileName + "\" could not be found within the downloaded contents. Please check that you are releasing your CodePush updates using the exact same JS bundle file name that was shipped with your app's binary.");
                }
                else
                {
                    if (diffManifestFile != null)
                    {
                        // TODO verify hash for diff update
                        // CodePushUpdateUtils.verifyHashForDiffUpdate(newUpdateFolderPath, newUpdateHash);
                    }

                    updatePackage[CodePushConstants.RelativeBundlePathKey] = relativeBundlePath;
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

        internal async Task<JObject> GetCurrentPackageAsync()
        {
            string packageHash = await GetCurrentPackageHashAsync();
            return packageHash == null ? null : await GetPackageAsync(packageHash);
        }

        internal async Task<StorageFile> GetCurrentPackageBundleAsync(string bundleFileName)
        {
            StorageFolder packageFolder = await GetCurrentPackageFolderAsync();
            if (packageFolder == null)
            {
                return null;
            }

            JObject currentPackage = await GetCurrentPackageAsync();
            var relativeBundlePath = (string)currentPackage[CodePushConstants.RelativeBundlePathKey];

            return relativeBundlePath == null 
                ? await packageFolder.GetFileAsync(bundleFileName)
                : await packageFolder.GetFileAsync(relativeBundlePath);
        }

        internal async Task<string> GetCurrentPackageHashAsync()
        {
            JObject info = await GetCurrentPackageInfoAsync();
            string currentPackageShortHash = (string)info[CodePushConstants.CurrentPackageKey];
            if (currentPackageShortHash == null)
            {
                return null;
            }

            JObject currentPackageMetadata = await GetPackageAsync(currentPackageShortHash);
            return currentPackageMetadata == null ? null : (string)currentPackageMetadata[CodePushConstants.PackageHashKey];
        }

        internal async Task<JObject> GetPackageAsync(string packageHash)
        {
            StorageFolder packageFolder = await GetPackageFolderAsync(packageHash, false);
            if (packageFolder == null)
            {
                return null;
            }

            try
            {
                StorageFile packageFile = await packageFolder.GetFileAsync(CodePushConstants.PackageFileName);
                return await CodePushUtils.GetJObjectFromFileAsync(packageFile);
            }
            catch (IOException)
            {
                return null;
            }
        }

        internal async Task<StorageFolder> GetPackageFolderAsync(string packageHash, bool createIfNotExists)
        {
            StorageFolder codePushFolder = await GetCodePushFolderAsync();
            try
            {
                packageHash = ShortenPackageHash(packageHash);
                return createIfNotExists
                    ? await codePushFolder.CreateFolderAsync(packageHash, CreationCollisionOption.OpenIfExists)
                    : await codePushFolder.GetFolderAsync(packageHash);
            }
            catch (FileNotFoundException)
            {
                return null;
            }
        }

        internal async Task<JObject> GetPreviousPackageAsync()
        {
            string packageHash = await GetPreviousPackageHashAsync();
            return packageHash == null ? null : await GetPackageAsync(packageHash);
        }

        internal async Task<string> GetPreviousPackageHashAsync()
        {
            JObject info = await GetCurrentPackageInfoAsync();
            string previousPackageShortHash = (string)info[CodePushConstants.PreviousPackageKey];
            if (previousPackageShortHash == null)
            {
                return null;
            }

            JObject previousPackageMetadata = await GetPackageAsync(previousPackageShortHash);
            return previousPackageMetadata == null ? null : (string)previousPackageMetadata[CodePushConstants.PackageHashKey];
        }

        internal async Task InstallPackageAsync(JObject updatePackage, bool currentUpdateIsPending)
        {
            var packageHash = (string)updatePackage[CodePushConstants.PackageHashKey];
            JObject info = await GetCurrentPackageInfoAsync();
            if (currentUpdateIsPending)
            {
                // Don't back up current update to the "previous" position because
                // it is an unverified update which should not be rolled back to.
                StorageFolder currentPackageFolder = await GetCurrentPackageFolderAsync();
                if (currentPackageFolder != null)
                {
                    await currentPackageFolder.DeleteAsync();
                }
            }
            else
            {
                string previousPackageHash = await GetPreviousPackageHashAsync();
                if (previousPackageHash != null && !previousPackageHash.Equals(packageHash))
                {
                    StorageFolder previousPackageFolder = await GetPackageFolderAsync(previousPackageHash, false);
                    if (previousPackageFolder != null)
                    {
                        await previousPackageFolder.DeleteAsync();
                    }
                }

                info[CodePushConstants.PreviousPackageKey] = info[CodePushConstants.CurrentPackageKey];
            }

            info[CodePushConstants.CurrentPackageKey] = packageHash;
            await UpdateCurrentPackageInfoAsync(info);
        }

        internal async Task RollbackPackageAsync()
        {
            JObject info = await GetCurrentPackageInfoAsync();
            StorageFolder currentPackageFolder = await GetCurrentPackageFolderAsync();
            if (currentPackageFolder != null)
            {
                await currentPackageFolder.DeleteAsync();
            }

            info[CodePushConstants.CurrentPackageKey] = info[CodePushConstants.PreviousPackageKey];
            info[CodePushConstants.PreviousPackageKey] = null;
            await UpdateCurrentPackageInfoAsync(info);
        }

        #endregion

        #region Private methods

        private async Task<StorageFolder> GetCodePushFolderAsync()
        {
            return await ApplicationData.Current.LocalFolder.CreateFolderAsync(CodePushConstants.CodePushFolderPrefix, CreationCollisionOption.OpenIfExists);
        }

        private async Task<StorageFolder> GetCurrentPackageFolderAsync()
        {
            JObject info = await GetCurrentPackageInfoAsync();
            var packageHash = (string)info[CodePushConstants.CurrentPackageKey];
            return packageHash == null ? null : await GetPackageFolderAsync(packageHash, false);
        }

        private async Task<JObject> GetCurrentPackageInfoAsync()
        {
            StorageFile statusFile = await GetStatusFileAsync();
            return await CodePushUtils.GetJObjectFromFileAsync(statusFile);
        }

        private async Task<StorageFile> GetDownloadFileAsync()
        {
            var codePushFolder = await GetCodePushFolderAsync();
            return await codePushFolder.CreateFileAsync(CodePushConstants.DownloadFileName, CreationCollisionOption.OpenIfExists);
        }

        private async Task<StorageFile> GetStatusFileAsync()
        {
            StorageFolder codePushFolder = await GetCodePushFolderAsync();
            return await codePushFolder.CreateFileAsync(CodePushConstants.StatusFileName, CreationCollisionOption.OpenIfExists);
        }

        private async Task<StorageFolder> GetUnzippedFolderAsync()
        {
            StorageFolder codePushFolder = await GetCodePushFolderAsync();
            return await codePushFolder.CreateFolderAsync(CodePushConstants.UnzippedFolderName, CreationCollisionOption.OpenIfExists);
        }

        private string ShortenPackageHash(string longPackageHash)
        {
            return longPackageHash.Substring(0, 8);
        }

        private async Task UpdateCurrentPackageInfoAsync(JObject packageInfo)
        {
            await FileIO.WriteTextAsync(await GetStatusFileAsync(), JsonConvert.SerializeObject(packageInfo));
        }

        #endregion
    }
}
