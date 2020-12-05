using CodePush.Net46.Adapters.Http;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using PCLStorage;
using System;
using System.IO;
using System.IO.Compression;
using System.Runtime.CompilerServices;
using System.Threading.Tasks;

namespace CodePush.ReactNative
{
    internal class UpdateManager
    {
        #region Internal methods

        internal async Task ClearUpdatesAsync()
        {
            await (await UpdateUtils.GetCodePushFolderAsync().ConfigureAwait(false)).DeleteAsync().ConfigureAwait(false);
        }

        internal async Task DownloadPackageAsync(JObject updatePackage, string expectedBundleFileName, Progress<HttpProgress> downloadProgress)
        {
            // Using its hash, get the folder where the new update will be saved
            var codePushFolder = await UpdateUtils.GetCodePushFolderAsync().ConfigureAwait(false);
            var newUpdateHash = (string)updatePackage[CodePushConstants.PackageHashKey];
            var newUpdateFolder = await GetPackageFolderAsync(newUpdateHash, false).ConfigureAwait(false);
            if (newUpdateFolder != null)
            {
                // This removes any stale data in newUpdateFolder that could have been left
                // uncleared due to a crash or error during the download or install process.
                await newUpdateFolder.DeleteAsync().ConfigureAwait(false);
            }

            newUpdateFolder = await GetPackageFolderAsync(newUpdateHash, true).ConfigureAwait(false);
            var newUpdateMetadataFile = await newUpdateFolder.CreateFileAsync(CodePushConstants.PackageFileName, CreationCollisionOption.ReplaceExisting).ConfigureAwait(false);
            var downloadUrlString = (string)updatePackage[CodePushConstants.DownloadUrlKey];
            var downloadFile = await GetDownloadFileAsync().ConfigureAwait(false);

            await UpdateUtils.DownloadBundleAsync(downloadUrlString, downloadFile.Path, downloadProgress);

            try
            {
                // Unzip the downloaded file and then delete the zip
                var unzippedFolder = await CreateUnzippedFolderAsync().ConfigureAwait(false);
                /**
                 * TODO:
                 *  1) ZipFile.ExtractToDirectory is not reliable and throws exception if:
                 *      - path is too long (> 250 chars)
                 *
                 *  2) Un-zipping is quite long operation. Does it make sense for async?
                 *  await UpdateUtils.UnzipBundleAsync(downloadFile.Path, unzippedFolder.Path);
                 *
                 *  Possible implementation
                 *
                 *  internal async static Task UnzipBundleAsync(string zipFileName, string targetDir)
                 *  {
                 *    await Task.Run(() =>
                 *      {
                 *        ZipFile.ExtractToDirectory(zipFileName, targetDir)
                 *        return Task.CompletedTask;
                 *      });
                 *  }
                */
                ZipFile.ExtractToDirectory(downloadFile.Path, unzippedFolder.Path);
                await downloadFile.DeleteAsync().ConfigureAwait(false);

                // Merge contents with current update based on the manifest
                IFile diffManifestFile = null;
                try
                {
                    diffManifestFile = await unzippedFolder.GetFileAsync(CodePushConstants.DiffManifestFileName).ConfigureAwait(false);
                }
                catch (FileNotFoundException)
                {
                    //file may not be present in folder just skip it
                }
                if (diffManifestFile != null)
                {
                    var currentPackageFolder = await GetCurrentPackageFolderAsync().ConfigureAwait(false);
                    if (currentPackageFolder == null)
                    {
                        throw new InvalidDataException("Received a diff update, but there is no current version to diff against.");
                    }

                    await UpdateUtils.CopyNecessaryFilesFromCurrentPackageAsync(diffManifestFile, currentPackageFolder, newUpdateFolder).ConfigureAwait(false);
                    await diffManifestFile.DeleteAsync().ConfigureAwait(false);
                }

                await FileUtils.MergeFoldersAsync(unzippedFolder, newUpdateFolder).ConfigureAwait(false);
                await unzippedFolder.DeleteAsync().ConfigureAwait(false);

                // For zip updates, we need to find the relative path to the jsBundle and save it in the
                // metadata so that we can find and run it easily the next time.
                var relativeBundlePath = await UpdateUtils.FindJSBundleInUpdateContentsAsync(newUpdateFolder, expectedBundleFileName).ConfigureAwait(false);
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
                await downloadFile.RenameAsync(expectedBundleFileName).ConfigureAwait(false);
                await downloadFile.MoveAsync(newUpdateFolder.Path, NameCollisionOption.ReplaceExisting).ConfigureAwait(false);
            }

            // Save metadata to the folder
            await newUpdateMetadataFile.WriteAllTextAsync(JsonConvert.SerializeObject(updatePackage)).ConfigureAwait(false);
        }

