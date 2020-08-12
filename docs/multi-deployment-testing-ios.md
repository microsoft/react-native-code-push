### iOS

> NOTE
>
> Complete demos configured with "multi-deployment testing" feature are [here]:
> *  **without using cocoa pods**: [link](https://github.com/microsoft/react-native-code-push/files/1259957/rncp976.copy.zip)
> *  **using cocoa pods**: [link](https://github.com/microsoft/react-native-code-push/files/1172217/rncp893.copy.zip)

Xcode allows you to define custom build settings for each "configuration" (like debug, release), which can then be referenced as the value of keys within the `Info.plist` file (like the `CodePushDeploymentKey` setting). This mechanism allows you to easily configure your builds to produce binaries, which are configured to synchronize with different CodePush deployments.

To set this up, perform the following steps:

1. Open up your Xcode project and select your project in the `Project navigator` window

2. Ensure the project node is selected, as opposed to one of your targets

3. Select the `Info` tab

4. Click the `+` button within the `Configurations` section and select `Duplicate "Release" Configuration`

   ![Configuration](https://cloud.githubusercontent.com/assets/116461/16101597/088714c0-331c-11e6-9504-5469d9a59d74.png)

5. Name the new configuration `Staging` (or whatever you prefer)

6. Select the `Build Settings` tab

7. Click the `+` button on the toolbar and select `Add User-Defined Setting`

   ![Setting](https://cloud.githubusercontent.com/assets/116461/15764165/a16dbe30-28dd-11e6-94f2-fa3b7eb0c7de.png)

   Name this new setting something like `Multi_Deployment_Config`. Go to the setting and add value `$(BUILD_DIR)/$(CONFIGURATION)$(EFFECTIVE_PLATFORM_NAME)` for Release. After that add value `$(BUILD_DIR)/Release$(EFFECTIVE_PLATFORM_NAME)` for Staging

   ![MultiDeploymentConfig](https://user-images.githubusercontent.com/48414875/87178636-1d6a6500-c2e6-11ea-890d-b7773f07e503.png)

   *NOTE: For Xcode 10 and lower version: Go to Build Location -> Per-configuration Build Products Path -> Staging and change Staging value from $(BUILD_DIR)/$(CONFIGURATION)$(EFFECTIVE_PLATFORM_NAME) to $(BUILD_DIR)/Release$(EFFECTIVE_PLATFORM_NAME)*

   ![BuildFilesPath1](https://cloud.githubusercontent.com/assets/4928157/22645377/b1d7df0e-ec77-11e6-83c6-291a27bcdb17.png)

   *NOTE: Due to https://github.com/facebook/react-native/issues/11813, we have to do this step to make it possible to use other configurations than Debug or Release on RN 0.40.0 or higher.*

8. Click the `+` button again on the toolbar and select `Add User-Defined Setting`

   Name this new setting something like `CODEPUSH_KEY`, expand it, and specify your `Staging` deployment key for the `Staging` config and your `Production` deployment key for the `Release` config.

    ![Setting Keys](https://cloud.githubusercontent.com/assets/8598682/16821919/fc1eac4a-490d-11e6-9b11-128129c24b80.png)

    *NOTE: As a reminder, you can retrieve these keys by running `appcenter codepush deployment list -a <ownerName>/<appName> -k` from your terminal.*

9. Open your project's `Info.plist` file and change the value of your `CodePushDeploymentKey` entry to `$(CODEPUSH_KEY)`

    ![Infoplist](https://cloud.githubusercontent.com/assets/116461/15764252/3ac8aed2-28de-11e6-8c19-2270ae9857a7.png)

And that's it! Now when you run or build your app, your staging builds will automatically be configured to sync with your `Staging` deployment, and your release builds will be configured to sync with your `Production` deployment.

*NOTE: CocoaPods users may need to run `pod install` before building with their new release configuration.*

*Note: If you encounter the error message `ld: library not found for ...`, please consult [this issue](https://github.com/microsoft/react-native-code-push/issues/426) for a possible solution.*

Additionally, if you want to give them seperate names and/or icons, you can modify the `Product Bundle Identifier`, `Product Name` and `Asset Catalog App Icon Set Name` build settings, which will allow your staging builds to be distinguishable from release builds when installed on the same device.
