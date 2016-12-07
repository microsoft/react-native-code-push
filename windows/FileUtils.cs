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
    }
}
