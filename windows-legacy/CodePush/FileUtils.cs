using System;
using System.Threading.Tasks;
using Windows.Storage;

namespace CodePush.ReactNative
{
    internal class FileUtils
    {
        internal async static Task MergeFoldersAsync(StorageFolder source, StorageFolder target)
        {
            foreach (StorageFile sourceFile in await source.GetFilesAsync().AsTask().ConfigureAwait(false))
            {
                await sourceFile.CopyAndReplaceAsync(await target.CreateFileAsync(sourceFile.Name, CreationCollisionOption.OpenIfExists).AsTask().ConfigureAwait(false)).AsTask().ConfigureAwait(false);
            }
            
            foreach (StorageFolder sourceDirectory in await source.GetFoldersAsync().AsTask().ConfigureAwait(false))
            {
                StorageFolder nextTargetSubDir = await target.CreateFolderAsync(sourceDirectory.Name, CreationCollisionOption.OpenIfExists).AsTask().ConfigureAwait(false);
                await MergeFoldersAsync(sourceDirectory, nextTargetSubDir).ConfigureAwait(false);
            }
        }

        internal async static Task ClearReactDevBundleCacheAsync()
        {
            var devBundleCacheFile = (StorageFile)await ApplicationData.Current.LocalFolder.TryGetItemAsync(CodePushConstants.ReactDevBundleCacheFileName).AsTask().ConfigureAwait(false);
            if (devBundleCacheFile != null)
            {
                await devBundleCacheFile.DeleteAsync().AsTask().ConfigureAwait(false);
            }
        }

        internal async static Task<long> GetBinaryResourcesModifiedTimeAsync(string fileName)
        {
            var assetJSBundleFile = await StorageFile.GetFileFromApplicationUriAsync(new Uri(CodePushConstants.AssetsBundlePrefix + fileName)).AsTask().ConfigureAwait(false);
            var fileProperties = await assetJSBundleFile.GetBasicPropertiesAsync().AsTask().ConfigureAwait(false);
            return fileProperties.DateModified.ToUnixTimeMilliseconds();
        }

    }
}
