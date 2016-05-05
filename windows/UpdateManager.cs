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
        internal async Task ClearUpdates()
        {
            await (await GetCodePushFolder()).DeleteAsync();
        }

        internal async Task DownloadPackage(JObject updatePackage, string expectedBundleFileName, Progress<HttpProgress> downloadProgress)
        {
            // Using its hash, get the folder where the new update will be saved 
            StorageFolder codePushFolder = await GetCodePushFolder();
            var newUpdateHash = (string)updatePackage[CodePushConstants.PackageHashKey];
            StorageFolder newUpdateFolder = await GetPackageFolder(newUpdateHash, false);
            if (newUpdateFolder != null)
            {
                // This removes any stale data in newPackageFolderPath that could have been left
                // uncleared due to a crash or error during the download or install process.
                await newUpdateFolder.DeleteAsync();
            }

            newUpdateFolder = await GetPackageFolder(newUpdateHash, true);
            StorageFile newUpdateMetadataFile = await newUpdateFolder.CreateFileAsync(CodePushConstants.PackageFileName);
            var downloadUrlString = (string)updatePackage[CodePushConstants.DownloadUrlKey];
            StorageFile downloadFile = await GetDownloadFile();
            var downloadUri = new Uri(downloadUrlString);

            // Download the file and send progress event asynchronously
            var request = new HttpRequestMessage(HttpMethod.Get, downloadUri);
            var client = new HttpClient();
            var cancellationTokenSource = new CancellationTokenSource();
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
                StorageFile diffManifestFile = (StorageFile)await unzippedFolder.TryGetItemAsync(CodePushConstants.DiffManifestFileName);
                if (diffManifestFile != null)
                {
                    StorageFolder currentPackageFolder = await GetCurrentPackageFolder();
                    if (currentPackageFolder == null)
                    {
                        throw new InvalidDataException("Received a diff update, but there is no current version to diff against.");
                    }

                    await UpdateUtils.CopyNecessaryFilesFromCurrentPackage(diffManifestFile, currentPackageFolder, newUpdateFolder);
                    await diffManifestFile.DeleteAsync();
                }

                await FileUtils.MergeFolders(unzippedFolder, newUpdateFolder);
                await unzippedFolder.DeleteAsync();

                // For zip updates, we need to find the relative path to the jsBundle and save it in the
                // metadata so that we can find and run it easily the next time.
                string relativeBundlePath = await UpdateUtils.FindJSBundleInUpdateContents(newUpdateFolder, expectedBundleFileName);
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

        internal async Task<JObject> GetCurrentPackage()
        {
            string packageHash = await GetCurrentPackageHash();
            return packageHash == null ? null : await GetPackage(packageHash);
        }

        internal async Task<StorageFile> GetCurrentPackageBundle(string bundleFileName)
        {
            StorageFolder packageFolder = await GetCurrentPackageFolder();
            if (packageFolder == null)
            {
                return null;
            }

            JObject currentPackage = await GetCurrentPackage();
            var relativeBundlePath = (string)currentPackage[CodePushConstants.RelativeBundlePathKey];

            return relativeBundlePath == null 
                ? await packageFolder.GetFileAsync(bundleFileName)
                : await packageFolder.GetFileAsync(relativeBundlePath);
        }

        internal async Task<string> GetCurrentPackageHash()
        {
            JObject info = await GetCurrentPackageInfo();
            string currentPackageShortHash = (string)info[CodePushConstants.CurrentPackageKey];
            if (currentPackageShortHash == null)
            {
                return null;
            }

            JObject currentPackageMetadata = await GetPackage(currentPackageShortHash);
            return currentPackageMetadata == null ? null : (string)currentPackageMetadata[CodePushConstants.PackageHashKey];
        }

        internal async Task<JObject> GetPackage(string packageHash)
        {
            StorageFolder packageFolder = await GetPackageFolder(packageHash, false);
            if (packageFolder == null)
            {
                return null;
            }

            try
            {
                StorageFile packageFile = await packageFolder.GetFileAsync(CodePushConstants.PackageFileName);
                return await CodePushUtils.GetJObjectFromFile(packageFile);
            }
            catch (IOException)
            {
                return null;
            }
        }

        internal async Task<StorageFolder> GetPackageFolder(string packageHash, bool createIfNotExists)
        {
            StorageFolder codePushFolder = await GetCodePushFolder();
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

        internal async Task<JObject> GetPreviousPackage()
        {
            string packageHash = await GetPreviousPackageHash();
            return packageHash == null ? null : await GetPackage(packageHash);
        }

        internal async Task<string> GetPreviousPackageHash()
        {
            JObject info = await GetCurrentPackageInfo();
            string previousPackageShortHash = (string)info[CodePushConstants.PreviousPackageKey];
            if (previousPackageShortHash == null)
            {
                return null;
            }

            JObject previousPackageMetadata = await GetPackage(previousPackageShortHash);
            return previousPackageMetadata == null ? null : (string)previousPackageMetadata[CodePushConstants.PackageHashKey];
        }

        internal async Task InstallPackage(JObject updatePackage, bool currentUpdateIsPending)
        {
            var packageHash = (string)updatePackage[CodePushConstants.PackageHashKey];
            JObject info = await GetCurrentPackageInfo();
            if (currentUpdateIsPending)
            {
                // Don't back up current update to the "previous" position because
                // it is an unverified update which should not be rolled back to.
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

                info[CodePushConstants.PreviousPackageKey] = info[CodePushConstants.CurrentPackageKey];
            }

            info[CodePushConstants.CurrentPackageKey] = packageHash;
            await UpdateCurrentPackageInfo(info);
        }

        internal async Task RollbackPackage()
        {
            JObject info = await GetCurrentPackageInfo();
            StorageFolder currentPackageFolder = await GetCurrentPackageFolder();
            if (currentPackageFolder != null)
            {
                await currentPackageFolder.DeleteAsync();
            }

            info[CodePushConstants.CurrentPackageKey] = info[CodePushConstants.PreviousPackageKey];
            info[CodePushConstants.PreviousPackageKey] = null;
            await UpdateCurrentPackageInfo(info);
        }
        #endregion

        #region Private methods
        private async Task<StorageFolder> GetCodePushFolder()
        {
            return await ApplicationData.Current.LocalFolder.CreateFolderAsync(CodePushConstants.CodePushFolderPrefix, CreationCollisionOption.OpenIfExists);
        }

        private async Task<StorageFolder> GetCurrentPackageFolder()
        {
            JObject info = await GetCurrentPackageInfo();
            var packageHash = (string)info[CodePushConstants.CurrentPackageKey];
            return packageHash == null ? null : await GetPackageFolder(packageHash, false);
        }

        private async Task<JObject> GetCurrentPackageInfo()
        {
            StorageFile statusFile = await GetStatusFile();
            return await CodePushUtils.GetJObjectFromFile(statusFile);
        }

        private async Task<StorageFile> GetDownloadFile()
        {
            var codePushFolder = await GetCodePushFolder();
            return await codePushFolder.CreateFileAsync(CodePushConstants.DownloadFileName, CreationCollisionOption.OpenIfExists);
        }

        private async Task<StorageFile> GetStatusFile()
        {
            StorageFolder codePushFolder = await GetCodePushFolder();
            return await codePushFolder.CreateFileAsync(CodePushConstants.StatusFileName, CreationCollisionOption.OpenIfExists);
        }

        private async Task<StorageFolder> GetUnzippedFolder()
        {
            StorageFolder codePushFolder = await GetCodePushFolder();
            return await codePushFolder.CreateFolderAsync(CodePushConstants.UnzippedFolderName, CreationCollisionOption.OpenIfExists);
        }

        private string ShortenPackageHash(string longPackageHash)
        {
            return longPackageHash.Substring(0, 8);
        }

        private async Task UpdateCurrentPackageInfo(JObject packageInfo)
        {
            await FileIO.WriteTextAsync(await GetStatusFile(), JsonConvert.SerializeObject(packageInfo));
        }
        #endregion
    }
}
