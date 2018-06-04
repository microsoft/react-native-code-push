### Objective-C API Reference (iOS)

The Objective-C API is made available by importing the `CodePush.h` header into your `AppDelegate.m` file, and consists of a single public class named `CodePush`.

#### CodePush

Contains static methods for retreiving the `NSURL` that represents the most recent JavaScript bundle file, and can be passed to the `RCTRootView`'s `initWithBundleURL` method when bootstrapping your app in the `AppDelegate.m` file.

The `CodePush` class' methods can be thought of as composite resolvers which always load the appropriate bundle, in order to accommodate the following scenarios:

1. When an end-user installs your app from the store (e.g. `1.0.0`), they will get the JS bundle that is contained within the binary. This is the behavior you would get without using CodePush, but we make sure it doesn't break :)

2. As soon as you begin releasing CodePush updates, your end-users will get the JS bundle that represents the latest release for the configured deployment. This is the behavior that allows you to iterate beyond what you shipped to the store.

3. As soon as you release an update to the app store (e.g. `1.1.0`), and your end-users update it, they will once again get the JS bundle that is contained within the binary. This behavior ensures that CodePush updates that targetted a previous app store version aren't used (since we don't know if they would work), and your end-users always have a working version of your app.

4. Repeat #2 and #3 as the CodePush releases and app store releases continue on into infinity (and beyond?)

Because of this behavior, you can safely deploy updates to both the app store(s) and CodePush as necesary, and rest assured that your end-users will always get the most recent version.

##### Methods

- __(NSURL \*)bundleURL__ - Returns the most recent JS bundle `NSURL` as described above. This method assumes that the name of the JS bundle contained within your app binary is `main.jsbundle`.

- __(NSURL \*)bundleURLForResource:(NSString \*)resourceName__ - Equivalent to the `bundleURL` method, but also allows customizing the name of the JS bundle that is looked for within the app binary. This is useful if you aren't naming this file `main` (which is the default convention). This method assumes that the JS bundle's extension is `*.jsbundle`.

- __(NSURL \*)bundleURLForResource:(NSString \*)resourceName withExtension:(NSString \*)resourceExtension__: Equivalent to the `bundleURLForResource:` method, but also allows customizing the extension used by the JS bundle that is looked for within the app binary. This is useful if you aren't naming this file `*.jsbundle` (which is the default convention).

- __(void)overrideAppVersion:(NSString \*)appVersionOverride__ - Sets the version of the application's binary interface, which would otherwise default to the App Store version specified as the `CFBundleShortVersionString` in the `Info.plist`. This should be called a single time, before the bundle URL is loaded.

- __(void)setDeploymentKey:(NSString \*)deploymentKey__ - Sets the deployment key that the app should use when querying for updates. This is a dynamic alternative to setting the deployment key in your `Info.plist` and/or specifying a deployment key in JS when calling `checkForUpdate` or `sync`.
