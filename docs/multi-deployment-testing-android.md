### Android

> NOTE
>
> Complete demo configured with "multi-deployment testing" feature is [here](https://github.com/microsoft/react-native-code-push/files/1314118/rncp1004.zip).

The [Android Gradle plugin](https://google.github.io/android-gradle-dsl/current/index.html) allows you to define custom config settings for each "build type" (like debug, release). This mechanism allows you to easily configure your debug builds to use your CodePush staging deployment key and your release builds to use your CodePush production deployment key.

*NOTE: As a reminder, you can retrieve these keys by running `appcenter codepush deployment list -a <ownerName>/<appName> -k` from your terminal.*

To set this up, perform the following steps:

**For React Native >= v0.76**

1. Open the project's app level `build.gradle` file (for example `android/app/build.gradle` in standard React Native projects)

2. Find the `android { buildTypes {} }` section and define `resValue` entries for both your `debug` and `release` build types, which reference your `Staging` and `Production` deployment keys respectively.

    ```groovy
    android {
        ...
        buildTypes {
            debug {
                ...
                // Note: CodePush updates should not be tested in Debug mode as they are overriden by the RN packager. However, because CodePush checks for updates in all modes, we must supply a key.
                resValue "string", "CodePushDeploymentKey", '""'
                ...
            }

            releaseStaging {
                ...
                resValue "string", "CodePushDeploymentKey", '"<INSERT_STAGING_KEY>"'

                // Note: It is a good idea to provide matchingFallbacks for the new buildType you create to prevent build issues
                // Add the following line if not already there
                matchingFallbacks = ['release']
                ...
            }

            release {
                ...
                resValue "string", "CodePushDeploymentKey", '"<INSERT_PRODUCTION_KEY>"'
                ...
            }
        }
        ...
    }
    ```
    
    *NOTE: Remember to remove the key from `strings.xml` if you are configuring the deployment key in the build process*

    *NOTE: The naming convention for `releaseStaging` is significant due to [this line](https://github.com/facebook/react-native/blob/e083f9a139b3f8c5552528f8f8018529ef3193b9/react.gradle#L79).*


And that's it! View [here](http://tools.android.com/tech-docs/new-build-system/resource-merging) for more details on how resource merging works in Android.
