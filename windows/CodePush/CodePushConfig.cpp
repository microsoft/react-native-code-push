// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

#include "pch.h"
#include "CodePushConfig.h"
#include "CodePushConfig.g.cpp"
#include "CodePushNativeModule.h"

#include "winrt/Windows.Foundation.Collections.h"
#include "winrt/Windows.Storage.h"

namespace winrt::Microsoft::CodePush::ReactNative::implementation
{
    using namespace Windows::Storage;
    using namespace Windows::Data::Json;
    using namespace Windows::Foundation::Collections;

    CodePushConfig CodePushConfig::s_currentConfig{};

    /*static*/ CodePushConfig& CodePushConfig::Current() noexcept
    {
        return s_currentConfig;
    }

    JsonObject CodePushConfig::GetConfiguration()
    {
        JsonObject configObject;
        for (const auto& pair : m_configuration)
        {
            configObject.Insert(pair.Key(), JsonValue::CreateStringValue(pair.Value()));
        }
        return configObject;
    }

    /*static*/ void CodePushConfig::Init(IMap<hstring, hstring> const& configMap) noexcept
    {
        std::optional<hstring> appVersion;
        std::optional<hstring> buildVersion;
        std::optional<hstring> deploymentKey;
        std::optional<hstring> publicKey;
        std::optional<hstring> serverUrl;

        if (configMap != nullptr)
        {
            appVersion = configMap.TryLookup(AppVersionConfigKey);
            buildVersion = configMap.TryLookup(BuildVersionConfigKey);
            deploymentKey = configMap.TryLookup(DeploymentKeyConfigKey);
            publicKey = configMap.TryLookup(PublicKeyKey);
            serverUrl = configMap.TryLookup(ServerURLConfigKey);
        }

        s_currentConfig.m_configuration = winrt::single_threaded_map<hstring, hstring>();
        auto addToConfiguration = [=](std::wstring_view key, std::optional<hstring> optValue) {
            if (optValue.has_value())
            {
                s_currentConfig.m_configuration.Insert(key, optValue.value());
            }
        };

        auto localSettings{ ::Microsoft::CodePush::ReactNative::CodePushNativeModule::GetLocalSettings() };
        hstring clientUniqueId;
        auto clientUniqueIdData{ localSettings.Values().TryLookup(ClientUniqueIDConfigKey) };
        if (clientUniqueIdData == nullptr)
        {
            auto newGuid{ GuidHelper::CreateNewGuid() };
            clientUniqueId = to_hstring(newGuid);
            localSettings.Values().Insert(ClientUniqueIDConfigKey, box_value(clientUniqueId));
        }
        else
        {
            clientUniqueId = unbox_value<hstring>(clientUniqueIdData);
        }

        addToConfiguration(AppVersionConfigKey, appVersion);
        addToConfiguration(BuildVersionConfigKey, buildVersion);
        addToConfiguration(DeploymentKeyConfigKey, deploymentKey);
        addToConfiguration(PublicKeyKey, publicKey);
        addToConfiguration(ServerURLConfigKey, serverUrl);

        s_currentConfig.m_configuration.Insert(ClientUniqueIDConfigKey, clientUniqueId);

        if (!serverUrl.has_value())
        {
            s_currentConfig.m_configuration.Insert(ServerURLConfigKey, L"https://codepush.appcenter.ms/");
        }

        ::Microsoft::CodePush::ReactNative::CodePushNativeModule::LoadBundle();
    }

    hstring CodePushConfig::QueryConfig(std::wstring_view key)
    {
        auto value{ m_configuration.TryLookup(key) };
        if (value.has_value())
        {
            return value.value();
        }
        return L"";
    }

    /*static*/ void CodePushConfig::SetHost(Microsoft::ReactNative::ReactNativeHost const& host) noexcept
    {
        ::Microsoft::CodePush::ReactNative::CodePushNativeModule::SetHost(host);
    }
}
