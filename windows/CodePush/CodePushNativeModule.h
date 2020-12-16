// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

#pragma once

#include "NativeModules.h"
#include "winrt/Windows.Data.Json.h"
#include "winrt/Windows.Storage.h"

#include "CodePushConfig.h"

// Helper functions for reading and sending JsonValues to and from JavaScript
namespace winrt::Microsoft::ReactNative
{
	void ReadValue(IJSValueReader const& reader, /*out*/ Windows::Data::Json::JsonObject& value) noexcept;
	void ReadValue(IJSValueReader const& reader, /*out*/ Windows::Data::Json::IJsonValue& value) noexcept;
}

namespace Microsoft::CodePush::ReactNative
{
	REACT_MODULE(CodePushNativeModule, L"CodePush");
	struct CodePushNativeModule
	{
		enum class CodePushInstallMode
		{
			Immediate = 0,
			OnNextRestart = 1,
			OnNextResume = 2,
			OnNextSuspend = 3
		};

		enum class CodePushUpdateState
		{
			Running = 0,
			Pending = 1,
			Latest = 2
		};

		static winrt::Windows::Foundation::IAsyncAction LoadBundle();
		static void SetHost(const winrt::Microsoft::ReactNative::ReactNativeHost& host);

		static winrt::Windows::Foundation::IAsyncOperation<winrt::Windows::Storage::StorageFile> GetBinaryBundleAsync();
		static winrt::Windows::Foundation::IAsyncOperation<winrt::Windows::Storage::StorageFile> GetBundleFileAsync();

		static winrt::Windows::Foundation::IAsyncOperation<winrt::Windows::Storage::StorageFolder> GetBundleAssetsFolderAsync();
		static winrt::Windows::Storage::StorageFolder GetLocalStorageFolder();
		static winrt::Windows::Storage::ApplicationDataContainer GetLocalSettings();

		void OverrideAppVersion(std::wstring_view appVersion);
		void SetDeploymentKey(std::wstring_view deploymentKey);

		bool IsFailedHash(std::wstring_view packageHash);

		winrt::Windows::Data::Json::JsonObject GetRollbackInfo();
		int GetRollbackCountForPackage(
			std::wstring_view packageHash,
			const winrt::Windows::Data::Json::JsonObject& latestRollbackInfo);

		static bool IsPendingUpdate(std::wstring_view packageHash);

		winrt::Windows::Foundation::IAsyncAction ClearDebugUpdates();

		REACT_INIT(Initialize);
		void Initialize(winrt::Microsoft::ReactNative::ReactContext const& reactContext) noexcept;

		REACT_CONSTANT_PROVIDER(GetConstants);
		void GetConstants(winrt::Microsoft::ReactNative::ReactConstantProvider& constants) noexcept;

		/*
		 * This is native-side of the JavaScript RemotePackage.download method
		 */
		REACT_METHOD(DownloadUpdateAsync, L"downloadUpdate");
		winrt::fire_and_forget DownloadUpdateAsync(
			winrt::Windows::Data::Json::JsonObject updatePackage,
			bool notifyProgress,
			winrt::Microsoft::ReactNative::ReactPromise<winrt::Windows::Data::Json::IJsonValue> promise) noexcept;

		/*
		 * This is the native side of the CodePush.getConfiguration method. It isn't
		 * currently exposed via the "react-native-code-push" module, and is used
		 * internally only by the CodePush.checkForUpdate method in order to get the
		 * app version, as well as the deployment key that was configured in the Info.plist file.
		 */
		REACT_METHOD(GetConfiguration, L"getConfiguration");
		winrt::fire_and_forget GetConfiguration(winrt::Microsoft::ReactNative::ReactPromise<winrt::Windows::Data::Json::IJsonValue> promise) noexcept;

		/*
		 * This method is the native side of the CodePush.getUpdateMetadata method.
		 */
		REACT_METHOD(GetUpdateMetadataAsync, L"getUpdateMetadata");
		winrt::fire_and_forget GetUpdateMetadataAsync(
			CodePushUpdateState updateState,
			winrt::Microsoft::ReactNative::ReactPromise<winrt::Windows::Data::Json::IJsonValue> promise) noexcept;

