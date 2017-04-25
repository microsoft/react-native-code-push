using System;
using System.Text;
using System.Collections.Generic;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Newtonsoft.Json.Linq;
using CodePush.ReactNative;

namespace CodePush.Net46.Test
{
    /// <summary>
    /// Summary description for TelemetryManagerTest
    /// </summary>
    [TestClass]
    public class TelemetryManagerTest
    {
        public TelemetryManagerTest()
        {
            //
            // TODO: Add constructor logic here
            //
        }

        private TestContext testContextInstance;

        /// <summary>
        ///Gets or sets the test context which provides
        ///information about and functionality for the current test run.
        ///</summary>
        public TestContext TestContext
        {
            get
            {
                return testContextInstance;
            }
            set
            {
                testContextInstance = value;
            }
        }

        #region Additional test attributes
        //
        // You can use the following additional attributes as you write your tests:
        //
        // Use ClassInitialize to run code before running the first test in the class
        // [ClassInitialize()]
        // public static void MyClassInitialize(TestContext testContext) { }
        //
        // Use ClassCleanup to run code after all tests in a class have run
        // [ClassCleanup()]
        // public static void MyClassCleanup() { }
        //
        // Use TestInitialize to run code before running each test 
        // [TestInitialize()]
        // public void MyTestInitialize() { }
        //
        // Use TestCleanup to run code after each test has run
        // [TestCleanup()]
        // public void MyTestCleanup() { }
        //
        #endregion

        [TestMethod]
        public void TestGetUpdateReport()
        {
            var input = new JObject();
            input.Add("deploymentKey", "depKeyParam");
            input.Add("label", "labelParam");

            var output = TelemetryManager.getUpdateReport(input);
            Assert.IsNotNull(output);
        }

        [TestMethod]
        public void TestGetUpdateReportNegative()
        {
            var inputNoLabel = new JObject();
            inputNoLabel.Add("deploymentKey", "depKeyParam");
            Assert.IsNull(TelemetryManager.getUpdateReport(inputNoLabel));

            var inputNoKey = new JObject();
            inputNoKey.Add("label", "labelParam");
            Assert.IsNull(TelemetryManager.getUpdateReport(inputNoKey));
        }
    }
}
