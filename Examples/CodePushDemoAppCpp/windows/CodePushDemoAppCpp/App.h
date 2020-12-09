#pragma once

#include "App.xaml.g.h"
#include "winrt/Microsoft.ReactNative.h"

namespace activation = winrt::Windows::ApplicationModel::Activation;

namespace winrt::CodePushDemoAppCpp::implementation
{
    struct App : AppT<App>
    {
        App() noexcept;

        void OnLaunched(Windows::ApplicationModel::Activation::LaunchActivatedEventArgs const&);
        void OnSuspending(IInspectable const&, Windows::ApplicationModel::SuspendingEventArgs const&);
        void OnNavigationFailed(IInspectable const&, Windows::UI::Xaml::Navigation::NavigationFailedEventArgs const&);
    
        winrt::Microsoft::ReactNative::ReactNativeHost& Host() noexcept { return m_host; }
    private:
        winrt::Microsoft::ReactNative::ReactNativeHost m_host;
    };
} // namespace winrt::CodePushDemoAppCpp::implementation
