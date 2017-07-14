using CodePush.Net46.Adapters.Http;
using Newtonsoft.Json.Linq;
using PCLStorage;
using System;
using System.IO;
using System.Net;
using System.Threading.Tasks;

namespace CodePush.ReactNative
{
    internal class UpdateUtils
    {
        internal async static Task CopyNecessaryFilesFromCurrentPackageAsync(IFile diffManifestFile, IFolder currentPackageFolder, IFolder newPackageFolder)
        {
            await FileUtils.MergeFoldersAsync(currentPackageFolder, newPackageFolder).ConfigureAwait(false);
            JObject diffManifest = await CodePushUtils.GetJObjectFromFileAsync(diffManifestFile).ConfigureAwait(false);
            var deletedFiles = (JArray)diffManifest["deletedFiles"];
            foreach (string fileNameToDelete in deletedFiles)
            {
                var fileToDelete = await newPackageFolder.GetFileAsync(fileNameToDelete).ConfigureAwait(false);
                await fileToDelete.DeleteAsync().ConfigureAwait(false);
            }
        }

        internal async static Task<string> FindJSBundleInUpdateContentsAsync(IFolder updateFolder, string expectedFileName)
        {
            foreach (IFile file in await updateFolder.GetFilesAsync().ConfigureAwait(false))
            {
                string fileName = file.Name;
                if (fileName.Equals(expectedFileName))
                {
                    return fileName;
                }
            }

            foreach (IFolder folder in await updateFolder.GetFoldersAsync().ConfigureAwait(false))
            {
                string mainBundlePathInSubFolder = await FindJSBundleInUpdateContentsAsync(folder, expectedFileName).ConfigureAwait(false);
                if (mainBundlePathInSubFolder != null)
                {
                    return Path.Combine(folder.Name, mainBundlePathInSubFolder);
                }
            }

            return null;
        }

        internal async static Task DownloadBundleAsync(string url, string fileName, IProgress<HttpProgress> downloadProgress)
        {
            var uri = new Uri(url);
            var client = new WebClient();
            client.DownloadProgressChanged += (s, e) =>
            {
                downloadProgress.Report(new HttpProgress
                {
                    BytesReceived = (ulong)e.BytesReceived,            //conversion long to ulong is safe
                    TotalBytesToReceive = (ulong)e.TotalBytesToReceive //because size can't be negative
                });
            };

            await client.DownloadFileTaskAsync(uri, fileName);
        }

        internal static async Task<IFolder> GetCodePushFolderAsync()
        {
            var pathToCodePush = Path.Combine(CodePushUtils.GetFileBundlePrefix(), CodePushConstants.CodePushFolderPrefix);
            return await FileSystem.Current.LocalStorage.CreateFolderAsync(pathToCodePush, CreationCollisionOption.OpenIfExists).ConfigureAwait(false);
        }
    }
}
