#pragma once
#include "winrt/Microsoft.CodePush.ReactNative.h"

#include "ReactPackageProvider.g.h"

using namespace winrt::Microsoft::ReactNative;

namespace winrt::Microsoft::CodePush::ReactNative::implementation {

struct ReactPackageProvider : ReactPackageProviderT<ReactPackageProvider> {
  ReactPackageProvider() = default;

  void CreatePackage(IReactPackageBuilder const &packageBuilder) noexcept;
};

} // namespace winrt::RNFS::implementation

namespace winrt::Microsoft::CodePush::ReactNative::factory_implementation {

struct ReactPackageProvider : ReactPackageProviderT<ReactPackageProvider, implementation::ReactPackageProvider> {};

} // namespace winrt::RNFS::factory_implementation
