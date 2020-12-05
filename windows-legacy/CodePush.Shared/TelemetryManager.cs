using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using System.Runtime.CompilerServices;

[assembly: InternalsVisibleTo("CodePush.Net46.Test")]

namespace CodePush.ReactNative
{
    /// <summary>
    /// Implementation is ported from 
    /// android\app\src\main\java\com\microsoft\codepush\react\CodePushTelemetry.java
    /// I've tried to leave all logic, comments and structure without significant modification.
    /// </summary>

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

        internal static JObject GetBinaryUpdateReport(string appVersion)
        {
            var previousStatusReportIdentifier = GetPreviousStatusReportIdentifier();

            if (previousStatusReportIdentifier == null)
            {
                ClearRetryStatusReport();

                var report = new JObject();
                report.Add(APP_VERSION_KEY, appVersion);
                return report;
            }

            if (!previousStatusReportIdentifier.Equals(appVersion))
            {
                ClearRetryStatusReport();

                var report = new JObject();
                report.Add(APP_VERSION_KEY, appVersion);
                if (IsStatusReportIdentifierCodePushLabel(previousStatusReportIdentifier))
                {
                    var previousDeploymentKey = GetDeploymentKeyFromStatusReportIdentifier(previousStatusReportIdentifier);
                    var previousLabel = GetVersionLabelFromStatusReportIdentifier(previousStatusReportIdentifier);

                    report.Add(PREVIOUS_DEPLOYMENT_KEY_KEY, previousDeploymentKey);
                    report.Add(PREVIOUS_LABEL_OR_APP_VERSION_KEY, previousLabel);
                }
                else
                {
                    // Previous status report was with a binary app version.
                    report.Add(PREVIOUS_LABEL_OR_APP_VERSION_KEY, previousStatusReportIdentifier);
                }
                return report;
            }

            return null;
        }

        internal static JObject GetRetryStatusReport()
        {
            var retryStatusReportString = SettingsManager.GetString(RETRY_DEPLOYMENT_REPORT_KEY);

            if (retryStatusReportString != null)
            {
                ClearRetryStatusReport();
                try
                {
                    var report = JObject.Parse(retryStatusReportString);
                    return report;
                }
                catch (Exception)
                {
                    //TODO: should be reported error
                }
            }

            return null;
        }

        internal static JObject GetRollbackReport(JObject lastFailedPackage)
        {
            var report = new JObject();
            report.Add(STATUS_KEY, DEPLOYMENT_FAILED_STATUS);
            report.Add(PACKAGE_KEY, lastFailedPackage);

            return report;
        }

        internal static JObject GetUpdateReport(JObject currentPackage)
        {
            var currentPackageIdentifier = GetPackageStatusReportIdentifier(currentPackage);
            if (currentPackageIdentifier == null)
            {
                return null;
            }

            var previousStatusReportIdentifier = GetPreviousStatusReportIdentifier();
            if (previousStatusReportIdentifier == null)
            {
                ClearRetryStatusReport();
                var report = new JObject();
                report.Add(PACKAGE_KEY, currentPackage);
                report.Add(STATUS_KEY, DEPLOYMENT_SUCCEEDED_STATUS);
                return report;
            }

            if (!previousStatusReportIdentifier.Equals(currentPackageIdentifier))
            {
                ClearRetryStatusReport();
                var report = new JObject();
                report.Add(PACKAGE_KEY, currentPackage);
                report.Add(STATUS_KEY, DEPLOYMENT_SUCCEEDED_STATUS);

                if (IsStatusReportIdentifierCodePushLabel(previousStatusReportIdentifier))
                {
                    var previousDeploymentKey = GetDeploymentKeyFromStatusReportIdentifier(previousStatusReportIdentifier);
                    var previousLabel = GetVersionLabelFromStatusReportIdentifier(previousStatusReportIdentifier);

                    report.Add(PREVIOUS_DEPLOYMENT_KEY_KEY, previousDeploymentKey);
                    report.Add(PREVIOUS_LABEL_OR_APP_VERSION_KEY, previousLabel);
                }
                else
                {
                    // Previous status report was with a binary app version.
                    report.Add(PREVIOUS_LABEL_OR_APP_VERSION_KEY, previousStatusReportIdentifier);
                }

                return report;
            }

            return null;
        }

        internal static void RecordStatusReported(JObject statusReport)
        {
            // We don't need to record rollback reports, so exit early if that's what was specified.
            var status = (string)statusReport.GetValue(STATUS_KEY);
            if ((!string.IsNullOrEmpty(status)) && DEPLOYMENT_FAILED_STATUS.Equals(status))
            {
                return;
            }

            var appVersion = (string)statusReport.GetValue(APP_VERSION_KEY);
            if (!string.IsNullOrEmpty(appVersion))
            {
                SaveStatusReportedForIdentifier(appVersion);
            }
            else
            {
                var package = (JObject)statusReport.GetValue(PACKAGE_KEY);
                if (package == null)
                {
                    return;
                }

                var packageIdentifier = GetPackageStatusReportIdentifier(package);
                SaveStatusReportedForIdentifier(packageIdentifier);
            }
        }

        internal static void SaveStatusReportForRetry(JObject statusReport)
        {
            SettingsManager.SetString(RETRY_DEPLOYMENT_REPORT_KEY, statusReport.ToString(Formatting.None));
        }

        #endregion

        #region Private methods
        static string GetPackageStatusReportIdentifier(JObject updatePackage)
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

        static string GetPreviousStatusReportIdentifier()
        {
            return SettingsManager.GetString(LAST_DEPLOYMENT_REPORT_KEY);
        }

        static private void ClearRetryStatusReport()
        {
            SettingsManager.RemoveString(RETRY_DEPLOYMENT_REPORT_KEY);
        }

        static bool IsStatusReportIdentifierCodePushLabel(string statusReportIdentifier)
        {
            return (!string.IsNullOrEmpty(statusReportIdentifier)) && statusReportIdentifier.Contains(":");
        }

        static string GetDeploymentKeyFromStatusReportIdentifier(string statusReportIdentifier)
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

        static string GetVersionLabelFromStatusReportIdentifier(string statusReportIdentifier)
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

        static void SaveStatusReportedForIdentifier(string appVersionOrPackageIdentifier)
        {
            SettingsManager.SetString(LAST_DEPLOYMENT_REPORT_KEY, appVersionOrPackageIdentifier);
        }
        #endregion
    }
}
