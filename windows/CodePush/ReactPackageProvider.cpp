#include "pch.h"
#include "ReactPackageProvider.h"
#include "ReactPackageProvider.g.cpp"

#include "CodePushNativeModule.h"

using namespace winrt::Microsoft::ReactNative;

namespace winrt::Microsoft::CodePush::ReactNative::implementation {

void ReactPackageProvider::CreatePackage(IReactPackageBuilder const &packageBuilder) noexcept {
  AddAttributedModules(packageBuilder);
}

} // namespace winrt::Microsoft::CodePush::ReactNative::implementation
