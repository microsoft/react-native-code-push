﻿using System;
using System.Text;
using System.Collections.Generic;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Newtonsoft.Json.Linq;
using CodePush.ReactNative;
using Newtonsoft.Json;

namespace CodePush.Net46.Test
{
    /// <summary>
    /// Summary description for TelemetryManagerTest
    /// </summary>
    [TestClass]
    public class TelemetryManagerTest
    {
        #region Constants from TelemetryManager
        private static readonly string APP_VERSION_KEY = "appVersion";
        private static readonly string DEPLOYMENT_FAILED_STATUS = "DeploymentFailed";
        private static readonly string DEPLOYMENT_KEY_KEY = "deploymentKey";
        private static readonly string DEPLOYMENT_SUCCEEDED_STATUS = "DeploymentSucceeded";
        private static readonly string LABEL_KEY = "label";
        private static readonly string LAST_DEPLOYMENT_REPORT_KEY = "CODE_PUSH_LAST_DEPLOYMENT_REPORT";
        private static readonly string PACKAGE_KEY = "package";
        private static readonly string PREVIOUS_DEPLOYMENT_KEY_KEY = "previousDeploymentKey";
        private static readonly string PREVIOUS_LABEL_OR_APP_VERSION_KEY = "previousLabelOrAppVersion";
        private static readonly string RETRY_DEPLOYMENT_REPORT_KEY = "CODE_PUSH_RETRY_DEPLOYMENT_REPORT";
        private static readonly string STATUS_KEY = "status";
        #endregion

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
        public void TestGetUpdateReportNoPreviousUpdate()
        {
            var input = new JObject();
            input.Add(DEPLOYMENT_KEY_KEY, "depKeyParam");
            input.Add(LABEL_KEY, "labelParam");

            var output = TelemetryManager.getUpdateReport(input);
            Assert.IsNotNull(output);
            Assert.IsTrue(output.ToString(Formatting.None).Contains("\"status\":\"DeploymentSucceeded\""));
        }

        [TestMethod]
        public void TestGetUpdateReportWithPreviousUpdate()
        {
            SettingsManager.SetString(LAST_DEPLOYMENT_REPORT_KEY, "prevKey:prevLabel");
            var input = new JObject();
            input.Add(DEPLOYMENT_KEY_KEY, "depKeyParam");
            input.Add(LABEL_KEY, "labelParam");

            var output = TelemetryManager.getUpdateReport(input);
            Assert.IsNotNull(output);
            Assert.IsTrue(output.ToString(Formatting.None).Contains("\"status\":\"DeploymentSucceeded\""));
            Assert.IsTrue(output.ToString(Formatting.None).Contains("\"previousDeploymentKey\":\"prevKey\",\"previousLabelOrAppVersion\":\"prevLabel\""));

            //Clean Up
            SettingsManager.RemoveString(LAST_DEPLOYMENT_REPORT_KEY);
        }

        [TestMethod]
        public void TestGetUpdateReportNegative()
        {
            var inputNoLabel = new JObject();
            inputNoLabel.Add(DEPLOYMENT_KEY_KEY, "depKeyParam");
            Assert.IsNull(TelemetryManager.getUpdateReport(inputNoLabel));

            var inputNoKey = new JObject();
            inputNoKey.Add(LABEL_KEY, "labelParam");
            Assert.IsNull(TelemetryManager.getUpdateReport(inputNoKey));
        }
    }
}
