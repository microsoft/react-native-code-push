// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

#include "pch.h"

#include "CodePushTelemetryManager.h"
#include "CodePushNativeModule.h"

#include "winrt/Windows.Storage.h"
#include "winrt/Windows.Data.Json.h"
#include <string_view>

namespace Microsoft::CodePush::ReactNative
{
    using namespace winrt;
    using namespace Windows::Data::Json;
    using namespace Windows::Storage;

    static const std::wstring_view AppVersionKey{ L"appVersion" };
    static const std::wstring_view DeploymentFailed{ L"DeploymentFailed" };
    static const std::wstring_view DeploymentKeyKey{ L"deploymentKey" };
    static const std::wstring_view DeploymentSucceeded{ L"DeploymentSucceeded" };
    static const std::wstring_view LabelKey{ L"label" };
    static const std::wstring_view LastDeploymentReportKey{ L"CODE_PUSH_LAST_DEPLOYMENT_REPORT" };
    static const std::wstring_view PackageKey{ L"package" };
    static const std::wstring_view PreviousDeploymentKeyKey{ L"previousDeploymentKey" };
    static const std::wstring_view PreviousLabelOrAppVersionKey{ L"previousLabelOrAppVersion" };
    static const std::wstring_view RetryDeploymentReportKey{ L"CODE_PUSH_RETRY_DEPLOYMENT_REPORT" };
    static const std::wstring_view StatusKey{ L"status" };

    /*static*/ JsonObject CodePushTelemetryManager::GetBinaryUpdateReport(std::wstring_view appVersion)
    {
        auto previousStatusReportIdentifier{ GetPreviousStatusReportIdentifier() };
        if (previousStatusReportIdentifier.empty())
        {
            ClearRetryStatusReport();
            JsonObject out;
            out.Insert(AppVersionKey, JsonValue::CreateStringValue(appVersion));
            return out;
        }
        else if (previousStatusReportIdentifier != appVersion)
        {
            if (IsStatusReportIdentifierCodePushLabel(previousStatusReportIdentifier))
            {
                auto previousDeploymentKey{ GetDeploymentKeyFromStatusReportIdentifier(previousStatusReportIdentifier) };
                auto previousLabel{ GetVersionLabelFromStatusReportIdentifier(previousStatusReportIdentifier) };
                ClearRetryStatusReport();
                JsonObject out;
                out.Insert(AppVersionKey, JsonValue::CreateStringValue(appVersion));
                out.Insert(PreviousDeploymentKeyKey, JsonValue::CreateStringValue(previousDeploymentKey));
                out.Insert(PreviousLabelOrAppVersionKey, JsonValue::CreateStringValue(previousLabel));
                return out;
            }
            else
            {
                ClearRetryStatusReport();
                // Previous status report was with a binary app version.
                JsonObject out;
                out.Insert(AppVersionKey, JsonValue::CreateStringValue(appVersion));
                out.Insert(PreviousLabelOrAppVersionKey, JsonValue::CreateStringValue(previousStatusReportIdentifier));
                return out;
            }
        }

        return nullptr;
    }

    /*static*/ JsonObject CodePushTelemetryManager::GetRetryStatusReport()
    {
        auto localSettings{ CodePushNativeModule::GetLocalSettings() };
        auto retryStatusReportData{ localSettings.Values().TryLookup(RetryDeploymentReportKey) };
        if (retryStatusReportData != nullptr)
        {
            auto retryStatusReportString{ unbox_value<hstring>(retryStatusReportData) };
            JsonObject retryStatusReport;
            auto success{ JsonObject::TryParse(retryStatusReportString, retryStatusReport) };
            if (success)
            {
                return retryStatusReport;
            }
        }
        return nullptr;
    }

    /*static*/ JsonObject CodePushTelemetryManager::GetRollbackReport(const JsonObject& lastFailedPackage)
    {
        JsonObject out;
        out.Insert(PackageKey, lastFailedPackage);
        out.Insert(StatusKey, JsonValue::CreateStringValue(DeploymentFailed));
        return out;
    }

