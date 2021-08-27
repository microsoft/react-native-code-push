## Windows Setup

Once you've acquired the CodePush plugin, you need to integrate it into the Visual Studio project of your React Native app and configure it correctly. To do this, take the following steps:

### Plugin Installation and Configuration for React Native Windows 0.63.6 version and above

#### Plugin Installation (Windows-npx)

Once the plugin has been downloaded, run `npx react-native autolink-windows` in your application's root directory to automatically add the CodePush c++ project to your application's windows solution file.

#### Plugin Configuration (Windows)

1. Replace the following files located at `windows/<app name>` with those in the CodePushDemoAppCpp example app in this repo found at `Examples/CodePushDemoAppCpp/windows/CodePushDemoAppCpp`:
   1. app.h
   2. app.cpp
   3. app.xaml

2. In the above files, replace any occurance of `CodePushDemoAppCpp` with the name of your application

3. Enter your application's app version and deployment key to the `configMap` object at the top of your app's `OnLaunched` method in `App.cpp`:

```c++
...
void App::OnLaunched(activation::LaunchActivatedEventArgs const& e)
{
    winrt::Microsoft::CodePush::ReactNative::CodePushConfig::SetHost(Host());
    auto configMap{ winrt::single_threaded_map<hstring, hstring>() };
    configMap.Insert(L"appVersion", L"1.0.0");
    configMap.Insert(L"deploymentKey", L"<app deployment key>");
    winrt::Microsoft::CodePush::ReactNative::CodePushConfig::Init(configMap);
...
}
...
```

#### Plugin Configuration (Windows) C#

1. add name space `Microsoft.CodePush` to `App.xaml.cs`

2. add app version and deployment key to `configMap` at the start of your app's `OnLaunched` method in `App.xaml.cs`.

```c#
using Microsoft.CodePush;

...
protected override void OnLaunched(LaunchActivatedEventArgs e)
{
    Microsoft.CodePush.ReactNative.CodePushConfig.SetHost(Host);
    IDictionary<string, string> configMap = new Dictionary<string, string>();
    configMap.Add("appVersion", "1.0.0");
    configMap.Add("deploymentKey", "deployment key");
    Microsoft.CodePush.ReactNative.CodePushConfig.Init(configMap);
...
}
...
```


### Plugin Installation and Configuration for React Native Windows lower than 0.60

#### Plugin Installation (Windows)

1. Open the Visual Studio solution located at `windows-legacy\<AppName>\<AppName>.sln` within your app

2. Right-click the solution node in the `Solution Explorer` window and select the `Add -> Existing Project...` menu item

   ![Add Project](https://cloud.githubusercontent.com/assets/116461/14467164/ddf6312e-008e-11e6-8a10-44a8b44b5dfc.PNG)

3. Browse to the `node_modules\react-native-code-push\windows` directory, select the `CodePush.csproj` file and click `OK`

4. Back in the `Solution Explorer`, right-click the project node that is named after your app, and select the `Add -> Reference...` menu item

   ![Add Reference](https://cloud.githubusercontent.com/assets/116461/14467154/d833bc98-008e-11e6-8e95-09864b1f05ef.PNG)

5. Select the `Projects` tab on the left hand side, check the `CodePush` item and then click `OK`

   ![Add Reference Dialog](https://cloud.githubusercontent.com/assets/116461/14467147/cb805b6e-008e-11e6-964f-f856c59b65af.PNG)

#### Plugin Configuration (Windows)

After installing the plugin, you need to configure your app to consult CodePush for the location of your JS bundle, since it will "take control" of managing the current and all future versions. To do this, update the `MainReactNativeHost.cs` file to use CodePush via the following changes:

```c#
...
// 1. Import the CodePush namespace
using CodePush.ReactNative;
...
class MainReactNativeHost : ReactNativeHost
{
    // 2. Declare a private instance variable for the CodePushModule instance.
    private CodePushReactPackage codePushReactPackage;

    // 3. Update the JavaScriptBundleFile property to initalize the CodePush runtime,
    // specifying the right deployment key, then use it to return the bundle URL from
    // CodePush instead of statically from the binary. If you don't already have your
    // deployment key, you can run "appcenter codepush deployment list -a <ownerName>/<appName> -k" to retrieve it.
    protected override string JavaScriptBundleFile
    {
        get
        {
            codePushReactPackage = new CodePushReactPackage("deployment-key-here", this);
            return codePushReactPackage.GetJavaScriptBundleFile();
        }
    }

    // 4. Add the codePushReactPackage instance to the list of existing packages.
    protected override List<IReactPackage> Packages
    {
        get
        {
            return new List<IReactPackage>
            {
                new MainReactPackage(),
                ...
                codePushReactPackage
            };
        }
    }
    ...
}
```
