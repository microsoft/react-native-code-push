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
            await FileUtils.MergeFoldersAsync(currentPackageFolder, newPackageFolder);
            JObject diffManifest = await CodePushUtils.GetJObjectFromFileAsync(diffManifestFile);
            var deletedFiles = (JArray)diffManifest["deletedFiles"];
            foreach (string fileNameToDelete in deletedFiles)
            {
                StorageFile fileToDelete = await newPackageFolder.GetFileAsync(fileNameToDelete);
                await fileToDelete.DeleteAsync();
            }
        }

        internal async static Task<string> FindJSBundleInUpdateContentsAsync(StorageFolder updateFolder, string expectedFileName)
        {
            foreach (StorageFile file in await updateFolder.GetFilesAsync())
            {
                string fileName = file.Name;
                if (fileName.Equals(expectedFileName))
                {
                    return fileName;
                }
            }

            foreach (StorageFolder folder in await updateFolder.GetFoldersAsync())
            {
                string mainBundlePathInSubFolder = await FindJSBundleInUpdateContentsAsync(folder, expectedFileName);
                if (mainBundlePathInSubFolder != null)
                {
                    return Path.Combine(folder.Name, mainBundlePathInSubFolder);
                }
            }

            return null;
        }
    }
}
