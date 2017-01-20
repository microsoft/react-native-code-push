using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace CodePush.Net46.Adapters.Storage
{
    /// <summary>
    /// Mimics Windows.Storage.ApplicationData class for NET framework
    /// </summary>
    public static class ApplicationData
    {
        public static string ApplicationName { get; set; }

        private static string localFolder;
        public static string LocalFolder
        {
            get
            {
                if (String.IsNullOrEmpty(localFolder))
                {
                    localFolder = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                        ApplicationName);
                }

                return localFolder;
            }
        }

        public static async Task<StorageFolder> CreateFolderAsync(string path)
        {
            return await Task.Run(() =>
             {
                 Directory.CreateDirectory(path);
                 return new StorageFolder(path);
             });
        }
    }
}
