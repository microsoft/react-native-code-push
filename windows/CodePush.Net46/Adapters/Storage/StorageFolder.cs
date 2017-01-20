using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace CodePush.Net46.Adapters.Storage
{
    /// <summary>
    /// Mimics Windows.Storage.StorageFolder clise for NET framework
    /// </summary>
    public class StorageFolder
    {
        private string _path;
        public StorageFolder(string path)
        {
            _path = path;
        }
        
        public StorageFile CreateFile(string name)
        {
            return new StorageFile(Path.Combine(_path, name));
        }

        public StorageFile GetFile(string name)
        {
            var fullName = Path.Combine(_path, name);
            if (!File.Exists(fullName))
                throw new FileNotFoundException($"File not found: {fullName}");

            return new StorageFile(fullName);
        }

        public async Task<StorageFolder> GetFolderAsync(string path)
        {
            return await Task.Run(() =>
            {
                var folder = Path.Combine(_path, path);
                if (!Directory.Exists(folder))
                    throw new FileNotFoundException($"Direcotory not found: {folder}");

                return new Storage.StorageFolder(folder);
            });
        }

        public async Task<StorageFolder> CreateFolderAsync(string path)
        {
            var folder = Path.Combine(_path, path);
            return await ApplicationData.CreateFolderAsync(folder);
        }

        public async Task DeleteAsync()
        {
            await Task.Run(() => {
                Directory.Delete(_path, true);
            });
        }

    }
}
