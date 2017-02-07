using PCLStorage;
using System;
using System.IO;
using System.Threading.Tasks;

namespace CodePush.ReactNative
{
    internal class FileUtils
    {
        internal async static Task MergeFoldersAsync(IFolder source, IFolder target)
        {
            foreach (IFile sourceFile in await source.GetFilesAsync().ConfigureAwait(false))
            {
                await CopyFileAsync(sourceFile.Path, Path.Combine(target.Path, sourceFile.Name)).ConfigureAwait(false);
            }

            foreach (IFolder sourceDirectory in await source.GetFoldersAsync().ConfigureAwait(false))
            {
                var nextTargetSubDir = await target.CreateFolderAsync(sourceDirectory.Name, CreationCollisionOption.OpenIfExists).ConfigureAwait(false);
                await MergeFoldersAsync(sourceDirectory, nextTargetSubDir).ConfigureAwait(false);
            }
        }

        internal async static Task ClearReactDevBundleCacheAsync()
        {

            if (await FileSystem.Current.LocalStorage.CheckExistsAsync(CodePushConstants.ReactDevBundleCacheFileName).ConfigureAwait(false) != ExistenceCheckResult.FileExists)
                return;

            var devBundleCacheFile = await FileSystem.Current.LocalStorage.GetFileAsync(CodePushConstants.ReactDevBundleCacheFileName).ConfigureAwait(false);
            await devBundleCacheFile.DeleteAsync().ConfigureAwait(false);
        }

        internal static Task<long> GetBinaryResourcesModifiedTimeAsync(string fileName)
        {
            var pathToAssembly = CodePushUtils.GetAppFolder();
            var pathToAssemblyResource = Path.Combine(pathToAssembly, CodePushConstants.AssetsBundlePrefix, fileName);
            var lastUpdateTime = File.GetCreationTime(pathToAssemblyResource);

            return Task.FromResult(new DateTimeOffset(lastUpdateTime).ToUnixTimeMilliseconds());
        }

        internal async static Task CopyFileAsync(string sourcePath, string destinationPath)
        {
            using (var source = File.Open(sourcePath, FileMode.Open, System.IO.FileAccess.Read))
            {
                using (var destination = File.Create(destinationPath)) // Replace if exists
                {
                    await source.CopyToAsync(destination);
                }
            }
        }

    }
}
