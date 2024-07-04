## Android Setup

* [Plugin Installation and Configuration for React Native 0.60 version and above](#plugin-installation-and-configuration-for-react-native-060-version-and-above-android)
* [Plugin Installation for React Native lower than 0.60 (Android)](#plugin-installation-for-react-native-lower-than-060-android)
  * [Plugin Installation (Android - RNPM)](#plugin-installation-android---rnpm)
  * [Plugin Installation (Android - Manual)](#plugin-installation-android---manual)
* [Plugin Configuration for React Native lower than 0.60 (Android)](#plugin-configuration-for-react-native-lower-than-060-android)
  * [For React Native v0.29 - v0.59](#for-react-native-v029---v059)
    * [For newly created React Native application](#for-newly-created-react-native-application)
    * [For existing native application](#for-existing-native-application)
  * [For React Native v0.19 - v0.28](#for-react-native-v019---v028)
  * [Background React Instances](#background-react-instances)
    * [For React Native >= v0.29 (Background React Instances)](#for-react-native--v029-background-react-instances)
    * [For React Native v0.19 - v0.28 (Background React Instances)](#for-react-native-v019---v028-background-react-instances)
  * [WIX React Native Navigation applications (ver 1.x)](#wix-react-native-navigation-applications)
* [Code Signing setup](#code-signing-setup)

In order to integrate CodePush into your Android project, please perform the following steps:

### Plugin Installation and Configuration for React Native 0.60 version and above (Android)

1. In your `android/settings.gradle` file, make the following additions at the end of the file:

    ```gradle
    ...
    include ':app', ':react-native-code-push'
    project(':react-native-code-push').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-code-push/android/app')
    ```
    
2. In your `android/app/build.gradle` file, add the `codepush.gradle` file as an additional build task definition to the end of the file:

    ```gradle
    ...
    apply from: "../../node_modules/react-native-code-push/android/codepush.gradle"
    ...
    ```

3. Update the `MainApplication` file to use CodePush via the following changes:

    For React Native 0.73 and above: update the `MainApplication.kt`

    ```kotlin
    ...
    // 1. Import the plugin class.
    import com.microsoft.codepush.react.CodePush

    class MainApplication : Application(), ReactApplication {

    override val reactNativeHost: ReactNativeHost =
        object : DefaultReactNativeHost(this) {
            ...

            // 2. Override the getJSBundleFile method in order to let
            // the CodePush runtime determine where to get the JS
            // bundle location from on each app start
            override fun getJSBundleFile(): String {
                return CodePush.getJSBundleFile() 
            }
        };
    }
    ```

    For React Native 0.72 and below: update the `MainApplication.java`

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
        };
    }
    ```

4. Add the Deployment key to `strings.xml`:

   To let the CodePush runtime know which deployment it should query for updates, open your app's `strings.xml` file and add a new string named `CodePushDeploymentKey`, whose value is the key of the deployment you want to configure this app against (like the key for the `Staging` deployment for the `FooBar` app). You can retrieve this value by running `appcenter codepush deployment list -a <ownerName>/<appName> -k` in the CodePush CLI (the `-k` flag is necessary since keys aren't displayed by default) and copying the value of the `Key` column which corresponds to the deployment you want to use (see below). Note that using the deployment's name (like Staging) will not work. The "friendly name" is intended only for authenticated management usage from the CLI, and not for public consumption within your app.

   ![Deployment list](https://cloud.githubusercontent.com/assets/116461/11601733/13011d5e-9a8a-11e5-9ce2-b100498ffb34.png)

   In order to effectively make use of the `Staging` and `Production` deployments that were created along with your CodePush app, refer to the [multi-deployment testing](../README.md#multi-deployment-testing) docs below before actually moving your app's usage of CodePush into production.

   Your `strings.xml` should looks like this:

   ```xml
    <resources>
        <string name="app_name">AppName</string>
        <string moduleConfig="true" name="CodePushDeploymentKey">DeploymentKey</string>
    </resources>
    ```

    *Note: If you need to dynamically use a different deployment, you can also override your deployment key in JS code using [Code-Push options](./api-js.md#CodePushOptions)*

### Plugin Installation for React Native lower than 0.60 (Android)

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

2. If you're using RNPM >=1.6.0, you will be prompted for the deployment key you'd like to use. If you don't already have it, you can retrieve this value by running `appcenter codepush deployment list -a <ownerName>/<appName> -k`, or you can choose to ignore it (by simply hitting `<ENTER>`) and add it in later. To get started, we would recommend just using your `Staging` deployment key, so that you can test out the CodePush end-to-end.

And that's it for installation using RNPM! Continue below to the [Plugin Configuration](#plugin-configuration-for-react-native-lower-than-060-android) section to complete the setup.

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

### Plugin Configuration for React Native lower than 0.60 (Android)

*NOTE: If you used RNPM or `react-native link` to automatically link the plugin, these steps have already been done for you so you may skip this section.*

After installing the plugin and syncing your Android Studio project with Gradle, you need to configure your app to consult CodePush for the location of your JS bundle, since it will "take control" of managing the current and all future versions. To do this:

#### For React Native v0.29 - v0.59

##### For newly created React Native application

If you are integrating Code Push into React Native application please do the following steps:

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
            // have it, you can run "appcenter codepush deployment list -a <ownerName>/<appName> -k" to retrieve your key.
            return Arrays.<ReactPackage>asList(
                new MainReactPackage(),
                new CodePush("deployment-key-here", MainApplication.this, BuildConfig.DEBUG)
            );
        }
    };
}
```

*NOTE: For React Native v0.49+ please be sure that `getJSMainModuleName` function in the `MainApplication.java` file determines correct URL to fetch JS bundle (used when dev support is enabled, see [this](https://github.com/facebook/react-native/blob/c7f37074ac89f7e568ca26a6bad3bdb02812c39f/ReactAndroid/src/main/java/com/facebook/react/ReactNativeHost.java#L124) for more details) e.g.*
```
@Override
protected String getJSMainModuleName() {
    return "index";
}
```

##### For existing native application

If you are integrating React Native into existing native application please do the following steps:

Update `MyReactActivity.java` (it could be named differently in your app) file to use CodePush via the following changes:

```java
...
// 1. Import the plugin class.
import com.microsoft.codepush.react.CodePush;

public class MyReactActivity extends Activity {
    private ReactRootView mReactRootView;
    private ReactInstanceManager mReactInstanceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ...
        mReactInstanceManager = ReactInstanceManager.builder()
                // ...
                // Add CodePush package
                .addPackage(new CodePush("deployment-key-here", getApplicationContext(), BuildConfig.DEBUG))
                // Get the JS Bundle File via Code Push
                .setJSBundleFile(CodePush.getJSBundleFile())
                // ...
                
                .build();
        mReactRootView.startReactApplication(mReactInstanceManager, "MyReactNativeApp", null);

        setContentView(mReactRootView);
    }

    ...
}
```

#### For React Native v0.19 - v0.28

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
        // have it, you can run "appcenter codepush deployment list -a <ownerName>/<appName> -k" to retrieve your key.
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

In order to update/restart your React Native instance, CodePush must be configured with a `ReactInstanceHolder` before attempting to restart an instance in the background. This is done in your `Application` implementation.

##### For React Native >= v0.29 (Background React Instances)

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

##### For React Native v0.19 - v0.28 (Background React Instances)

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

#### WIX React Native Navigation applications

If you are using [WIX React Native Navigation **version 1.x**](https://github.com/wix/react-native-navigation) based application, please do the following steps to integrate CodePush:

1. No need to change `MainActivity.java` file, so if you are integrating CodePush to newly created RNN application it might be looking like this:

```java
import com.facebook.react.ReactActivity;
import com.reactnativenavigation.controllers.SplashActivity;

public class MainActivity extends SplashActivity {

}
```

2. Update the `MainApplication.java` file to use CodePush via the following changes:

```java
// ...
import com.facebook.react.ReactInstanceManager;

// Add CodePush imports
import com.microsoft.codepush.react.ReactInstanceHolder;
import com.microsoft.codepush.react.CodePush;

public class MainApplication extends NavigationApplication implements ReactInstanceHolder {

	@Override
	public boolean isDebug() {
		// Make sure you are using BuildConfig from your own application
		return BuildConfig.DEBUG;
	}

	protected List<ReactPackage> getPackages() {
		// Add additional packages you require here
		return Arrays.<ReactPackage>asList(
			new CodePush("deployment-key-here", getApplicationContext(), BuildConfig.DEBUG)
		);
	}

	@Override
	public List<ReactPackage> createAdditionalReactPackages() {
		return getPackages();
	}

	@Override
	public String getJSBundleFile() {
        // Override default getJSBundleFile method with the one CodePush is providing
		return CodePush.getJSBundleFile();
	}

	@Override
	public String getJSMainModuleName() {
		return "index";
	}

    @Override
    public ReactInstanceManager getReactInstanceManager() {
        // CodePush must be told how to find React Native instance
        return getReactNativeHost().getReactInstanceManager();
    }
}
```
If you are using [WIX React Native Navigation **version 2.x**](https://github.com/wix/react-native-navigation/tree/v2) based application, please do the following steps to integrate CodePush:

1. As per React Native Navigation's documentation, `MainActivity.java` should extend `NavigationActivity`, no changes required to incorporate CodePush:

```java
import com.reactnativenavigation.NavigationActivity;

public class MainActivity extends NavigationActivity {

}
```

2. Update the `MainApplication.java` file to use CodePush via the following changes:

```java
// ...
import com.facebook.react.ReactInstanceManager;

// Add CodePush imports
import com.microsoft.codepush.react.CodePush;

public class MainApplication extends NavigationApplication {

    @Override
    public boolean isDebug() {
        return BuildConfig.DEBUG;
    }

    @Override
    protected ReactGateway createReactGateway() {
        ReactNativeHost host = new NavigationReactNativeHost(this, isDebug(), createAdditionalReactPackages()) {
            @javax.annotation.Nullable
            @Override
            protected String getJSBundleFile() {
                return CodePush.getJSBundleFile();
            }
            
        };
        return new ReactGateway(this, isDebug(), host);
    }

    @Override
    public List<ReactPackage> createAdditionalReactPackages() {
        return Arrays.<ReactPackage>asList(
	    new CodePush("deployment-key-here", getApplicationContext(), isDebug())
	    //,MainReactPackage , etc...
    }
}
```

### Code Signing setup

Starting with CLI version **2.1.0** you can self sign bundles during release and verify its signature before installation of update. For more info about Code Signing please refer to [relevant code-push documentation section](https://github.com/microsoft/code-push/tree/v3.0.1/cli#code-signing). In order to use Public Key for Code Signing you need to do following steps:

   Add `CodePushPublicKey` string item to `/path_to_your_app/android/app/src/main/res/values/strings.xml`. It may looks like this:

 ```xml
 <resources>
    <string name="app_name">my_app</string>
    <string name="CodePushPublicKey">-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtPSR9lkGzZ4FR0lxF+ZA
P6jJ8+Xi5L601BPN4QESoRVSrJM08roOCVrs4qoYqYJy3Of2cQWvNBEh8ti3FhHu
tiuLFpNdfzM4DjAw0Ti5hOTfTixqVBXTJPYpSjDh7K6tUvp9MV0l5q/Ps3se1vud
M1/X6g54lIX/QoEXTdMgR+SKXvlUIC13T7GkDHT6Z4RlwxkWkOmf2tGguRcEBL6j
ww7w/3g0kWILz7nNPtXyDhIB9WLH7MKSJWdVCZm+cAqabUfpCFo7sHiyHLnUxcVY
OTw3sz9ceaci7z2r8SZdsfjyjiDJrq69eWtvKVUpredy9HtyALtNuLjDITahdh8A
zwIDAQAB
-----END PUBLIC KEY-----</string>
</resources>
 ```

#### For React Native <= v0.60 you should configure the `CodePush` instance to use this parameter using one of the following approaches

##### Using constructor

```java
new CodePush(
    "deployment-key",
    getApplicationContext(),
    BuildConfig.DEBUG,
    R.string.CodePushPublicKey)
```

##### Using builder

 ```java
new CodePushBuilder("deployment-key-here",getApplicationContext())
    .setIsDebugMode(BuildConfig.DEBUG)
    .setPublicKeyResourceDescriptor(R.string.CodePushPublicKey)
    .build()
```