        internal async Task<JObject> GetCurrentPackageAsync()
        {
            var packageHash = await GetCurrentPackageHashAsync().ConfigureAwait(false);
            return packageHash == null ? null : await GetPackageAsync(packageHash).ConfigureAwait(false);
        }

        internal async Task<IFile> GetCurrentPackageBundleAsync(string bundleFileName)
        {
            var packageFolder = await GetCurrentPackageFolderAsync().ConfigureAwait(false);
            if (packageFolder == null)
            {
                return null;
            }

            var currentPackage = await GetCurrentPackageAsync().ConfigureAwait(false);
            var relativeBundlePath = (string)currentPackage[CodePushConstants.RelativeBundlePathKey];

            return relativeBundlePath == null
                ? await packageFolder.GetFileAsync(bundleFileName).ConfigureAwait(false)
                : await packageFolder.GetFileAsync(relativeBundlePath).ConfigureAwait(false);
        }

        internal async Task<string> GetCurrentPackageHashAsync()
        {
            var info = await GetCurrentPackageInfoAsync().ConfigureAwait(false);
            var currentPackageShortHash = (string)info[CodePushConstants.CurrentPackageKey];
            if (currentPackageShortHash == null)
            {
                return null;
            }

            var currentPackageMetadata = await GetPackageAsync(currentPackageShortHash).ConfigureAwait(false);
            return currentPackageMetadata == null ? null : (string)currentPackageMetadata[CodePushConstants.PackageHashKey];
        }

        internal async Task<JObject> GetPackageAsync(string packageHash)
        {
            var packageFolder = await GetPackageFolderAsync(packageHash, false).ConfigureAwait(false);
            if (packageFolder == null)
            {
                return null;
            }

            try
            {
                var packageFile = await packageFolder.GetFileAsync(CodePushConstants.PackageFileName).ConfigureAwait(false);
                return await CodePushUtils.GetJObjectFromFileAsync(packageFile).ConfigureAwait(false);
            }
            catch (IOException)
            {
                return null;
            }
        }

        internal async Task<IFolder> GetPackageFolderAsync(string packageHash, bool createIfNotExists)
        {
            var codePushFolder = await UpdateUtils.GetCodePushFolderAsync().ConfigureAwait(false);
            try
            {
                packageHash = ShortenPackageHash(packageHash);
                return createIfNotExists
                    ? await codePushFolder.CreateFolderAsync(packageHash, CreationCollisionOption.OpenIfExists).ConfigureAwait(false)
                    : await codePushFolder.GetFolderAsync(packageHash).ConfigureAwait(false);
            }
            catch (FileNotFoundException)
            {
                return null;
            }
            catch (DirectoryNotFoundException)
            {
                return null;
            }
        }

        internal async Task<JObject> GetPreviousPackageAsync()
        {
            var packageHash = await GetPreviousPackageHashAsync().ConfigureAwait(false);
            return packageHash == null ? null : await GetPackageAsync(packageHash).ConfigureAwait(false);
        }

        internal async Task<string> GetPreviousPackageHashAsync()
        {
            var info = await GetCurrentPackageInfoAsync().ConfigureAwait(false);
            var previousPackageShortHash = (string)info[CodePushConstants.PreviousPackageKey];
            if (previousPackageShortHash == null)
            {
                return null;
            }

            var previousPackageMetadata = await GetPackageAsync(previousPackageShortHash).ConfigureAwait(false);
            return previousPackageMetadata == null ? null : (string)previousPackageMetadata[CodePushConstants.PackageHashKey];
        }

