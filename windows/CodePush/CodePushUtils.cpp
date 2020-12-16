// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

#include "pch.h"
#include "CodePushUtils.h"

namespace Microsoft::CodePush::ReactNative
{
	using namespace winrt::Windows::Storage;

	/*static*/ void CodePushUtils::Log(winrt::hstring message)
	{
		OutputDebugStringW(L"[CodePush] ");
		OutputDebugStringW(message.c_str());
		OutputDebugStringW(L"\n");
	}

	/*static*/ void CodePushUtils::Log(const winrt::hresult_error& ex)
	{
		OutputDebugStringW(L"[CodePush] Exception ");
		OutputDebugStringW(ex.message().c_str());
		OutputDebugStringW(L"\n");
	}

	/*static*/ void CodePushUtils::LogBundleUrl(const IStorageFile& bundle)
	{
		CodePushUtils::Log(L"Loading JS bundle from \"" + bundle.Path() + L"\"");
	}
}
