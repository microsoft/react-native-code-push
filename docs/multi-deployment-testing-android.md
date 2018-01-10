### Android

> NOTE
>
> Complete demo configured with "multi-deployment testing" feature is [here](https://github.com/Microsoft/react-native-code-push/files/1314118/rncp1004.zip).

The [Android Gradle plugin](http://google.github.io/android-gradle-dsl/current/index.html) allows you to define custom config settings for each "build type" (e.g. debug, release), which in turn are generated as properties on the `BuildConfig` class that you can reference from your Java code. This mechanism allows you to easily configure your debug builds to use your CodePush staging deployment key and your release builds to use your CodePush production deployment key.

To set this up, perform the following steps:

1. Open your app's `build.gradle` file (e.g. `android/app/build.gradle` in standard React Native projects)

2. Find the `android { buildTypes {} }` section and define `buildConfigField` entries for both your `debug` and `release` build types, which reference your `Staging` and `Production` deployment keys respectively. If you prefer, you can define the key literals in your `gradle.properties` file, and then reference them here. Either way will work, and it's just a matter of personal preference.

    ```groovy
    android {
        ...
        buildTypes {
            debug {
                ...
                // Note: CodePush updates should not be tested in Debug mode as they are overriden by the RN packager. However, because CodePush checks for updates in all modes, we must supply a key.
                buildConfigField "String", "CODEPUSH_KEY", '""'
                ...
            }

            releaseStaging {
                ...
                buildConfigField "String", "CODEPUSH_KEY", '"<INSERT_STAGING_KEY>"'
                ...
            }

            release {
                ...
                buildConfigField "String", "CODEPUSH_KEY", '"<INSERT_PRODUCTION_KEY>"'
                ...
            }
        }
        ...
    }
    ```

    *NOTE: As a reminder, you can retrieve these keys by running `code-push deployment ls <APP_NAME> -k` from your terminal.*

    *NOTE: The naming convention for `releaseStaging` is significant due to [this line](https://github.com/facebook/react-native/blob/e083f9a139b3f8c5552528f8f8018529ef3193b9/react.gradle#L79).*

4. Pass the deployment key to the `CodePush` constructor via the build config you just defined, as opposed to a string literal.

**For React Native >= v0.29**

Open up your `MainApplication.java` file and make the following changes:

 ```java
@Override
protected List<ReactPackage> getPackages() {
     return Arrays.<ReactPackage>asList(
         ...
         new CodePush(BuildConfig.CODEPUSH_KEY, MainApplication.this, BuildConfig.DEBUG), // Add/change this line.
         ...
     );
}
 ```

**For React Native v0.19 - v0.28**

Open up your `MainActivity.java` file and make the following changes:

 ```java
 @Override
 protected List<ReactPackage> getPackages() {
     return Arrays.<ReactPackage>asList(
         ...
         new CodePush(BuildConfig.CODEPUSH_KEY, this, BuildConfig.DEBUG), // Add/change this line.
         ...
     );
 }
 ```

*Note: If you gave your build setting a different name in your Gradle file, simply make sure to reflect that in your Java code.*

And that's it! Now when you run or build your app, your debug builds will automatically be configured to sync with your `Staging` deployment, and your release builds will be configured to sync with your `Production` deployment.

*NOTE: By default, the `react-native run-android` command builds and deploys the debug version of your app, so if you want to test out a release/production build, simply run `react-native run-android --variant release. Refer to the [React Native docs](http://facebook.github.io/react-native/docs/signed-apk-android.html#conten) for details about how to configure and create release builds for your Android apps.*

If you want to be able to install both debug and release builds simultaneously on the same device (highly recommended!), then you need to ensure that your debug build has a unique identity and icon from your release build. Otherwise, neither the OS nor you will be able to differentiate between the two. You can achieve this by performing the following steps:

1. In your `build.gradle` file, specify the [`applicationIdSuffix`](http://google.github.io/android-gradle-dsl/current/com.android.build.gradle.internal.dsl.BuildType.html#com.android.build.gradle.internal.dsl.BuildType:applicationIdSuffix) field for your debug build type, which gives your debug build a unique identity for the OS (e.g. `com.foo` vs. `com.foo.debug`).

```groovy
buildTypes {
    debug {
        applicationIdSuffix ".debug"
    }
}
```

2. Create the `app/src/debug/res` directory structure in your app, which allows overriding resources (e.g. strings, icons, layouts) for your debug builds

3. Create a `values` directory underneath the debug res directory created in #2, and copy the existing `strings.xml` file from the `app/src/main/res/values` directory

4. Open up the new debug `strings.xml` file and change the `<string name="app_name">` element's value to something else (e.g. `foo-debug`). This ensures that your debug build now has a distinct display name, so that you can differentiate it from your release build.

5. Optionally, create "mirrored" directories in the `app/src/debug/res` directory for all of your app's icons that you want to change for your debug build. This part isn't technically critical, but it can make it easier to quickly spot your debug builds on a device if its icon is noticeable different.

And that's it! View [here](http://tools.android.com/tech-docs/new-build-system/resource-merging) for more details on how resource merging works in Android.