		/*
		 * This method is the native side of the LocalPackage.install method.
		 */
		REACT_METHOD(InstallUpdateAsync, L"installUpdate");
		winrt::fire_and_forget InstallUpdateAsync(
			winrt::Windows::Data::Json::JsonObject updatePackage,
			CodePushInstallMode installMode,
			int minimumBackgroundDuration,
			winrt::Microsoft::ReactNative::ReactPromise<void> promise) noexcept;

		/*
		 * This method isn't publicly exposed via the "react-native-code-push"
		 * module, and is only used internally to populate the RemotePackage.failedInstall property.
		 */
		REACT_METHOD(IsFailedUpdate, L"isFailedUpdate");
		void IsFailedUpdate(
			std::wstring packageHash,
			winrt::Microsoft::ReactNative::ReactPromise<bool> promise) noexcept;

		REACT_METHOD(SetLatestRollbackInfo, L"setLatestRollbackInfo");
		void SetLatestRollbackInfo(std::wstring packageHash) noexcept;

		REACT_METHOD(GetLatestRollbackInfo, L"getLatestRollbackInfo");
		void GetLatestRollbackInfo(winrt::Microsoft::ReactNative::ReactPromise<winrt::Windows::Data::Json::IJsonValue> promise) noexcept;

		/*
		 * This method isn't publicly exposed via the "react-native-code-push"
		 * module, and is only used internally to populate the LocalPackage.isFirstRun property.
		 */
		REACT_METHOD(IsFirstRun, L"isFirstRun");
		winrt::fire_and_forget IsFirstRun(
			std::wstring packageHash,
			winrt::Microsoft::ReactNative::ReactPromise<bool> promise) noexcept;

		/*
		 * This method is the native side of the CodePush.notifyApplicationReady() method.
		 */
		REACT_METHOD(NotifyApplicationReady, L"notifyApplicationReady");
		void NotifyApplicationReady(winrt::Microsoft::ReactNative::ReactPromise<winrt::Windows::Data::Json::IJsonValue> promise) noexcept;

		REACT_METHOD(Allow, L"allow");
		void Allow(winrt::Microsoft::ReactNative::ReactPromise<winrt::Microsoft::ReactNative::JSValue> promise) noexcept;

		REACT_METHOD(ClearPendingRestart, L"clearPendingRestart");
		void ClearPendingRestart() noexcept;

		REACT_METHOD(Disallow, L"disallow");
		void Disallow(winrt::Microsoft::ReactNative::ReactPromise<winrt::Microsoft::ReactNative::JSValue> promise) noexcept;

		/*
		 * This method is the native side of the CodePush.restartApp() method.
		 */
		REACT_METHOD(RestartApp, L"restartApp");
		winrt::fire_and_forget RestartApp(
			bool onlyIfUpdateIsPending,
			winrt::Microsoft::ReactNative::ReactPromise<winrt::Microsoft::ReactNative::JSValue> promise) noexcept;

		/*
		 * This method clears CodePush's downloaded updates.
		 * It is needed to switch to a different deployment if the current deployment is more recent.
		 * Note: we don’t recommend to use this method in scenarios other than that (CodePush will call this method
		 * automatically when needed in other cases) as it could lead to unpredictable behavior.
		 */
		REACT_METHOD(ClearUpdates, L"clearUpdates");
		winrt::fire_and_forget ClearUpdates() noexcept;

		/*
		 * This method is the native side of the CodePush.downloadAndReplaceCurrentBundle()
		 * method, which replaces the current bundle with the one downloaded from
		 * removeBundleUrl. It is only to be used during tests and no-ops if the test
		 * configuration flag is not set.
		 */
		REACT_METHOD(DownloadAndReplaceCurrentBundle, L"downloadAndReplaceCurrentBundle");
		winrt::fire_and_forget DownloadAndReplaceCurrentBundle(std::wstring remoteBundleUrl) noexcept;

