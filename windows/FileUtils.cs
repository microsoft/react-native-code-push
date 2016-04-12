using Newtonsoft.Json.Linq;
using System;
using System.IO;
using System.IO.Compression;
using System.Threading.Tasks;
using Windows.Storage;
using Windows.Storage.Streams;

namespace ReactNative.CodePush
{
    class FileUtils
    {
        public async static Task MergeDirectories(StorageFolder source, StorageFolder target)
        {
            foreach (StorageFile sourceFile in await source.GetFilesAsync())
            {
                await sourceFile.CopyAndReplaceAsync(await target.CreateFileAsync(sourceFile.Name, CreationCollisionOption.OpenIfExists));
            }
            
            foreach (StorageFolder sourceDirectory in await source.GetFoldersAsync())
            {
                StorageFolder nextTargetSubDir = await target.CreateFolderAsync(sourceDirectory.Name, CreationCollisionOption.OpenIfExists);
                await MergeDirectories(sourceDirectory, nextTargetSubDir);
            }
        }
    }
}
