// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

#pragma once

#include "winrt/base.h"
#include "winrt/Windows.Storage.h"
#include <exception>

namespace Microsoft::CodePush::ReactNative
{
	struct CodePushUtils
	{
		static void Log(winrt::hstring message);
		static void Log(const winrt::hresult_error& ex);
		static void LogBundleUrl(const winrt::Windows::Storage::IStorageFile& bundle);
	};
}
