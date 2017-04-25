using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Runtime.CompilerServices;
using System.Text;

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
        //private static readonly string APP_VERSION_KEY = "appVersion";
        //private static readonly string DEPLOYMENT_FAILED_STATUS = "DeploymentFailed";
        private static readonly string DEPLOYMENT_KEY_KEY = "deploymentKey";
        //private static readonly string DEPLOYMENT_SUCCEEDED_STATUS = "DeploymentSucceeded";
        private static readonly string LABEL_KEY = "label";
        //private static readonly string LAST_DEPLOYMENT_REPORT_KEY = "CODE_PUSH_LAST_DEPLOYMENT_REPORT";
        //private static readonly string PACKAGE_KEY = "package";
        //private static readonly string PREVIOUS_DEPLOYMENT_KEY_KEY = "previousDeploymentKey";
        //private static readonly string PREVIOUS_LABEL_OR_APP_VERSION_KEY = "previousLabelOrAppVersion";
        //private static readonly string RETRY_DEPLOYMENT_REPORT_KEY = "CODE_PUSH_RETRY_DEPLOYMENT_REPORT";
        //private static readonly string STATUS_KEY = "status";
#endregion

#region Internal methods
        internal JObject getBinaryUpdateReport(string appVersion)
        {
            // TODO: Implement me!
            Trace.WriteLine($"called getBinaryUpdateReport({appVersion})", "[TelemetryManager]");

            var report = new JObject();
            return report;
        }

        internal JObject getRetryStatusReport()
        {
            // TODO: Implement me!
            Trace.WriteLine($"called getRetryStatusReport()", "[TelemetryManager]");

            var report = new JObject();
            return report;
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
            // TODO: Implement me!
            Trace.WriteLine($"called getUpdateReport({currentPackage.ToString(Formatting.None)})", "[TelemetryManager]");

            var currentPackageIdentifier = getPackageStatusReportIdentifier(currentPackage);
            if (currentPackageIdentifier == null)
            {
                return null;
            }
            

            var report = new JObject();
            Trace.WriteLine($"returned getUpdateReport: {report.ToString(Formatting.None)}", "[TelemetryManager]");
            return report;
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
#endregion
    }
}
