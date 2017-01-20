using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace CodePush.Net46.Adapters.Storage
{
    /// <summary>
    /// Mimics Windows.Storage.StorageFile clise for NET framework
    /// </summary>
    public class StorageFile
    {
        public string Path { get; private set; }
        public StorageFile(string name)
        {
            Path = name;
        }
    }
}
