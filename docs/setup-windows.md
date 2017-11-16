## Windows Setup

Once you've acquired the CodePush plugin, you need to integrate it into the Visual Studio project of your React Native app and configure it correctly. To do this, take the following steps:

### Plugin Installation (Windows)

1. Open the Visual Studio solution located at `windows\<AppName>\<AppName>.sln` within your app

2. Right-click the solution node in the `Solution Explorer` window and select the `Add -> Existing Project...` menu item

   ![Add Project](https://cloud.githubusercontent.com/assets/116461/14467164/ddf6312e-008e-11e6-8a10-44a8b44b5dfc.PNG)

3. Browse to the `node_modules\react-native-code-push\windows` directory, select the `CodePush.csproj` file and click `OK`

4. Back in the `Solution Explorer`, right-click the project node that is named after your app, and select the `Add -> Reference...` menu item

   ![Add Reference](https://cloud.githubusercontent.com/assets/116461/14467154/d833bc98-008e-11e6-8e95-09864b1f05ef.PNG)

5. Select the `Projects` tab on the left hand side, check the `CodePush` item and then click `OK`

   ![Add Reference Dialog](https://cloud.githubusercontent.com/assets/116461/14467147/cb805b6e-008e-11e6-964f-f856c59b65af.PNG)

### Plugin Configuration (Windows)

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
    // deployment key, you can run "code-push deployment ls <appName> -k" to retrieve it.
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
