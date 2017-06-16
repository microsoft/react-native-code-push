using CodePush.ReactNative;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace CodePush.Net46.Test
{
    /// <summary>
    /// Some tests for telemetry manager
    /// As implementation of TelemetryManager was ported from android version, we do not test logic here.
    /// Here are tests for some tricky parts of implementation, or check some data transformation that
    /// has no full equvalent in C#
    /// </summary>
    [TestClass]
    public class TelemetryManagerTest
    {
        #region Constants from TelemetryManager
        //private static readonly string APP_VERSION_KEY = "appVersion";
        private static readonly string DEPLOYMENT_FAILED_STATUS = "DeploymentFailed";
        private static readonly string DEPLOYMENT_KEY_KEY = "deploymentKey";
        private static readonly string DEPLOYMENT_SUCCEEDED_STATUS = "DeploymentSucceeded";
        private static readonly string LABEL_KEY = "label";
        private static readonly string LAST_DEPLOYMENT_REPORT_KEY = "CODE_PUSH_LAST_DEPLOYMENT_REPORT";
        //private static readonly string PACKAGE_KEY = "package";
        //private static readonly string PREVIOUS_DEPLOYMENT_KEY_KEY = "previousDeploymentKey";
        //private static readonly string PREVIOUS_LABEL_OR_APP_VERSION_KEY = "previousLabelOrAppVersion";
        private static readonly string RETRY_DEPLOYMENT_REPORT_KEY = "CODE_PUSH_RETRY_DEPLOYMENT_REPORT";
        private static readonly string STATUS_KEY = "status";
        #endregion

        [TestMethod]
        public void TestGetUpdateReportNoPreviousUpdate()
        {
            var input = new JObject();
            input.Add(DEPLOYMENT_KEY_KEY, "depKeyParam");
            input.Add(LABEL_KEY, "labelParam");

            var output = TelemetryManager.GetUpdateReport(input);
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

            var output = TelemetryManager.GetUpdateReport(input);
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
            Assert.IsNull(TelemetryManager.GetUpdateReport(inputNoLabel));

            var inputNoKey = new JObject();
            inputNoKey.Add(LABEL_KEY, "labelParam");
            Assert.IsNull(TelemetryManager.GetUpdateReport(inputNoKey));
        }

        [TestMethod]
        public void TestRecordStatusReportWithRollback()
        {
            var report = new JObject();
            report.Add(STATUS_KEY, DEPLOYMENT_FAILED_STATUS);

            TelemetryManager.RecordStatusReported(report);
            Assert.IsTrue(true);
        }

        [TestMethod]
        public void TestRecordStatusReportWithoutRollback()
        {
            var reportSuccess = new JObject();
            reportSuccess.Add(STATUS_KEY, DEPLOYMENT_SUCCEEDED_STATUS);
            TelemetryManager.RecordStatusReported(reportSuccess);

            var reportNoStatus = new JObject();
            TelemetryManager.RecordStatusReported(reportNoStatus);

            Assert.IsTrue(true);
        }

        [TestMethod]
        public void TestStatusReportForRetrySerialization()
        {
            SettingsManager.RemoveString(RETRY_DEPLOYMENT_REPORT_KEY);
            var original = new JObject();
            original.Add("keyString", "stringValue");
            original.Add("keyInt", 42);
            original.Add("keyBool", true);

            TelemetryManager.SaveStatusReportForRetry(original);

            var stringified = SettingsManager.GetString(RETRY_DEPLOYMENT_REPORT_KEY);
            SettingsManager.RemoveString(RETRY_DEPLOYMENT_REPORT_KEY);

            Assert.IsNotNull(stringified);
            var result = JObject.Parse(stringified);

            Assert.IsTrue((bool)result.GetValue("keyBool"));
            Assert.AreEqual(42, (int)result.GetValue("keyInt"));
            Assert.AreEqual("stringValue", (string)result.GetValue("keyString"));
        }
    }
}
