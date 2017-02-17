## Android Setup

In order to integrate CodePush into your Android project, perform the following steps:

### Plugin Installation (Android)

In order to accommodate as many developer preferences as possible, the CodePush plugin supports Android installation via two mechanisms:

1. [**RNPM**](#plugin-installation-android---rnpm) - [React Native Package Manager (RNPM)](https://github.com/rnpm/rnpm) is an awesome tool that provides the simplest installation experience possible for React Native plugins. If you're already using it, or you want to use it, then we recommend this approach.

2. [**"Manual"**](#plugin-installation-android---manual) - If you don't want to depend on any additional tools or are fine with a few extra installation steps (it's a one-time thing), then go with this approach.

*Note: Due to a code change from the React Native repository, if your installed React Native version ranges from 0.29 to 0.32, we recommend following the manual steps to set up correctly. *

#### Plugin Installation (Android - RNPM)

1. As of v0.27 of React Native, `rnpm link` has already been merged into the React Native CLI. Simply run:
    ```
    react-native link react-native-code-push
    ```

    If your app uses a version of React Native that is lower than v0.27, run the following:
    ```
    rnpm link react-native-code-push
    ```

    *Note: If you don't already have RNPM installed, you can do so by simply running `npm i -g rnpm` and then executing the above command.*

2. If you're using RNPM >=1.6.0, you will be prompted for the deployment key you'd like to use. If you don't already have it, you can retreive this value by running `code-push deployment ls <appName> -k`, or you can choose to ignore it (by simply hitting `<ENTER>`) and add it in later. To get started, we would recommend just using your `Staging` deployment key, so that you can test out the CodePush end-to-end.

And that's it for installation using RNPM! Continue below to the [Plugin Configuration](#plugin-configuration-android) section to complete the setup.

#### Plugin Installation (Android - Manual)

1. In your `android/settings.gradle` file, make the following additions:

    ```gradle
    include ':app', ':react-native-code-push'
    project(':react-native-code-push').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-code-push/android/app')
    ```

2. In your `android/app/build.gradle` file, add the `:react-native-code-push` project as a compile-time dependency:

    ```gradle
    ...
    dependencies {
        ...
        compile project(':react-native-code-push')
    }
    ```

3. In your `android/app/build.gradle` file, add the `codepush.gradle` file as an additional build task definition underneath `react.gradle`:

    ```gradle
    ...
    apply from: "../../node_modules/react-native/react.gradle"
    apply from: "../../node_modules/react-native-code-push/android/codepush.gradle"
    ...
    ```

### Plugin Configuration (Android)

*NOTE: If you used RNPM or `react-native link` to automatically link the plugin, these steps have already been done for you so you may skip this section.*

After installing the plugin and syncing your Android Studio project with Gradle, you need to configure your app to consult CodePush for the location of your JS bundle, since it will "take control" of managing the current and all future versions. To do this:

**For React Native >= v0.29**

Update the `MainApplication.java` file to use CodePush via the following changes:

```java
...
// 1. Import the plugin class.
import com.microsoft.codepush.react.CodePush;

public class MainApplication extends Application implements ReactApplication {

    private final ReactNativeHost mReactNativeHost = new ReactNativeHost(this) {
        ...
        // 2. Override the getJSBundleFile method in order to let
        // the CodePush runtime determine where to get the JS
        // bundle location from on each app start
        @Override
        protected String getJSBundleFile() {
            return CodePush.getJSBundleFile();
        }

        @Override
        protected List<ReactPackage> getPackages() {
            // 3. Instantiate an instance of the CodePush runtime and add it to the list of
            // existing packages, specifying the right deployment key. If you don't already
            // have it, you can run "code-push deployment ls <appName> -k" to retrieve your key.
            return Arrays.<ReactPackage>asList(
                new MainReactPackage(),
                new CodePush("deployment-key-here", MainApplication.this, BuildConfig.DEBUG)
            );
        }
    };
}
```

**For React Native v0.19 - v0.28**

Update the `MainActivity.java` file to use CodePush via the following changes:

```java
...
// 1. Import the plugin class (if you used RNPM to install the plugin, this
// should already be done for you automatically so you can skip this step).
import com.microsoft.codepush.react.CodePush;

public class MainActivity extends ReactActivity {
    // 2. Override the getJSBundleFile method in order to let
    // the CodePush runtime determine where to get the JS
    // bundle location from on each app start
    @Override
    protected String getJSBundleFile() {
        return CodePush.getJSBundleFile();
    }

    @Override
    protected List<ReactPackage> getPackages() {
        // 3. Instantiate an instance of the CodePush runtime and add it to the list of
        // existing packages, specifying the right deployment key. If you don't already
        // have it, you can run "code-push deployment ls <appName> -k" to retrieve your key.
        return Arrays.<ReactPackage>asList(
            new MainReactPackage(),
            new CodePush("deployment-key-here", this, BuildConfig.DEBUG)
        );
    }

    ...
}
```

#### Background React Instances

*This section is only necessary if you're <b>explicitly</b> launching a React Native instance without an `Activity` (for example, from within a native push notification receiver). For these situations, CodePush must be told how to find your React Native instance.*

In order to update/restart your React Native instance, CodePush must be configured with a `ReactInstanceHolder` before attempting to restart an instance in the background. This is usually done in your `Application` implementation.

**For React Native >= v0.29**

Update the `MainApplication.java` file to use CodePush via the following changes:

```java
...
// 1. Declare your ReactNativeHost to extend ReactInstanceHolder. ReactInstanceHolder is a subset of ReactNativeHost, so no additional implementation is needed.
import com.microsoft.codepush.react.ReactInstanceHolder;

public class MyReactNativeHost extends ReactNativeHost implements ReactInstanceHolder {
  // ... usual overrides
}

// 2. Provide your ReactNativeHost to CodePush.

public class MainApplication extends Application implements ReactApplication {

   private final MyReactNativeHost mReactNativeHost = new MyReactNativeHost(this);

   @Override
   public void onCreate() {
     CodePush.setReactInstanceHolder(mReactNativeHost);
     super.onCreate();
  }
}
```

**For React Native v0.19 - v0.28**

Before v0.29, React Native did not provide a `ReactNativeHost` abstraction. If you're launching a background instance, you'll likely have built your own, which should now implement `ReactInstanceHolder`. Once that's done:

```java
// 1. Provide your ReactInstanceHolder to CodePush.

public class MainApplication extends Application {

   @Override
   public void onCreate() {
     // ... initialize your instance holder
     CodePush.setReactInstanceHolder(myInstanceHolder);
     super.onCreate();
  }
}
```

In order to effectively make use of the `Staging` and `Production` deployments that were created along with your CodePush app, refer to the [multi-deployment testing](../README.md#multi-deployment-testing) docs below before actually moving your app's usage of CodePush into production.
