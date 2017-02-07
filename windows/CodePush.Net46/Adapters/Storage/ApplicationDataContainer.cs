using CodePush.ReactNative;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using PCLStorage;
using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace CodePush.Net46.Adapters.Storage
{
    public enum ApplicationDataCreateDisposition
    {
        Always = 0,
        Existing = 1
    }

    public class DictionaryWithDefault<TKey, TValue> : Dictionary<TKey, TValue>
    {
        TValue _default;
        public TValue DefaultValue
        {
            get { return _default; }
            set { _default = value; }
        }
        public DictionaryWithDefault() : base() { }
        public DictionaryWithDefault(TValue defaultValue) : base()
        {
            _default = defaultValue;
        }
        public new TValue this[TKey key]
        {
            get
            {
                TValue t;
                return base.TryGetValue(key, out t) ? t : _default;
            }
            set
            {
                base[key] = value;
                DataChanged(this, null);
            }
        }

        public bool Remove(TKey key)
        {
            var found = base.Remove(key);
            if (found)
                DataChanged(this, null);

            return found;
        }

        public event EventHandler DataChanged;

    }

    // A naive implementation of Windows.Storage.ApplicationDataContainer
    public class ApplicationDataContainer
    {
        public DictionaryWithDefault<string, string> Values;
        private readonly SemaphoreSlim mutex = new SemaphoreSlim(1, 1);

        const string STORAGE_NAME = "AppStorage.data";
        IFile storageFile = null;

        public ApplicationDataContainer(string name = STORAGE_NAME)
        {
            storageFile = FileSystem.Current.LocalStorage.CreateFileAsync(name, CreationCollisionOption.OpenIfExists).Result;
            var data = CodePushUtils.GetJObjectFromFileAsync(storageFile).Result;

            if (data != null)
            {
                Values = data.ToObject<DictionaryWithDefault<string, string>>();
            }
            else
            {
                Values = new DictionaryWithDefault<string, string>();
            }

            Values.DataChanged += async (s, e) => await SaveAsync();
        }

        ~ApplicationDataContainer()
        {
            mutex.Dispose();
        }

        async Task SaveAsync()
        {
            await mutex.WaitAsync().ConfigureAwait(false);
            var jobject = JObject.FromObject(Values);
            await storageFile.WriteAllTextAsync(JsonConvert.SerializeObject(jobject)).ConfigureAwait(false);
            mutex.Release();
        }

        public async Task DeleteAsync()
        {
            Values.Clear();
            await storageFile.DeleteAsync();
        }
    }
}
