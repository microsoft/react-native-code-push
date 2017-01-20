using System;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using CodePush.ReactNative;
using System.Threading.Tasks;
using CodePush.Net46.Adapters.Storage;

namespace CodePush.Net46.UnitTest
{
    [TestClass]
    public class UpdateManagerTest
    {
        const string AssetsBundleFileName = "index.windows.bundle";

        [TestInitialize()]
        public void Startup()
        {
            ApplicationData.ApplicationName = "Test\\UT";
        }

        [TestCleanup()]
        public void Cleanup()
        {
        }

        [TestMethod]
        public async Task GetCurrentPackageBundleAsyncTest()
        {
            UpdateManager manager = new UpdateManager();

            var packageFile = await manager.GetCurrentPackageBundleAsync(AssetsBundleFileName).ConfigureAwait(false);
            Assert.IsNotNull(packageFile);
        }

        [TestMethod]
        public async Task ClearUpdatesAsyncTest()
        {
            UpdateManager manager = new UpdateManager();

            await manager.ClearUpdatesAsync().ConfigureAwait(false);
            Assert.IsTrue(true);
        }
    }
}
