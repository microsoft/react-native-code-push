using System;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using CodePush.Net46.Adapters.Storage;
using System.Threading.Tasks;

namespace CodePush.Net46.Test
{
    [TestClass]
    public class ApplicationDataContainerTest
    {
        readonly static string key1 = "key1";
        readonly static string key2 = "key2";
        readonly static string key3 = "key3";

        readonly static string val1 = "string data1";
        readonly static string val2 = "string data2";
        readonly static string val3 = "string data1";

        [TestMethod]
        public void TestInMemmorySet()
        {
            var settings = new ApplicationDataContainer();

            settings.Values[key1] = val1;
            settings.Values[key2] = val2;
            settings.Values[key3] = val3;

            Assert.AreEqual(settings.Values[key1], val1);
            Assert.AreEqual(settings.Values[key2], val2);
            Assert.AreEqual(settings.Values[key3], val3);

            settings.DeleteAsync().Wait();
        }

        [TestMethod]
        public void TestInMemmoryReset()
        {
            var settings = new ApplicationDataContainer();

            settings.Values[key1] = val1;
            settings.Values[key1] = val2;
            settings.Values[key1] = val3;

            Assert.AreEqual(settings.Values[key1], val3);

            settings.DeleteAsync().Wait();
        }

        [TestMethod]
        public void TestInMemmoryRemove()
        {
            var settings = new ApplicationDataContainer();

            settings.Values[key1] = val1;
            settings.Values[key2] = val2;
            settings.Values[key3] = val3;

            settings.Values.Remove(key2);

            Assert.AreEqual(settings.Values[key1], val1);
            Assert.IsNull(settings.Values[key2]);
            Assert.AreEqual(settings.Values[key3], val3);

            settings.DeleteAsync().Wait();
        }

        [TestMethod]
        public void TestPersistentSet()
        {
            var settings = new ApplicationDataContainer();

            settings.Values[key1] = val1;
            settings.Values[key2] = val2;
            settings.Values[key3] = val3;

            settings = new ApplicationDataContainer();

            Assert.AreEqual(settings.Values[key1], val1);
            Assert.AreEqual(settings.Values[key2], val2);
            Assert.AreEqual(settings.Values[key3], val3);

            settings.DeleteAsync().Wait();
        }

        [TestMethod]
        public void TestPersistentRemove()
        {
            var settings = new ApplicationDataContainer();

            settings.Values[key1] = val1;
            settings.Values[key2] = val2;
            settings.Values[key3] = val3;

            settings.Values.Remove(key2);

            settings = new ApplicationDataContainer();

            Assert.AreEqual(settings.Values[key1], val1);
            Assert.IsNull(settings.Values[key2]);
            Assert.AreEqual(settings.Values[key3], val3);

            settings.DeleteAsync().Wait();
        }
    }
}
