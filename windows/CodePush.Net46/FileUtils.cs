using System;
using System.Threading.Tasks;
using PCLStorage;
using System.IO;
using System.Reflection;

namespace CodePush.ReactNative
{
    internal class FileUtils
    {
        internal async static Task MergeFoldersAsync(IFolder source, IFolder target)
        {
            foreach (IFile sourceFile in await source.GetFilesAsync().ConfigureAwait(false))
            {
                await CopyFileAsync(sourceFile.Path, target.Path).ConfigureAwait(false);
            }
            
            foreach (IFolder sourceDirectory in await source.GetFoldersAsync().ConfigureAwait(false))
            {
                var nextTargetSubDir = await target.CreateFolderAsync(sourceDirectory.Name, CreationCollisionOption.OpenIfExists).ConfigureAwait(false);
                await MergeFoldersAsync(sourceDirectory, nextTargetSubDir).ConfigureAwait(false);
            }
        }
        internal async static Task ClearReactDevBundleCacheAsync()
        {
            var devBundleCacheFile = await FileSystem.Current.LocalStorage.GetFileAsync(CodePushConstants.ReactDevBundleCacheFileName).ConfigureAwait(false);
            
            if (devBundleCacheFile != null)
            {
                await devBundleCacheFile.DeleteAsync().ConfigureAwait(false);
            }
        }

        internal static Task<long> GetBinaryResourcesModifiedTimeAsync(string fileName)
        {
            var assembly = Assembly.GetAssembly(typeof(UpdateManager));
            var assemblyName = assembly.GetName();
            var pathToAssembly = Path.GetDirectoryName(assemblyName.CodeBase);
            var pathToAssemblyResource = Path.Combine(pathToAssembly, CodePushConstants.AssetsBundlePrefix.Replace("ms-appx:///", String.Empty), fileName);
            var u = new Uri(pathToAssemblyResource);
            var lastUpdateTime = File.GetCreationTime(u.LocalPath);

            return Task.FromResult(new DateTimeOffset(lastUpdateTime).ToUnixTimeMilliseconds());
        }
    
        internal async static Task CopyFileAsync(string sourcePath, string destinationPath)
        {
            using (Stream source = File.Open(sourcePath, FileMode.Open, System.IO.FileAccess.Read))
            {
                using (Stream destination = File.Create(destinationPath)) // Replace if exists
                {
                    await source.CopyToAsync(destination); 
                }
            }
        }

    }
}