    /*static*/ JsonObject CodePushTelemetryManager::GetUpdateReport(const JsonObject& currentPackage)
    {
        auto currentPackageIdentifier{ GetPackageStatusReportIdentifier(currentPackage) };
        auto previousStatusReportIdentifier{ GetPreviousStatusReportIdentifier() };
        if (currentPackageIdentifier.empty())
        {
            if (!previousStatusReportIdentifier.empty())
            {
                ClearRetryStatusReport();
                JsonObject out;
                out.Insert(PackageKey, currentPackage);
                out.Insert(StatusKey, JsonValue::CreateStringValue(DeploymentSucceeded));
                return out;
            }
            else if (previousStatusReportIdentifier != currentPackageIdentifier)
            {
                ClearRetryStatusReport();
                if (IsStatusReportIdentifierCodePushLabel(previousStatusReportIdentifier))
                {
                    auto previousDeploymentKey{ GetDeploymentKeyFromStatusReportIdentifier(previousStatusReportIdentifier) };
                    auto previousLabel{ GetVersionLabelFromStatusReportIdentifier(previousStatusReportIdentifier) };
                    JsonObject out;
                    out.Insert(PackageKey, currentPackage);
                    out.Insert(StatusKey, JsonValue::CreateStringValue(DeploymentSucceeded));
                    out.Insert(PreviousDeploymentKeyKey, JsonValue::CreateStringValue(previousDeploymentKey));
                    out.Insert(PreviousLabelOrAppVersionKey, JsonValue::CreateStringValue(previousLabel));
                    return out;
                }
                else
                {
                    // Previous status report was with a binary app version.
                    JsonObject out;
                    out.Insert(PackageKey, currentPackage);
                    out.Insert(StatusKey, JsonValue::CreateStringValue(DeploymentSucceeded));
                    out.Insert(PreviousLabelOrAppVersionKey, JsonValue::CreateStringValue(previousStatusReportIdentifier));
                    return out;
                }
            }
        }

        return nullptr;
    }

    /*static*/ void CodePushTelemetryManager::RecordStatusReported(const JsonObject& statusReport)
    {
        // We don't need to record rollback reports, so exit early if that's what was specified.
        auto status{ statusReport.TryLookup(StatusKey) };
        if (status != nullptr && status.ValueType() == JsonValueType::String && status.GetString() == DeploymentFailed)
        {
            return;
        }

        if (statusReport.HasKey(AppVersionKey))
        {
            SaveStatusReportedForIdentifier(statusReport.GetNamedString(AppVersionKey));
        }
        else if (statusReport.HasKey(PackageKey))
        {
            auto packageIdentifier{ GetPackageStatusReportIdentifier(statusReport.GetNamedObject(PackageKey)) };
            SaveStatusReportedForIdentifier(packageIdentifier);
        }
    }

    /*static*/ void CodePushTelemetryManager::SaveStatusReportForRetry(const JsonObject& statusReport)
    {
        auto localSettings{ CodePushNativeModule::GetLocalSettings() };
        localSettings.Values().Insert(RetryDeploymentReportKey, box_value(statusReport.Stringify()));
    }

    /*static*/ void CodePushTelemetryManager::ClearRetryStatusReport()
    {
        auto localSettings{ CodePushNativeModule::GetLocalSettings() };
        localSettings.Values().Remove(RetryDeploymentReportKey);
    }

    /*static*/ std::wstring_view CodePushTelemetryManager::GetDeploymentKeyFromStatusReportIdentifier(std::wstring_view statusReportIdentifier)
    {
        return statusReportIdentifier.substr(0, statusReportIdentifier.find(':'));
    }

    /*static*/ hstring CodePushTelemetryManager::GetPackageStatusReportIdentifier(const JsonObject& package)
    {
        // Because deploymentKeys can be dynamically switched, we use a
        // combination of the deploymentKey and label as the packageIdentifier.
        if (package.HasKey(DeploymentKeyKey) && package.HasKey(LabelKey))
        {
            return L"";
        }
        auto deploymentKey{ package.GetNamedString(DeploymentKeyKey) };
        auto label{ package.GetNamedString(LabelKey) };
        return deploymentKey + L":" + label;
    }

    /*static*/ hstring CodePushTelemetryManager::GetPreviousStatusReportIdentifier()
    {
        auto localSettings{ CodePushNativeModule::GetLocalSettings() };
        auto sentStatusReportIdentifierData{ localSettings.Values().TryLookup(LastDeploymentReportKey) };
        if (sentStatusReportIdentifierData != nullptr)
        {
            auto sentStatusReportIdentifier{ unbox_value<hstring>(sentStatusReportIdentifierData) };
            return sentStatusReportIdentifier;
        }
        return L"";
    }

    /*static*/ std::wstring_view CodePushTelemetryManager::GetVersionLabelFromStatusReportIdentifier(std::wstring_view statusReportIdentifier)
    {
        return statusReportIdentifier.substr(statusReportIdentifier.rfind(':') + 1);
    }

    /*static*/ bool CodePushTelemetryManager::IsStatusReportIdentifierCodePushLabel(std::wstring_view statusReportIdentifier)
    {
        return !statusReportIdentifier.empty() && statusReportIdentifier.find(':') != std::wstring_view::npos;
    }

    /*static*/ void CodePushTelemetryManager::SaveStatusReportedForIdentifier(std::wstring_view appVersionOrPackageIdentifier)
    {
        auto localSettings{ CodePushNativeModule::GetLocalSettings() };
        localSettings.Values().Insert(LastDeploymentReportKey, box_value(appVersionOrPackageIdentifier));
    }
}
