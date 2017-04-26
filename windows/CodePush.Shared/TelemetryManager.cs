using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.Runtime.CompilerServices;
using System.Text;
#if !WINDOWS_UWP
using System.Diagnostics;
#endif
[assembly: InternalsVisibleTo("CodePush.Net46.Test")]

namespace CodePush.ReactNative
{

#if WINDOWS_UWP
    internal class Trace
    {
        public static void WriteLine(string message, string category)
        {
        }
    }
#endif

    internal class TelemetryManager
    {
        #region Constants
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

        #region Internal methods

        internal static JObject getBinaryUpdateReport(string appVersion)
        {
            Trace.WriteLine($"called getBinaryUpdateReport({appVersion})", "[TelemetryManager]");

            var previousStatusReportIdentifier = getPreviousStatusReportIdentifier();

            if (previousStatusReportIdentifier != null)
            {
                clearRetryStatusReport();

                var report = new JObject();
                report.Add(APP_VERSION_KEY, appVersion);
                Trace.WriteLine($"returned getBinaryUpdateReport1: {report.ToString(Formatting.None)}", "[TelemetryManager]");
                return report;
            }

            if (!previousStatusReportIdentifier.Equals(appVersion))
            {
                clearRetryStatusReport();

                var report = new JObject();
                report.Add(APP_VERSION_KEY, appVersion);
                if (isStatusReportIdentifierCodePushLabel(previousStatusReportIdentifier))
                {
                    var previousDeploymentKey = getDeploymentKeyFromStatusReportIdentifier(previousStatusReportIdentifier);
                    var previousLabel = getVersionLabelFromStatusReportIdentifier(previousStatusReportIdentifier);

                    report.Add(PREVIOUS_DEPLOYMENT_KEY_KEY, previousDeploymentKey);
                    report.Add(PREVIOUS_LABEL_OR_APP_VERSION_KEY, previousLabel);
                }
                else
                {
                    // Previous status report was with a binary app version.
                    report.Add(PREVIOUS_LABEL_OR_APP_VERSION_KEY, previousStatusReportIdentifier);
                }
                Trace.WriteLine($"returned getBinaryUpdateReport2: {report.ToString(Formatting.None)}", "[TelemetryManager]");
                return report;
            }

            return null;
        }
  
        internal static JObject getRetryStatusReport()
        {
            Trace.WriteLine($"called getRetryStatusReport()", "[TelemetryManager]");
            var retryStatusReportString = SettingsManager.GetString(RETRY_DEPLOYMENT_REPORT_KEY);

            if (retryStatusReportString != null)
            {
                clearRetryStatusReport();
                try
                {
                    var report = JObject.Parse(retryStatusReportString);
                    Trace.WriteLine($"returned getRetryStatusReport: {report.ToString(Formatting.None)}", "[TelemetryManager]");
                    return report;
                }
                catch (Exception e)
                {
                    //TODO: should be reported error
                    Trace.WriteLine(e.ToString(), "[CodePush.Telemetry]");
                }
            }

            return null;
        }

        internal JObject getRollbackReportReport(JObject lastFailedPackage)
        {
            // TODO: Implement me!
            Trace.WriteLine($"called getRollbackReportReport({lastFailedPackage.ToString(Formatting.None)})", "[TelemetryManager]");

            var report = new JObject();
            return report;
        }

        internal static JObject getUpdateReport(JObject currentPackage)
        {
            Trace.WriteLine($"called getUpdateReport({currentPackage.ToString(Formatting.None)})", "[TelemetryManager]");

            var currentPackageIdentifier = getPackageStatusReportIdentifier(currentPackage);
            if (currentPackageIdentifier == null)
            {
                return null;
            }

            var previousStatusReportIdentifier = getPreviousStatusReportIdentifier();
            if (previousStatusReportIdentifier == null)
            {
                clearRetryStatusReport();
                var report = new JObject();
                report.Add(PACKAGE_KEY, currentPackage);
                report.Add(STATUS_KEY, DEPLOYMENT_SUCCEEDED_STATUS);
                Trace.WriteLine($"returned getUpdateReport1: {report.ToString(Formatting.None)}", "[TelemetryManager]");
                return report;
            }

            if (!previousStatusReportIdentifier.Equals(currentPackageIdentifier))
            {
                clearRetryStatusReport();
                var report = new JObject();
                report.Add(PACKAGE_KEY, currentPackage);
                report.Add(STATUS_KEY, DEPLOYMENT_SUCCEEDED_STATUS);

                if (isStatusReportIdentifierCodePushLabel(previousStatusReportIdentifier))
                {
                    var previousDeploymentKey = getDeploymentKeyFromStatusReportIdentifier(previousStatusReportIdentifier);
                    var previousLabel = getVersionLabelFromStatusReportIdentifier(previousStatusReportIdentifier);

                    report.Add(PREVIOUS_DEPLOYMENT_KEY_KEY, previousDeploymentKey);
                    report.Add(PREVIOUS_LABEL_OR_APP_VERSION_KEY, previousLabel);
                }
                else
                {
                    // Previous status report was with a binary app version.
                    report.Add(PREVIOUS_LABEL_OR_APP_VERSION_KEY, previousStatusReportIdentifier);
                }

                Trace.WriteLine($"returned getUpdateReport2: {report.ToString(Formatting.None)}", "[TelemetryManager]");
                return report;
            }

            return null;
        }

        internal void recordStatusReported(JObject statusReport)
        {
            // TODO: Implement me!
            Trace.WriteLine($"called recordStatusReported({statusReport.ToString(Formatting.None)})", "[TelemetryManager]");
        }

        internal void saveStatusReportForRetry(JObject statusReport)
        {
            // TODO: Implement me!
            Trace.WriteLine($"called saveStatusReportForRetry({statusReport.ToString(Formatting.None)})", "[TelemetryManager]");
        }

        #endregion

        #region Private methods
        static string getPackageStatusReportIdentifier(JObject updatePackage)
        {
            // Because deploymentKeys can be dynamically switched, we use a
            // combination of the deploymentKey and label as the packageIdentifier.
            try
            {
                var deploymentKey = (string)updatePackage[DEPLOYMENT_KEY_KEY];
                var label = (string)updatePackage[LABEL_KEY];
                if (string.IsNullOrEmpty(deploymentKey) || string.IsNullOrEmpty(label))
                {
                    return null;
                }

                return $"{deploymentKey}:{label}";
            }
            catch
            {
                return null;
            }
        }

        static string getPreviousStatusReportIdentifier()
        {
            return SettingsManager.GetString(LAST_DEPLOYMENT_REPORT_KEY);
        }

        static private void clearRetryStatusReport()
        {
            SettingsManager.RemoveString(RETRY_DEPLOYMENT_REPORT_KEY);
        }

        static bool isStatusReportIdentifierCodePushLabel(string statusReportIdentifier)
        {
            return (!string.IsNullOrEmpty(statusReportIdentifier)) && statusReportIdentifier.Contains(":");
        }

        static string getDeploymentKeyFromStatusReportIdentifier(string statusReportIdentifier)
        {
            string[] parsedIdentifier = statusReportIdentifier.Split(':');
            if (parsedIdentifier.Length > 0)
            {
                return parsedIdentifier[0];
            }
            else
            {
                return null;
            }
        }

        static string getVersionLabelFromStatusReportIdentifier(string statusReportIdentifier)
        {
            string[] parsedIdentifier = statusReportIdentifier.Split(':');
            if (parsedIdentifier.Length > 1)
            {
                return parsedIdentifier[1];
            }
            else
            {
                return null;
            }
        }
        #endregion
    }
}
