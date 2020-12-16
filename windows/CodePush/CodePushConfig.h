// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

#pragma once
#include "CodePushConfig.g.h"

#include "NativeModules.h"

#include <string_view>
#include "winrt/Microsoft.ReactNative.h"
#include "winrt/Windows.Data.Json.h"
#include "winrt/Windows.Foundation.Collections.h"

namespace winrt::Microsoft::CodePush::ReactNative::implementation
{
    struct CodePushConfig : CodePushConfigT<CodePushConfig>
    {
        CodePushConfig() = default;

        static void Init(Windows::Foundation::Collections::IMap<hstring, hstring> const& configMap) noexcept;
        static void SetHost(Microsoft::ReactNative::ReactNativeHost const& host) noexcept;

        static CodePushConfig& Current() noexcept;

        hstring GetAppVersion() { return QueryConfig(AppVersionConfigKey); }
        void SetAppVersion(std::wstring_view appVersion) { m_configuration.Insert(AppVersionConfigKey, appVersion); }

        hstring GetBuildVersion() { return QueryConfig(BuildVersionConfigKey); }

        Windows::Data::Json::JsonObject GetConfiguration();

        hstring GetDeploymentKey() { return QueryConfig(DeploymentKeyConfigKey); }
        void SetDeploymentKey(std::wstring_view deploymentKey) { m_configuration.Insert(DeploymentKeyConfigKey, deploymentKey); }

        hstring GetServerUrl() { return QueryConfig(ServerURLConfigKey); }
        void SetServerUrl(std::wstring_view serverUrl) { m_configuration.Insert(ServerURLConfigKey, serverUrl); }

        hstring GetPublicKey() { return QueryConfig(PublicKeyKey); }
        void SetPublicKey(std::wstring_view publicKey) { m_configuration.Insert(PublicKeyKey, publicKey); }

    private:
        static constexpr std::wstring_view AppVersionConfigKey{ L"appVersion" };
        static constexpr std::wstring_view BuildVersionConfigKey{ L"buildVersion" };
        static constexpr std::wstring_view ClientUniqueIDConfigKey{ L"clientUniqueId" };
        static constexpr std::wstring_view DeploymentKeyConfigKey{ L"deploymentKey" };
        static constexpr std::wstring_view ServerURLConfigKey{ L"serverUrl" };
        static constexpr std::wstring_view PublicKeyKey{ L"publicKey" };

        Windows::Foundation::Collections::IMap<hstring, hstring> m_configuration;
        static CodePushConfig s_currentConfig;

        hstring QueryConfig(std::wstring_view key);
    };
}

namespace winrt::Microsoft::CodePush::ReactNative::factory_implementation
{
    struct CodePushConfig : CodePushConfigT<CodePushConfig, implementation::CodePushConfig>
    {
    };
}

namespace Microsoft::CodePush::ReactNative
{
    using winrt::Microsoft::CodePush::ReactNative::implementation::CodePushConfig;
}
