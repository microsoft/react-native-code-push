using Newtonsoft.Json.Linq;
using System;
using System.IO;
using System.Threading.Tasks;
using Windows.Storage;

namespace CodePush.ReactNative
{
    internal class UpdateUtils
    {
        internal async static Task CopyNecessaryFilesFromCurrentPackageAsync(StorageFile diffManifestFile, StorageFolder currentPackageFolder, StorageFolder newPackageFolder)
        {
            await FileUtils.MergeFoldersAsync(currentPackageFolder, newPackageFolder).ConfigureAwait(false);
            JObject diffManifest = await CodePushUtils.GetJObjectFromFileAsync(diffManifestFile).ConfigureAwait(false);
            var deletedFiles = (JArray)diffManifest["deletedFiles"];
            foreach (string fileNameToDelete in deletedFiles)
            {
                StorageFile fileToDelete = await newPackageFolder.GetFileAsync(fileNameToDelete.Replace("/", "\\")).AsTask().ConfigureAwait(false);
                await fileToDelete.DeleteAsync().AsTask().ConfigureAwait(false);
            }
        }

        internal async static Task<string> FindJSBundleInUpdateContentsAsync(StorageFolder updateFolder, string expectedFileName)
        {
            foreach (StorageFile file in await updateFolder.GetFilesAsync().AsTask().ConfigureAwait(false))
            {
                string fileName = file.Name;
                if (fileName.Equals(expectedFileName))
                {
                    return fileName;
                }
            }

            foreach (StorageFolder folder in await updateFolder.GetFoldersAsync().AsTask().ConfigureAwait(false))
            {
                string mainBundlePathInSubFolder = await FindJSBundleInUpdateContentsAsync(folder, expectedFileName).ConfigureAwait(false);
                if (mainBundlePathInSubFolder != null)
                {
                    return Path.Combine(folder.Name, mainBundlePathInSubFolder);
                }
            }

            return null;
        }
    }
}
