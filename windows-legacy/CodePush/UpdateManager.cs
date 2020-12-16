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
            await (await GetCodePushFolderAsync().ConfigureAwait(false)).DeleteAsync().AsTask().ConfigureAwait(false);
        }

        internal async Task DownloadPackageAsync(JObject updatePackage, string expectedBundleFileName, Progress<HttpProgress> downloadProgress)
        {
            // Using its hash, get the folder where the new update will be saved
            StorageFolder codePushFolder = await GetCodePushFolderAsync().ConfigureAwait(false);
            var newUpdateHash = (string)updatePackage[CodePushConstants.PackageHashKey];
            StorageFolder newUpdateFolder = await GetPackageFolderAsync(newUpdateHash, false).ConfigureAwait(false);
            if (newUpdateFolder != null)
            {
                // This removes any stale data in newUpdateFolder that could have been left
                // uncleared due to a crash or error during the download or install process.
                await newUpdateFolder.DeleteAsync().AsTask().ConfigureAwait(false);
            }

            newUpdateFolder = await GetPackageFolderAsync(newUpdateHash, true).ConfigureAwait(false);
            StorageFile newUpdateMetadataFile = await newUpdateFolder.CreateFileAsync(CodePushConstants.PackageFileName).AsTask().ConfigureAwait(false);
            var downloadUrlString = (string)updatePackage[CodePushConstants.DownloadUrlKey];
            StorageFile downloadFile = await GetDownloadFileAsync().ConfigureAwait(false);
            var downloadUri = new Uri(downloadUrlString);

            // Download the file and send progress event asynchronously
            var request = new HttpRequestMessage(HttpMethod.Get, downloadUri);
            var client = new HttpClient();
            var cancellationTokenSource = new CancellationTokenSource();
            using (HttpResponseMessage response = await client.SendRequestAsync(request).AsTask(cancellationTokenSource.Token, downloadProgress).ConfigureAwait(false))
            using (IInputStream inputStream = await response.Content.ReadAsInputStreamAsync().AsTask().ConfigureAwait(false))
            using (IRandomAccessStream downloadFileStream = await downloadFile.OpenAsync(FileAccessMode.ReadWrite).AsTask().ConfigureAwait(false))
            {
                await RandomAccessStream.CopyAsync(inputStream, downloadFileStream).AsTask().ConfigureAwait(false);
            }

            try
            {
                // Unzip the downloaded file and then delete the zip
                StorageFolder unzippedFolder = await CreateUnzippedFolderAsync().ConfigureAwait(false);
                ZipFile.ExtractToDirectory(downloadFile.Path, unzippedFolder.Path);
                await downloadFile.DeleteAsync().AsTask().ConfigureAwait(false);

                // Merge contents with current update based on the manifest
                StorageFile diffManifestFile = (StorageFile)await unzippedFolder.TryGetItemAsync(CodePushConstants.DiffManifestFileName).AsTask().ConfigureAwait(false);
                if (diffManifestFile != null)
                {
                    StorageFolder currentPackageFolder = await GetCurrentPackageFolderAsync().ConfigureAwait(false);
                    if (currentPackageFolder == null)
                    {
                        throw new InvalidDataException("Received a diff update, but there is no current version to diff against.");
                    }

                    await UpdateUtils.CopyNecessaryFilesFromCurrentPackageAsync(diffManifestFile, currentPackageFolder, newUpdateFolder).ConfigureAwait(false);
                    await diffManifestFile.DeleteAsync().AsTask().ConfigureAwait(false);
                }

                await FileUtils.MergeFoldersAsync(unzippedFolder, newUpdateFolder).ConfigureAwait(false);
                await unzippedFolder.DeleteAsync().AsTask().ConfigureAwait(false);

                // For zip updates, we need to find the relative path to the jsBundle and save it in the
                // metadata so that we can find and run it easily the next time.
                string relativeBundlePath = await UpdateUtils.FindJSBundleInUpdateContentsAsync(newUpdateFolder, expectedBundleFileName).ConfigureAwait(false);
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
                await downloadFile.RenameAsync(expectedBundleFileName).AsTask().ConfigureAwait(false);
                await downloadFile.MoveAsync(newUpdateFolder).AsTask().ConfigureAwait(false);
            }
            /*TODO: ZipFile.ExtractToDirectory is not reliable and throws exceptions if:
            - path is too long
            it needs to be handled
            */

            // Save metadata to the folder
            await FileIO.WriteTextAsync(newUpdateMetadataFile, JsonConvert.SerializeObject(updatePackage)).AsTask().ConfigureAwait(false);
        }

        internal async Task<JObject> GetCurrentPackageAsync()
        {
            string packageHash = await GetCurrentPackageHashAsync().ConfigureAwait(false);
            return packageHash == null ? null : await GetPackageAsync(packageHash).ConfigureAwait(false);
        }

        internal async Task<StorageFile> GetCurrentPackageBundleAsync(string bundleFileName)
        {
            StorageFolder packageFolder = await GetCurrentPackageFolderAsync().ConfigureAwait(false);
            if (packageFolder == null)
            {
                return null;
            }

            JObject currentPackage = await GetCurrentPackageAsync().ConfigureAwait(false);
            var relativeBundlePath = (string)currentPackage[CodePushConstants.RelativeBundlePathKey];

            return relativeBundlePath == null
                ? await packageFolder.GetFileAsync(bundleFileName).AsTask().ConfigureAwait(false)
                : await packageFolder.GetFileAsync(relativeBundlePath).AsTask().ConfigureAwait(false);
        }

        internal async Task<string> GetCurrentPackageHashAsync()
        {
            JObject info = await GetCurrentPackageInfoAsync().ConfigureAwait(false);
            string currentPackageShortHash = (string)info[CodePushConstants.CurrentPackageKey];
            if (currentPackageShortHash == null)
            {
                return null;
            }

            JObject currentPackageMetadata = await GetPackageAsync(currentPackageShortHash).ConfigureAwait(false);
            return currentPackageMetadata == null ? null : (string)currentPackageMetadata[CodePushConstants.PackageHashKey];
        }

        internal async Task<JObject> GetPackageAsync(string packageHash)
        {
            StorageFolder packageFolder = await GetPackageFolderAsync(packageHash, false).ConfigureAwait(false);
            if (packageFolder == null)
            {
                return null;
            }

            try
            {
                StorageFile packageFile = await packageFolder.GetFileAsync(CodePushConstants.PackageFileName).AsTask().ConfigureAwait(false);
                return await CodePushUtils.GetJObjectFromFileAsync(packageFile).ConfigureAwait(false);
            }
            catch (IOException)
            {
                return null;
            }
        }

        internal async Task<StorageFolder> GetPackageFolderAsync(string packageHash, bool createIfNotExists)
        {
            StorageFolder codePushFolder = await GetCodePushFolderAsync().ConfigureAwait(false);
            try
            {
                packageHash = ShortenPackageHash(packageHash);
                return createIfNotExists
                    ? await codePushFolder.CreateFolderAsync(packageHash, CreationCollisionOption.OpenIfExists).AsTask().ConfigureAwait(false)
                    : await codePushFolder.GetFolderAsync(packageHash).AsTask().ConfigureAwait(false);
            }
            catch (FileNotFoundException)
            {
                return null;
            }
        }

        internal async Task<JObject> GetPreviousPackageAsync()
        {
            string packageHash = await GetPreviousPackageHashAsync().ConfigureAwait(false);
            return packageHash == null ? null : await GetPackageAsync(packageHash).ConfigureAwait(false);
        }

        internal async Task<string> GetPreviousPackageHashAsync()
        {
            JObject info = await GetCurrentPackageInfoAsync().ConfigureAwait(false);
            string previousPackageShortHash = (string)info[CodePushConstants.PreviousPackageKey];
            if (previousPackageShortHash == null)
            {
                return null;
            }

            JObject previousPackageMetadata = await GetPackageAsync(previousPackageShortHash).ConfigureAwait(false);
            return previousPackageMetadata == null ? null : (string)previousPackageMetadata[CodePushConstants.PackageHashKey];
        }

        internal async Task InstallPackageAsync(JObject updatePackage, bool currentUpdateIsPending)
        {
            var packageHash = (string)updatePackage[CodePushConstants.PackageHashKey];
            JObject info = await GetCurrentPackageInfoAsync().ConfigureAwait(false);
            if (currentUpdateIsPending)
            {
                // Don't back up current update to the "previous" position because
                // it is an unverified update which should not be rolled back to.
                StorageFolder currentPackageFolder = await GetCurrentPackageFolderAsync().ConfigureAwait(false);
                if (currentPackageFolder != null)
                {
                    await currentPackageFolder.DeleteAsync().AsTask().ConfigureAwait(false);
                }
            }
            else
            {
                string previousPackageHash = await GetPreviousPackageHashAsync().ConfigureAwait(false);
                if (previousPackageHash != null && !previousPackageHash.Equals(packageHash))
                {
                    StorageFolder previousPackageFolder = await GetPackageFolderAsync(previousPackageHash, false).ConfigureAwait(false);
                    if (previousPackageFolder != null)
                    {
                        await previousPackageFolder.DeleteAsync().AsTask().ConfigureAwait(false);
                    }
                }

                info[CodePushConstants.PreviousPackageKey] = info[CodePushConstants.CurrentPackageKey];
            }

            info[CodePushConstants.CurrentPackageKey] = packageHash;
            await UpdateCurrentPackageInfoAsync(info).ConfigureAwait(false);
        }

        internal async Task RollbackPackageAsync()
        {
            JObject info = await GetCurrentPackageInfoAsync().ConfigureAwait(false);
            StorageFolder currentPackageFolder = await GetCurrentPackageFolderAsync().ConfigureAwait(false);
            if (currentPackageFolder != null)
            {
                await currentPackageFolder.DeleteAsync().AsTask().ConfigureAwait(false);
            }

            info[CodePushConstants.CurrentPackageKey] = info[CodePushConstants.PreviousPackageKey];
            info[CodePushConstants.PreviousPackageKey] = null;
            await UpdateCurrentPackageInfoAsync(info).ConfigureAwait(false);
        }

        #endregion

        #region Private methods

        private async Task<StorageFolder> GetCodePushFolderAsync()
        {
            return await ApplicationData.Current.LocalFolder.CreateFolderAsync(CodePushConstants.CodePushFolderPrefix, CreationCollisionOption.OpenIfExists).AsTask().ConfigureAwait(false);
        }

        private async Task<StorageFolder> GetCurrentPackageFolderAsync()
        {
            JObject info = await GetCurrentPackageInfoAsync().ConfigureAwait(false);
            var packageHash = (string)info[CodePushConstants.CurrentPackageKey];
            return packageHash == null ? null : await GetPackageFolderAsync(packageHash, false).ConfigureAwait(false);
        }

        private async Task<JObject> GetCurrentPackageInfoAsync()
        {
            StorageFile statusFile = await GetStatusFileAsync().ConfigureAwait(false);
            return await CodePushUtils.GetJObjectFromFileAsync(statusFile).ConfigureAwait(false);
        }

        private async Task<StorageFile> GetDownloadFileAsync()
        {
            var codePushFolder = await GetCodePushFolderAsync().ConfigureAwait(false);
            return await codePushFolder.CreateFileAsync(CodePushConstants.DownloadFileName, CreationCollisionOption.OpenIfExists).AsTask().ConfigureAwait(false);
        }

        private async Task<StorageFile> GetStatusFileAsync()
        {
            StorageFolder codePushFolder = await GetCodePushFolderAsync().ConfigureAwait(false);
            return await codePushFolder.CreateFileAsync(CodePushConstants.StatusFileName, CreationCollisionOption.OpenIfExists).AsTask().ConfigureAwait(false);
        }

        private async Task<StorageFolder> CreateUnzippedFolderAsync()
        {
            StorageFolder codePushFolder = await GetCodePushFolderAsync().ConfigureAwait(false);
            var unzippedFolder = await codePushFolder.TryGetItemAsync(CodePushConstants.UnzippedFolderName).AsTask().ConfigureAwait(false);

            if (unzippedFolder != null)
            {
                await unzippedFolder.DeleteAsync();
            }

            return await codePushFolder.CreateFolderAsync(CodePushConstants.UnzippedFolderName, CreationCollisionOption.OpenIfExists).AsTask().ConfigureAwait(false);
        }

        private string ShortenPackageHash(string longPackageHash)
        {
            return longPackageHash.Substring(0, 8);
        }

        private async Task UpdateCurrentPackageInfoAsync(JObject packageInfo)
        {
            await FileIO.WriteTextAsync(await GetStatusFileAsync().ConfigureAwait(false), JsonConvert.SerializeObject(packageInfo)).AsTask().ConfigureAwait(false);
        }

        #endregion
    }
}
