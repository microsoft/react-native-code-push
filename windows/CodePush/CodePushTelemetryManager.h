// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

#pragma once

#include "winrt/Windows.Data.Json.h"
#include <string_view>

namespace Microsoft::CodePush::ReactNative
{
	struct CodePushTelemetryManager
	{
		static winrt::Windows::Data::Json::JsonObject GetBinaryUpdateReport(std::wstring_view appVersion);
		static winrt::Windows::Data::Json::JsonObject GetRetryStatusReport();
		static winrt::Windows::Data::Json::JsonObject GetRollbackReport(const winrt::Windows::Data::Json::JsonObject& lastFailedPackage);
		static winrt::Windows::Data::Json::JsonObject GetUpdateReport(const winrt::Windows::Data::Json::JsonObject& currentPackage);
		static void RecordStatusReported(const winrt::Windows::Data::Json::JsonObject& statusReport);
		static void SaveStatusReportForRetry(const winrt::Windows::Data::Json::JsonObject& statusReport);

	private:
		static void ClearRetryStatusReport();
		static std::wstring_view GetDeploymentKeyFromStatusReportIdentifier(std::wstring_view statusReportIdentifier);
		static winrt::hstring GetPackageStatusReportIdentifier(const winrt::Windows::Data::Json::JsonObject& package);
		static winrt::hstring GetPreviousStatusReportIdentifier();
		static std::wstring_view GetVersionLabelFromStatusReportIdentifier(std::wstring_view statusReportIdentifier);
		static bool IsStatusReportIdentifierCodePushLabel(std::wstring_view statusReportIdentifier);
		static void SaveStatusReportedForIdentifier(std::wstring_view appVersionOrPackageIdentifier);
	};
}