        internal async Task InstallPackageAsync(JObject updatePackage, bool currentUpdateIsPending)
        {
            var packageHash = (string)updatePackage[CodePushConstants.PackageHashKey];
            var info = await GetCurrentPackageInfoAsync().ConfigureAwait(false);
            if (currentUpdateIsPending)
            {
                // Don't back up current update to the "previous" position because
                // it is an unverified update which should not be rolled back to.
                var currentPackageFolder = await GetCurrentPackageFolderAsync().ConfigureAwait(false);
                if (currentPackageFolder != null)
                {
                    await currentPackageFolder.DeleteAsync().ConfigureAwait(false);
                }
            }
            else
            {
                var previousPackageHash = await GetPreviousPackageHashAsync().ConfigureAwait(false);
                if (previousPackageHash != null && !previousPackageHash.Equals(packageHash))
                {
                    var previousPackageFolder = await GetPackageFolderAsync(previousPackageHash, false).ConfigureAwait(false);
                    if (previousPackageFolder != null)
                    {
                        await previousPackageFolder.DeleteAsync().ConfigureAwait(false);
                    }
                }

                info[CodePushConstants.PreviousPackageKey] = info[CodePushConstants.CurrentPackageKey];
            }

            info[CodePushConstants.CurrentPackageKey] = packageHash;
            await UpdateCurrentPackageInfoAsync(info).ConfigureAwait(false);
        }

        internal async Task RollbackPackageAsync()
        {
            var info = await GetCurrentPackageInfoAsync().ConfigureAwait(false);
            var currentPackageFolder = await GetCurrentPackageFolderAsync().ConfigureAwait(false);
            if (currentPackageFolder != null)
            {
                await currentPackageFolder.DeleteAsync().ConfigureAwait(false);
            }

            info[CodePushConstants.CurrentPackageKey] = info[CodePushConstants.PreviousPackageKey];
            info[CodePushConstants.PreviousPackageKey] = null;
            await UpdateCurrentPackageInfoAsync(info).ConfigureAwait(false);
        }

        #endregion

        #region Private methods

        private async Task<IFolder> GetCurrentPackageFolderAsync()
        {
            var info = await GetCurrentPackageInfoAsync().ConfigureAwait(false);
            if (info == null)
            {
                return null;
            }
            var packageHash = (string)info[CodePushConstants.CurrentPackageKey];
            return packageHash == null ? null : await GetPackageFolderAsync(packageHash, false).ConfigureAwait(false);
        }

        private async Task<JObject> GetCurrentPackageInfoAsync()
        {
            var statusFile = await GetStatusFileAsync().ConfigureAwait(false);
            var info = await CodePushUtils.GetJObjectFromFileAsync(statusFile).ConfigureAwait(false);

            if (info != null)
            {
                return info;
            }

            // info file has been corrupted - re-create it
            await statusFile.DeleteAsync().ConfigureAwait(false);
            statusFile = await GetStatusFileAsync().ConfigureAwait(false);
            return await CodePushUtils.GetJObjectFromFileAsync(statusFile).ConfigureAwait(false);
        }

        private async Task<IFile> GetDownloadFileAsync()
        {
            var codePushFolder = await UpdateUtils.GetCodePushFolderAsync().ConfigureAwait(false);
            return await codePushFolder.CreateFileAsync(CodePushConstants.DownloadFileName, CreationCollisionOption.OpenIfExists).ConfigureAwait(false);
        }

        private async Task<IFile> GetStatusFileAsync()
        {
            var codePushFolder = await UpdateUtils.GetCodePushFolderAsync().ConfigureAwait(false);
            return await codePushFolder.CreateFileAsync(CodePushConstants.StatusFileName, CreationCollisionOption.OpenIfExists).ConfigureAwait(false);
        }

        private async Task<IFolder> CreateUnzippedFolderAsync()
        {
            var codePushFolder = await UpdateUtils.GetCodePushFolderAsync().ConfigureAwait(false);
            var isUnzippedFolderExists = await codePushFolder.CheckExistsAsync(CodePushConstants.UnzippedFolderName).ConfigureAwait(false);

            if (isUnzippedFolderExists != ExistenceCheckResult.NotFound)
            {
                await codePushFolder.GetFolderAsync(CodePushConstants.UnzippedFolderName).ContinueWith((existingFolder) => existingFolder.Result.DeleteAsync());
            }

            return await codePushFolder.CreateFolderAsync(CodePushConstants.UnzippedFolderName, CreationCollisionOption.OpenIfExists).ConfigureAwait(false);
        }

        private string ShortenPackageHash(string longPackageHash)
        {
            return longPackageHash.Substring(0, 8);
        }

        private async Task UpdateCurrentPackageInfoAsync(JObject packageInfo)
        {
            var file = await GetStatusFileAsync().ConfigureAwait(false);
            await file.WriteAllTextAsync(JsonConvert.SerializeObject(packageInfo)).ConfigureAwait(false);
        }
        #endregion
    }
}