		/*
		 * This method is checks if a new status update exists (new version was installed,
		 * or an update failed) and return its details (version label, status).
		 */
		REACT_METHOD(GetNewStatusReportAsync, L"getNewStatusReport");
		winrt::fire_and_forget GetNewStatusReportAsync(winrt::Microsoft::ReactNative::ReactPromise<winrt::Windows::Data::Json::IJsonValue> promise) noexcept;

		REACT_METHOD(RecordStatusReported, L"recordStatusReported");
		void RecordStatusReported(winrt::Windows::Data::Json::JsonObject statusReport) noexcept;

		REACT_METHOD(SaveStatusReportForRetry, L"saveStatusReportForRetry");
		void SaveStatusReportForRetry(winrt::Windows::Data::Json::JsonObject statusReport) noexcept;

	private:
		bool m_isFirstRunAfterUpdate{ false };
		static CodePushInstallMode s_installMode;

		// Used to coordinate the dispatching of download progress events to JS.
		uint64_t m_latestExpectedContentLength{ 0 };
		uint64_t m_latestReceivedContentLength{ 0 };
		bool m_didUpdateProgress{ false };

		bool m_allowed{ true };
		bool m_restartInProgress{ false };
		std::vector<uint8_t> m_restartQueue;

		static constexpr std::wstring_view BundleExtension{ L".bundle" };
		static winrt::hstring s_javaScriptBundleFileName;

		// These constants represent emitted events
		static constexpr std::wstring_view DownloadProgressEvent{ L"CodePushDownloadProgress" };

		// These constants represent valid deployment statuses
		static constexpr std::wstring_view DeploymentFailed{ L"DeploymentFailed" };
		static constexpr std::wstring_view DeploymentSucceeded{ L"DeploymentSucceeded" };

		// These keys represent the names we use to store data in LocalSettings
		static constexpr std::wstring_view FailedUpdatesKey{ L"CODE_PUSH_FAILED_UPDATES" };
		static constexpr std::wstring_view PendingUpdateKey{ L"CODE_PUSH_PENDING_UPDATE" };

		// These keys are already "namespaced" by the PendingUpdateKey, so
		// their values don't need to be obfuscated to prevent collision with app data
		static constexpr std::wstring_view PendingUpdateHashKey{ L"hash" };
		static constexpr std::wstring_view PendingUpdateIsLoadingKey{ L"isLoading" };

		// These keys are used to inspect/augment the metadata
		// that is associated with an update's package.
		static constexpr std::wstring_view AppVersionKey{ L"appVersion" };
		static constexpr std::wstring_view BinaryBundleDateKey{ L"binaryDate" }; // The date of the BUILD -> the modified date of the executable
		static constexpr std::wstring_view PackageHashKey{ L"packageHash" };
		static constexpr std::wstring_view PackageIsPendingKey{ L"isPending" };

		static bool isRunningBinaryVersion;
		static bool needToReportRollback;

		static winrt::Microsoft::ReactNative::ReactNativeHost s_host;
		winrt::Microsoft::ReactNative::ReactContext m_context;

		// These keys represent the names we use to store information about the latest rollback
		static constexpr std::wstring_view LatestRollbackInfoKey{ L"LATEST_ROLLBACK_INFO" };
		static constexpr std::wstring_view LatestRollbackPackageHashKey{ L"packageHash" };
		static constexpr std::wstring_view LatestRollbackTimeKey{ L"time" };
		static constexpr std::wstring_view LatestRollbackCountKey{ L"count" };

		// Bool that keeps track of whether the app has been initialized at least once.
		static bool s_initialized;

		static winrt::Windows::Foundation::IAsyncAction ClearUpdatesStaticAsync();
		void DispatchDownloadProgressEvent();
		winrt::Windows::Foundation::IAsyncAction InitializeUpdateAfterRestart();
		winrt::Windows::Foundation::IAsyncAction RollbackPackage();
		static void RemoveFailedUpdates();
		static void RemovePendingUpdate();
		winrt::Windows::Foundation::IAsyncAction RestartAppInternal(bool onlyIfUpdateIsPending);
		void SaveFailedUpdate(winrt::Windows::Data::Json::JsonObject& failedPackage);
		void SavePendingUpdate(std::wstring_view packageHash, bool isLoading);
	};
}
