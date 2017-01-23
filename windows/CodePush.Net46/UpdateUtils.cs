using Newtonsoft.Json.Linq;
using System.IO;
using System.Threading.Tasks;
using PCLStorage;
using System;
using CodePush.Net46.Adapters.Http;
using System.Net;


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
                    BytesReceived = (UInt64)e.BytesReceived,           //conversion ulong to long is safe
                    TotalBytesToReceive = (UInt64)e.TotalBytesToReceive //because size can't be negative
                });
            };

            await client.DownloadFileTaskAsync(uri, fileName);
        }

        //internal async static Task UnzipBundleAsync(string zipFileName, string targetDir)
        //{
        //    await Task.Run( () =>
        //    {
        //        using (var zip = new ZipFile(zipFileName))
        //        {
        //            foreach (var entry in zip)
        //            {
        //                entry.Extract(targetDir);
        //            }
        //        }

        //        return Task.CompletedTask;
        //    });
        //}
    }
}
