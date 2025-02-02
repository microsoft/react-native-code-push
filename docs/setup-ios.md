## iOS Setup

Once you've acquired the CodePush plugin, you need to integrate it into the Xcode project of your React Native app and configure it correctly. To do this, take the following steps:
​
### Plugin Installation and Configuration for React Native 0.76 version and above (iOS)

1. Run `cd ios && pod install && cd ..` to install all the necessary CocoaPods dependencies.
​
2. Open up the `AppDelegate.m` file, and add an import statement for the CodePush headers:

   ```objective-c
   #import <CodePush/CodePush.h>
   ```

3. Find the following line of code, which sets the source URL for bridge for production releases:

   ```objective-c
   return [[NSBundle mainBundle] URLForResource:@"main" withExtension:@"jsbundle"];
   ```

4. Replace it with this line:
   
   ```objective-c
   return [CodePush bundleURL];
   ```
   This change configures your app to always load the most recent version of your app's JS bundle. On the first launch, this will correspond to the file that was compiled with the app. However, after an update has been pushed via CodePush, this will return the location of the most recently installed update.
  
   *NOTE: The `bundleURL` method assumes your app's JS bundle is named `main.jsbundle`. If you have configured your app to use a different file name, simply call the `bundleURLForResource:` method (which assumes you're using the `.jsbundle` extension) or `bundleURLForResource:withExtension:` method instead, in order to overwrite that default behavior*

   Typically, you're only going to want to use CodePush to resolve your JS bundle location within release builds, and therefore, we recommend using the `DEBUG` pre-processor macro to dynamically switch between using the packager server and CodePush, depending on whether you are debugging or not. This will make it much simpler to ensure you get the right behavior you want in production, while still being able to use the Chrome Dev Tools, live reload, etc. at debug-time.

   Your `sourceURLForBridge` method should look like this:

   ```objective-c
   - (NSURL *)sourceURLForBridge:(RCTBridge *)bridge
   {
     #if DEBUG
       return [[RCTBundleURLProvider sharedSettings] jsBundleURLForBundleRoot:@"index"];
     #else
       return [CodePush bundleURL];
     #endif
   }
   ```

5. Add the Deployment key to `Info.plist`:

   To let the CodePush runtime know which deployment it should query for updates against, open your app's `Info.plist` file and add a new entry named `CodePushDeploymentKey`, whose value is the key of the deployment you want to configure this app against (like the key for the `Staging` deployment for the `FooBar` app). You can retrieve this value by running `appcenter codepush deployment list -a <ownerName>/<appName> -k` in the AppCenter CLI (the `-k` flag is necessary since keys aren't displayed by default) and copying the value of the `Key` column which corresponds to the deployment you want to use (see below). Note that using the deployment's name (like Staging) will not work. That "friendly name" is intended only for authenticated management usage from the CLI, and not for public consumption within your app.

   ![Deployment list](https://cloud.githubusercontent.com/assets/116461/11601733/13011d5e-9a8a-11e5-9ce2-b100498ffb34.png)

   In order to effectively make use of the `Staging` and `Production` deployments that were created along with your CodePush app, refer to the [multi-deployment testing](../README.md#multi-deployment-testing) docs below before actually moving your app's usage of CodePush into production.

   *Note: If you need to dynamically use a different deployment, you can also override your deployment key in JS code using [Code-Push options](./api-js.md#CodePushOptions)*


### HTTP exception domains configuration (iOS)

CodePush plugin makes HTTPS requests to the following domains:

- codepush.appcenter.ms
- codepush.blob.core.windows.net
- codepushupdates.azureedge.net

If you want to change the default HTTP security configuration for any of these domains, you have to define the [`NSAppTransportSecurity` (ATS)][ats] configuration inside your __Info.plist__ file:

```xml
<plist version="1.0">
  <dict>
    <!-- ...other configs... -->

    <key>NSAppTransportSecurity</key>
    <dict>
      <key>NSExceptionDomains</key>
      <dict>
        <key>codepush.appcenter.ms</key>
        <dict><!-- read the ATS Apple Docs for available options --></dict>
      </dict>
    </dict>

    <!-- ...other configs... -->
  </dict>
</plist>
```

Before doing anything, please [read the docs][ats] first.

[ats]: https://developer.apple.com/library/content/documentation/General/Reference/InfoPlistKeyReference/Articles/CocoaKeys.html#//apple_ref/doc/uid/TP40009251-SW33

### Code Signing setup

Starting with CLI version **2.1.0** you can self sign bundles during release and verify its signature before installation of update. For more info about Code Signing please refer to [relevant code-push documentation section](https://github.com/microsoft/code-push/tree/v3.0.1/cli#code-signing).

In order to configure Public Key for bundle verification you need to add record in `Info.plist` with name `CodePushPublicKey` and string value of public key content. Example:

```xml
<plist version="1.0">
  <dict>
    <!-- ...other configs... -->

    <key>CodePushPublicKey</key>
        <string>-----BEGIN PUBLIC KEY-----
MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBANkWYydPuyOumR/sn2agNBVDnzyRpM16NAUpYPGxNgjSEp0etkDNgzzdzyvyl+OsAGBYF3jCxYOXozum+uV5hQECAwEAAQ==
-----END PUBLIC KEY-----</string>

    <!-- ...other configs... -->
  </dict>
</plist>
```

