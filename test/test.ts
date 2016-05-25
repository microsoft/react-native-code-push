/// <reference path="../typings/assert.d.ts" />
/// <reference path="../typings/codePush.d.ts" />
/// <reference path="../typings/code-push-plugin-testing-framework.d.ts" />
/// <reference path="../typings/mocha.d.ts" />
/// <reference path="../typings/mkdirp.d.ts" />
/// <reference path="../typings/node.d.ts" />
/// <reference path="../typings/q.d.ts" />

"use strict";

import assert = require("assert");
import fs = require("fs");
import mkdirp = require("mkdirp");
import path = require("path");

import { Platform, PluginTestingFramework, ProjectManager, ServerUtil } from "code-push-plugin-testing-framework";

import Q = require("q");

var del = require("del");

// Create the ProjectManager to use for the tests.
class RNProjectManager extends ProjectManager {
    /**
     * Returns the name of the plugin being tested, ie Cordova or React-Native
     */
    public getPluginName(): string {
        return "React-Native";
    }
    
    /**
     * Copies over the template files into the specified project, overwriting existing files.
     */
    public copyTemplate(templatePath: string, projectDirectory: string): Q.Promise<string> {
        function copyDirectoryRecursively(directoryFrom: string, directoryTo: string): Q.Promise<string> {
            var promises: Q.Promise<string>[] = [];
            
            fs.readdirSync(directoryFrom).forEach(file => {
                var fileStats: fs.Stats;
                var fileInFrom: string = path.join(directoryFrom, file);
                var fileInTo: string = path.join(directoryTo, file);
                
                try { fileStats = fs.statSync(fileInFrom); } catch (e) { /* fs.statSync throws if the file doesn't exist. */ }
                
                // If it is a file, just copy directly
                if (fileStats && fileStats.isFile()) {
                    promises.push(ProjectManager.copyFile(fileInFrom, fileInTo, true));
                }
                else {
                    // If it is a directory, create the directory if it doesn't exist on the target and then copy over
                    if (!fs.existsSync(fileInTo)) mkdirp.sync(fileInTo);
                    promises.push(copyDirectoryRecursively(fileInFrom, fileInTo));
                }
            });
            
            // Chain promise so that it maintains Q.Promise<string> type instead of Q.Promise<string[]>
            return Q.all<string>(promises).then(() => { return null; });
        }
        
        return copyDirectoryRecursively(templatePath, path.join(projectDirectory, PluginTestingFramework.TestAppName));
    }

	/**
	 * Creates a new test application at the specified path, and configures it
	 * with the given server URL, android and ios deployment keys.
	 */
    public setupProject(projectDirectory: string, templatePath: string, appName: string, appNamespace: string, version?: string): Q.Promise<string> {
        if (fs.existsSync(projectDirectory)) {
            del.sync([projectDirectory], { force: true });
        }
        mkdirp.sync(projectDirectory);

        // React-Native adds a "com." to the front of the name you provide, so provide the namespace with the "com." removed
        return ProjectManager.execChildProcess("react-native init " + appName + " --package " + appNamespace, { cwd: projectDirectory }, true)
            .then(this.copyTemplate.bind(this, templatePath, projectDirectory))
            .then<string>(ProjectManager.execChildProcess.bind(undefined, "npm install " + PluginTestingFramework.thisPluginPath, { cwd: path.join(projectDirectory, PluginTestingFramework.TestAppName) }));
    }
    
    /** JSON mapping project directories to the current scenario
     *
     *  EXAMPLE:
     *  {
     *      "TEMP_DIR/test-run": "scenarios/scenarioCheckForUpdate.js",
     *      "TEMP_DIR/updates": "scenarios/updateSync.js"
     *  }
     */
    private static currentScenario: any = {};
    
    /** JSON mapping project directories to whether or not they've built the current scenario
     *
     *  EXAMPLE:
     *  {
     *      "TEMP_DIR/test-run": "true",
     *      "TEMP_DIR/updates": "false"
     *  }
     */
    private static currentScenarioHasBuilt: any = {};
    
    /**
     * Sets up the scenario for a test in an already existing project.
     */
    public setupScenario(projectDirectory: string, appId: string, templatePath: string, jsPath: string, targetPlatform: Platform.IPlatform, version?: string): Q.Promise<string> {
        // We don't need to anything if it is the current scenario.
        if (RNProjectManager.currentScenario[projectDirectory] === jsPath) return Q<string>(undefined);
        RNProjectManager.currentScenario[projectDirectory] = jsPath;
        RNProjectManager.currentScenarioHasBuilt[projectDirectory] = false;
        
        var indexHtml = "index.js";
        var templateIndexPath = path.join(templatePath, indexHtml);
        var destinationIndexPath = path.join(projectDirectory, PluginTestingFramework.TestAppName, indexHtml);
        
        var scenarioJs = "scenarios/" + jsPath;
        
        // var packageFile = eval("(" + fs.readFileSync("./package.json", "utf8") + ")");
        // var pluginVersion = packageFile.version;
        
        console.log("Setting up scenario " + jsPath + " in " + projectDirectory);

        // Copy index html file and replace
        return ProjectManager.copyFile(templateIndexPath, destinationIndexPath, true)
            .then<void>(ProjectManager.replaceString.bind(undefined, destinationIndexPath, ProjectManager.CODE_PUSH_TEST_APP_NAME_PLACEHOLDER, PluginTestingFramework.TestAppName))
            .then<void>(ProjectManager.replaceString.bind(undefined, destinationIndexPath, ProjectManager.SERVER_URL_PLACEHOLDER, targetPlatform.getServerUrl()))
            .then<void>(ProjectManager.replaceString.bind(undefined, destinationIndexPath, ProjectManager.INDEX_JS_PLACEHOLDER, scenarioJs))
            .then<string>(ProjectManager.replaceString.bind(undefined, destinationIndexPath, ProjectManager.CODE_PUSH_APP_VERSION_PLACEHOLDER, version));
    }

    /**
     * Creates a CodePush update package zip for a project.
     */
    public createUpdateArchive(projectDirectory: string, targetPlatform: Platform.IPlatform, isDiff?: boolean): Q.Promise<string> {
        /* // Android creates a bundle when it builds, so use that one.
        // NOTE: Android does not support diffs, so always pass false.
        if (targetPlatform === Platform.Android.getInstance()) return Q<string>(path.join(projectDirectory, PluginTestingFramework.TestAppName, "android", "app", "build", "intermediates", "assets", "release", "index.android.bundle"));
        */
        
        var bundleFolder: string = path.join(projectDirectory, PluginTestingFramework.TestAppName, "CodePush/");
        var bundleName: string;
        if (targetPlatform === Platform.IOS.getInstance()) bundleName = "main.jsbundle";
        if (targetPlatform === Platform.Android.getInstance()) bundleName = "index.android.bundle";
        var bundlePath: string = path.join(bundleFolder, bundleName);
        var deferred = Q.defer<string>();
        fs.exists(bundleFolder, (exists) => {
            if (exists) del.sync([bundleFolder], { force: true });
            mkdirp.sync(bundleFolder);
            deferred.resolve(undefined);
        });
        return deferred.promise
            .then(ProjectManager.execChildProcess.bind(undefined, "react-native bundle --platform " + targetPlatform.getName() + " --entry-file index." + targetPlatform.getName() + ".js --bundle-output " + bundlePath + " --assets-dest " + bundleFolder + " --dev false",
                { cwd: path.join(projectDirectory, PluginTestingFramework.TestAppName) }))
            // NOTE: Android does not support diffs, so always pass false if targetPlatform is Android.
            .then<string>(ProjectManager.archiveFolder.bind(undefined, bundleFolder, path.join(projectDirectory, PluginTestingFramework.TestAppName, "update.zip"), targetPlatform !== Platform.Android.getInstance() && isDiff));
    }
    
    /** JSON file containing the platforms the plugin is currently installed for.
     *  Keys must match targetPlatform.getName()!
     *
     *  EXAMPLE:
     *  {
     *      "android": true,
     *      "ios": false
     *  }
     */
    private static platformsJSON: string = "platforms.json";
    
    /**
     * Installs the CodePush plugin for Android and prepares the test app to run Android tests.
     */
    public static installCodePushAndPrepareAndroid(innerProjectFolder: string): Q.Promise<string> {
        //// Set up gradle to build CodePush with the app
        // Add CodePush to android/app/build.gradle
        var buildGradle = path.join(innerProjectFolder, "android", "app", "build.gradle");
        ProjectManager.replaceString(buildGradle,
            "apply from: \"../../node_modules/react-native/react.gradle\"",
            "apply from: \"../../node_modules/react-native/react.gradle\"\napply from: \"" + path.join(innerProjectFolder, "node_modules", "react-native-code-push", "android", "codepush.gradle") + "\"");
        ProjectManager.replaceString(buildGradle,
            "compile \"com.facebook.react:react-native:+\"",
            "compile \"com.facebook.react:react-native:0.25.+\"");
        ProjectManager.replaceString(buildGradle,
            "// From node_modules",
            "\n    compile project(':react-native-code-push') // From node_modules");
        // Add CodePush to android/settings.gradle
        ProjectManager.replaceString(path.join(innerProjectFolder, "android", "settings.gradle"),
            "include ':app'",
            "include ':app', ':react-native-code-push'\nproject(':react-native-code-push').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-code-push/android/app')");
        
        //// Set the app version to 1.0.0 instead of 1.0
        // Set the app version to 1.0.0 in android/app/build.gradle
        ProjectManager.replaceString(buildGradle, "versionName \"1.0\"", "versionName \"1.0.0\"");
        // Set the app version to 1.0.0 in AndroidManifest.xml
        ProjectManager.replaceString(path.join(innerProjectFolder, "android", "app", "src", "main", "AndroidManifest.xml"), "android:versionName=\"1.0\"", "android:versionName=\"1.0.0\"");
            
        //// Replace the MainActivity.java with the correct server url and deployment key
        var mainActivity = path.join(innerProjectFolder, "android", "app", "src", "main", "java", "com", "microsoft", "codepush", "test", "MainActivity.java");
        ProjectManager.replaceString(mainActivity, ProjectManager.CODE_PUSH_TEST_APP_NAME_PLACEHOLDER, PluginTestingFramework.TestAppName);
        ProjectManager.replaceString(mainActivity, ProjectManager.SERVER_URL_PLACEHOLDER, Platform.Android.getInstance().getServerUrl());
        ProjectManager.replaceString(mainActivity, ProjectManager.ANDROID_KEY_PLACEHOLDER, Platform.Android.getInstance().getDefaultDeploymentKey());
        
        return Q<string>(undefined);
    }
    
    /**
     * Installs the CodePush plugin for Android and prepares the test app to run Android tests.
     */
    public static installCodePushAndPrepareIOS(innerProjectFolder: string): Q.Promise<string> {
        var iOSProject: string = path.join(innerProjectFolder, "ios");
        var infoPlistPath: string = path.join(iOSProject, PluginTestingFramework.TestAppName, "Info.plist");
        var appDelegatePath: string = path.join(iOSProject, PluginTestingFramework.TestAppName, "AppDelegate.m");
        // Create and install the Podfile
        return ProjectManager.execChildProcess("pod init", { cwd: iOSProject })
            .then(() => { return fs.appendFileSync(path.join(iOSProject, "Podfile"),
                "target '" + PluginTestingFramework.TestAppName + "'\n  pod 'React', :path => '../node_modules/react-native', :subspecs => [ 'Core', 'RCTImage', 'RCTNetwork', 'RCTText', 'RCTWebSocket', ]\n  pod 'CodePush', :path => '../node_modules/react-native-code-push'\n"); })
            // Put the IOS deployment key in the Info.plist
            .then(ProjectManager.replaceString.bind(undefined, infoPlistPath,
                "</dict>\n</plist>",
                "<key>CodePushDeploymentKey</key>\n\t<string>" + Platform.IOS.getInstance().getDefaultDeploymentKey() + "</string>\n\t<key>CodePushServerURL</key>\n\t<string>" + Platform.IOS.getInstance().getServerUrl() + "</string>\n\t</dict>\n</plist>"))
            // Add the correct linker flags to the project.pbxproj
            .then(ProjectManager.replaceString.bind(undefined, path.join(iOSProject, PluginTestingFramework.TestAppName + ".xcodeproj", "project.pbxproj"), 
                "\"-lc[+][+]\",", "\"-lc++\", \"$(inherited)\""))
            // Install the Pod
            .then(ProjectManager.execChildProcess.bind(undefined, "pod install", { cwd: iOSProject }))
            // Add the correct bundle identifier to the Info.plist
            .then(ProjectManager.replaceString.bind(undefined, infoPlistPath, 
                "org[.]reactjs[.]native[.]example[.][$][(]PRODUCT_NAME:rfc1034identifier[)]",
                PluginTestingFramework.TestNamespace))
            // Set the app version to 1.0.0 instead of 1.0 in the Info.plist
            .then(ProjectManager.replaceString.bind(undefined, infoPlistPath, "1.0", "1.0.0"))
            // Fix the linker flag list in project.pbxproj (pod install adds an extra comma)
            .then(ProjectManager.replaceString.bind(undefined, path.join(iOSProject, PluginTestingFramework.TestAppName + ".xcodeproj", "project.pbxproj"), 
                "\"[$][(]inherited[)]\",\\s*[)];", "\"$(inherited)\"\n\t\t\t\t);"))
            // Copy the AppDelegate.m to the project
            .then(ProjectManager.copyFile.bind(undefined,
                path.join(PluginTestingFramework.templatePath, "ios", PluginTestingFramework.TestAppName, "AppDelegate.m"),
                appDelegatePath, true))
            .then<string>(ProjectManager.replaceString.bind(undefined, appDelegatePath, ProjectManager.CODE_PUSH_TEST_APP_NAME_PLACEHOLDER, PluginTestingFramework.TestAppName))
    }
    
    /**
     * Prepares a specific platform for tests.
     */
    public preparePlatform(projectFolder: string, targetPlatform: Platform.IPlatform): Q.Promise<string> {
        var deferred= Q.defer<string>();
        
        var platformsJSONPath = path.join(projectFolder, RNProjectManager.platformsJSON);
        
        // We create a JSON file in the project folder to contain the installed platforms.
        // Check the file to see if the plugin for this platform has been installed and update the file appropriately.
        fs.exists(platformsJSONPath, (exists) => {
            if (!exists) {
                fs.writeFileSync(platformsJSONPath, "{}");
            }
            
            var platformJSON = eval("(" + fs.readFileSync(platformsJSONPath, "utf8") + ")");
            if (platformJSON[targetPlatform.getName()] === true) deferred.reject("Platform " + targetPlatform.getName() + " is already installed in " + projectFolder + "!");
            else {
                platformJSON[targetPlatform.getName()] = true;
                fs.writeFileSync(platformsJSONPath, JSON.stringify(platformJSON));
                deferred.resolve(undefined);
            }
        });
        
        var innerProjectFolder: string = path.join(projectFolder, PluginTestingFramework.TestAppName);
        
        return deferred.promise
            .then<string>(() => {
                if (targetPlatform === Platform.Android.getInstance()) {
                    return RNProjectManager.installCodePushAndPrepareAndroid(innerProjectFolder);
                } else if (targetPlatform === Platform.IOS.getInstance()) {
                    return RNProjectManager.installCodePushAndPrepareIOS(innerProjectFolder);
                }
            }, (error) => { /* The platform is already installed! */ console.log(error); return null; });
    }
    
    /**
     * Cleans up a specific platform after tests.
     */
    public cleanupAfterPlatform(projectFolder: string, targetPlatform: Platform.IPlatform): Q.Promise<string> {
        // Can't uninstall from command line, so noop.
        return Q<string>(undefined);
    }
    
    private static getApkPath(projectDirectory: string): string {
        return path.join(projectDirectory, PluginTestingFramework.TestAppName, "android", "app", "build", "outputs", "apk", "app-release-unsigned.apk");
    }
    
    /**
     * Builds the test app on Android.
     */
    public static buildAndroid(projectDirectory: string): Q.Promise<string> {
        // In order to run on Android without the package manager, we must create a release APK and then sign it with the debug certificate.
        var androidDirectory: string = path.join(projectDirectory, PluginTestingFramework.TestAppName, "android");
        var apkPath = RNProjectManager.getApkPath(projectDirectory);
        return ProjectManager.execChildProcess("./gradlew assembleRelease --daemon", { cwd: androidDirectory })
            .then<string>(ProjectManager.execChildProcess.bind(undefined, "jarsigner -verbose -keystore ~/.android/debug.keystore -storepass android -keypass android " + apkPath + " androiddebugkey", { cwd: androidDirectory }, false));
    }
    
    /**
     * Maps project directories to whether or not they have built an IOS project before.
     * 
     * Currently, React-Native resets your bundle identifier on the first build of the app.
     * Obviously we don't want this, so on the first build we need to fix the package name and build again.
     *
     *  EXAMPLE:
     *  {
     *      "TEMP_DIR/test-run": true,
     *      "TEMP_DIR/updates": false
     *  }
     */
    private static iosFirstBuild: any = {};
    
    /**
     * Builds the test app on IOS.
     */
    public static buildIOS(projectDirectory: string): Q.Promise<string> {
        // NOTE: fix this for any device
        var iOSProject: string = path.join(projectDirectory, PluginTestingFramework.TestAppName, "ios");
        
        // Don't log the build output because iOS's build output is too verbose and overflows the buffer!
        return ProjectManager.execChildProcess("xcodebuild -workspace " + path.join(iOSProject, PluginTestingFramework.TestAppName) + ".xcworkspace -scheme " + PluginTestingFramework.TestAppName + " -configuration Release -destination \"platform=iOS Simulator,id=" +
                "A63E7FA6-05E4-4726-A8FA-A61B95652D65" + "\" -derivedDataPath build", { cwd: iOSProject, maxBuffer: 1024 * 1000 * 10 }, false)
            .then<string>(
                () => { return null; },
                () => {
                    // The first time an iOS project is built, it fails because it does not finish building libReact.a before it builds the test app.
                    // Simply build again to fix the issue.
                    if (!RNProjectManager.iosFirstBuild[projectDirectory]) {
                        RNProjectManager.iosFirstBuild[projectDirectory] = true;
                        return RNProjectManager.buildIOS(projectDirectory);
                    }
                    return null;
                });
    }
    
    /**
     * Builds the test app on the specified platform.
     */
    public static buildTestApp(projectDirectory: string, targetPlatform: Platform.IPlatform): Q.Promise<string> {
        if (targetPlatform === Platform.Android.getInstance()) {
            return RNProjectManager.buildAndroid(projectDirectory);
        } else if (targetPlatform === Platform.IOS.getInstance()) {
            return RNProjectManager.buildIOS(projectDirectory);
        }
        return Q<string>(undefined);
    }

    /**
     * Runs the test app on the given target / platform.
     */
    public runPlatform(projectFolder: string, targetPlatform: Platform.IPlatform): Q.Promise<string> {
        console.log("Running project in " + projectFolder + " on " + targetPlatform.getName());
        
        return Q<string>(undefined)
            .then(() => {
                // Build if this scenario has not yet been built.
                if (!RNProjectManager.currentScenarioHasBuilt[projectFolder]) {
                    RNProjectManager.currentScenarioHasBuilt[projectFolder] = true;
                    return RNProjectManager.buildTestApp(projectFolder, targetPlatform);
                }
            })
            .then(() => {
                // Uninstall so that none of the app's data carries over between tests.
                return targetPlatform.getEmulatorManager().uninstallApplication(PluginTestingFramework.TestNamespace);
            })
            .then(() => {
                if (targetPlatform === Platform.Android.getInstance()) {
                    var androidDirectory: string = path.join(projectFolder, PluginTestingFramework.TestAppName, "android");
                    return ProjectManager.execChildProcess("adb install -r " + RNProjectManager.getApkPath(projectFolder), { cwd: androidDirectory })
                        .then<string>(targetPlatform.getEmulatorManager().launchInstalledApplication.bind(undefined, PluginTestingFramework.TestNamespace));
                } else if (targetPlatform === Platform.IOS.getInstance()) {
                    var iOSProject: string = path.join(projectFolder, PluginTestingFramework.TestAppName, "ios");
                    return ProjectManager.execChildProcess("xcrun simctl install booted " + path.join(iOSProject, "build", "Build", "Products", "Release-iphonesimulator", PluginTestingFramework.TestAppName + ".app"))
                        .then<string>(targetPlatform.getEmulatorManager().launchInstalledApplication.bind(undefined, PluginTestingFramework.TestNamespace));
                }
            });
    }
};

// Scenarios used in the tests.
const ScenarioCheckForUpdatePath = "scenarioCheckForUpdate.js";
const ScenarioCheckForUpdateCustomKey = "scenarioCheckForUpdateCustomKey.js";
const ScenarioDownloadUpdate = "scenarioDownloadUpdate.js";
const ScenarioInstall = "scenarioInstall.js";
const ScenarioInstallOnResumeWithRevert = "scenarioInstallOnResumeWithRevert.js";
const ScenarioInstallOnRestartWithRevert = "scenarioInstallOnRestartWithRevert.js";
const ScenarioInstallWithRevert = "scenarioInstallWithRevert.js";
const ScenarioSync1x = "scenarioSync.js";
const ScenarioSyncResume = "scenarioSyncResume.js";
const ScenarioSyncResumeDelay = "scenarioSyncResumeDelay.js";
const ScenarioSyncRestartDelay = "scenarioSyncResumeDelay.js";
const ScenarioSync2x = "scenarioSync2x.js";
const ScenarioRestart = "scenarioRestart.js";
const ScenarioSyncMandatoryDefault = "scenarioSyncMandatoryDefault.js";
const ScenarioSyncMandatoryResume = "scenarioSyncMandatoryResume.js";
const ScenarioSyncMandatoryRestart = "scenarioSyncMandatoryRestart.js";

const UpdateDeviceReady = "updateDeviceReady.js";
const UpdateNotifyApplicationReady = "updateNotifyApplicationReady.js";
const UpdateSync = "updateSync.js";
const UpdateSync2x = "updateSync2x.js";
const UpdateNotifyApplicationReadyConditional = "updateNARConditional.js";

// Describe the tests.
var testBuilderDescribes: PluginTestingFramework.TestBuilderDescribe[] = [
    
    new PluginTestingFramework.TestBuilderDescribe("#window.codePush.checkForUpdate",
    
        [
            new PluginTestingFramework.TestBuilderIt("window.codePush.checkForUpdate.noUpdate",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    var noUpdateResponse = PluginTestingFramework.createDefaultResponse();
                    noUpdateResponse.isAvailable = false;
                    noUpdateResponse.appVersion = "0.0.1";
                    PluginTestingFramework.updateResponse = { updateInfo: noUpdateResponse };

                    PluginTestingFramework.testMessageCallback = (requestBody: any) => {
                        try {
                            assert.equal(requestBody.message, ServerUtil.TestMessage.CHECK_UP_TO_DATE);
                            done();
                        } catch (e) {
                            done(e);
                        }
                    };

                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                },
                false),
            
            new PluginTestingFramework.TestBuilderIt("window.codePush.checkForUpdate.sendsBinaryHash",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    if (targetPlatform === Platform.Android.getInstance()) {
                        console.log("Android does not send a binary hash!");
                        done();
                        return;
                    }
                    
                    var noUpdateResponse = PluginTestingFramework.createDefaultResponse();
                        noUpdateResponse.isAvailable = false;
                        noUpdateResponse.appVersion = "0.0.1";

                        PluginTestingFramework.updateCheckCallback = (request: any) => {
                            try {
                                assert(request.query.packageHash);
                            } catch (e) {
                                done(e);
                            }
                        };
                        
                        PluginTestingFramework.updateResponse = { updateInfo: noUpdateResponse };

                        PluginTestingFramework.testMessageCallback = (requestBody: any) => {
                            try {
                                assert.equal(requestBody.message, ServerUtil.TestMessage.CHECK_UP_TO_DATE);
                                done();
                            } catch (e) {
                                done(e);
                            }
                        };

                        projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                }, false),
            
            new PluginTestingFramework.TestBuilderIt("window.codePush.checkForUpdate.noUpdate.updateAppVersion", 
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    var updateAppVersionResponse = PluginTestingFramework.createDefaultResponse();
                    updateAppVersionResponse.updateAppVersion = true;
                    updateAppVersionResponse.appVersion = "2.0.0";

                    PluginTestingFramework.updateResponse = { updateInfo: updateAppVersionResponse };

                    PluginTestingFramework.testMessageCallback = (requestBody: any) => {
                        try {
                            assert.equal(requestBody.message, ServerUtil.TestMessage.CHECK_UP_TO_DATE);
                            done();
                        } catch (e) {
                            done(e);
                        }
                    };

                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                }, false),
            
            new PluginTestingFramework.TestBuilderIt("window.codePush.checkForUpdate.update", 
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    var updateResponse = PluginTestingFramework.createMockResponse();
                    PluginTestingFramework.updateResponse = { updateInfo: updateResponse };

                    PluginTestingFramework.testMessageCallback = (requestBody: any) => {
                        try {
                            assert.equal(requestBody.message, ServerUtil.TestMessage.CHECK_UPDATE_AVAILABLE);
                            assert.notEqual(requestBody.args[0], null);
                            var remotePackage: IRemotePackage = requestBody.args[0];
                            assert.equal(remotePackage.downloadUrl, updateResponse.downloadURL);
                            assert.equal(remotePackage.isMandatory, updateResponse.isMandatory);
                            assert.equal(remotePackage.label, updateResponse.label);
                            assert.equal(remotePackage.packageHash, updateResponse.packageHash);
                            assert.equal(remotePackage.packageSize, updateResponse.packageSize);
                            assert.equal(remotePackage.deploymentKey, targetPlatform.getDefaultDeploymentKey());
                            done();
                        } catch (e) {
                            done(e);
                        }
                    };

                    PluginTestingFramework.updateCheckCallback = (request: any) => {
                        try {
                            assert.notEqual(null, request);
                            assert.equal(request.query.deploymentKey, targetPlatform.getDefaultDeploymentKey());
                        } catch (e) {
                            done(e);
                        }
                    };

                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                }, true),
            
            new PluginTestingFramework.TestBuilderIt("window.codePush.checkForUpdate.error", 
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    PluginTestingFramework.updateResponse = "invalid {{ json";

                    PluginTestingFramework.testMessageCallback = (requestBody: any) => {
                        try {
                            assert.equal(requestBody.message, ServerUtil.TestMessage.CHECK_ERROR);
                            done();
                        } catch (e) {
                            done(e);
                        }
                    };

                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                }, false)
        ], ScenarioCheckForUpdatePath),
    
    new PluginTestingFramework.TestBuilderDescribe("#window.codePush.checkForUpdate.customKey",
        
        [new PluginTestingFramework.TestBuilderIt("window.codePush.checkForUpdate.customKey.update",
            (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                var updateResponse = PluginTestingFramework.createMockResponse();
                PluginTestingFramework.updateResponse = { updateInfo: updateResponse };

                PluginTestingFramework.updateCheckCallback = (request: any) => {
                    try {
                        assert.notEqual(null, request);
                        assert.equal(request.query.deploymentKey, "CUSTOM-DEPLOYMENT-KEY");
                        done();
                    } catch (e) {
                        done(e);
                    }
                };

                projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
            }, false)],
        ScenarioCheckForUpdateCustomKey),
        
    new PluginTestingFramework.TestBuilderDescribe("#remotePackage.download",
        
        [
            new PluginTestingFramework.TestBuilderIt("remotePackage.download.success",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

                    /* pass the path to any file for download (here, index.js) to make sure the download completed callback is invoked */
                    PluginTestingFramework.updatePackagePath = path.join(PluginTestingFramework.templatePath, "index.js");
                    
                    var deferred = Q.defer<void>();
                    deferred.promise.then(() => { done(); }, (e) => { done(e); });

                    PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                        ServerUtil.TestMessage.CHECK_UPDATE_AVAILABLE,
                        ServerUtil.TestMessage.DOWNLOAD_SUCCEEDED], deferred);

                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                }, false),
            
            new PluginTestingFramework.TestBuilderIt("remotePackage.download.error",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

                    /* pass an invalid path */
                    PluginTestingFramework.updatePackagePath = path.join(PluginTestingFramework.templatePath, "invalid_path.zip");
                    
                    var deferred = Q.defer<void>();
                    deferred.promise.then(() => { done(); }, (e) => { done(e); });

                    PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                        ServerUtil.TestMessage.CHECK_UPDATE_AVAILABLE,
                        ServerUtil.TestMessage.DOWNLOAD_ERROR], deferred);

                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                }, false)
        ], ScenarioDownloadUpdate),
        
    new PluginTestingFramework.TestBuilderDescribe("#localPackage.install",
    
        [
            // // CHANGE THIS TEST CASE, accepts both a jsbundle and a zip
            // new PluginTestingFramework.TestBuilderIt("localPackage.install.unzip.error",
            //     (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
            //         PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

            //         /* pass an invalid zip file, here, index.js */
            //         PluginTestingFramework.updatePackagePath = path.join(PluginTestingFramework.templatePath, "index.js");
                    
            //         var deferred = Q.defer<void>();
            //         deferred.promise.then(() => { done(); }, (e) => { done(e); });

            //         PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
            //             ServerUtil.TestMessage.CHECK_UPDATE_AVAILABLE,
            //             ServerUtil.TestMessage.DOWNLOAD_SUCCEEDED,
            //             ServerUtil.TestMessage.INSTALL_ERROR], deferred);

            //         projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
            //     }, false),
            
            new PluginTestingFramework.TestBuilderIt("localPackage.install.handlesDiff.againstBinary",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    if (targetPlatform === Platform.Android.getInstance()) {
                        console.log("Android does not support diffs!");
                        done();
                        return;
                    }
                    
                    PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

                    /* create an update */
                    PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateNotifyApplicationReady, "Diff Update 1")
                        .then<void>((updatePath: string) => {
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.updatePackagePath = updatePath;
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                ServerUtil.TestMessage.CHECK_UPDATE_AVAILABLE,
                                ServerUtil.TestMessage.DOWNLOAD_SUCCEEDED,
                                ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
                            projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* run the app again to ensure it was not reverted */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .done(() => { done(); }, (e) => { done(e); });
                }, false),
            
            new PluginTestingFramework.TestBuilderIt("localPackage.install.immediately",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

                    /* create an update */
                    PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateNotifyApplicationReady, "Update 1")
                        .then<void>((updatePath: string) => {
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.updatePackagePath = updatePath;
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                ServerUtil.TestMessage.CHECK_UPDATE_AVAILABLE,
                                ServerUtil.TestMessage.DOWNLOAD_SUCCEEDED,
                                ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
                            projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* run the app again to ensure it was not reverted */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .done(() => { done(); }, (e) => { done(e); });
                }, false)
        ], ScenarioInstall),
        
    new PluginTestingFramework.TestBuilderDescribe("#localPackage.install.revert",
    
        [
            new PluginTestingFramework.TestBuilderIt("localPackage.install.revert.dorevert",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

                    /* create an update */
                    PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateDeviceReady, "Update 1 (bad update)")
                        .then<void>((updatePath: string) => {
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.updatePackagePath = updatePath;
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                ServerUtil.TestMessage.CHECK_UPDATE_AVAILABLE,
                                ServerUtil.TestMessage.DOWNLOAD_SUCCEEDED,
                                ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
                            projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* restart the app to ensure it was reverted and send it another update */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                ServerUtil.TestMessage.CHECK_UPDATE_AVAILABLE,
                                ServerUtil.TestMessage.DOWNLOAD_SUCCEEDED,
                                ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* restart the app again to ensure it was reverted again and send the same update and expect it to reject it */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.UPDATE_FAILED_PREVIOUSLY], deferred);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .done(() => { done(); }, (e) => { done(e); });
                }, false),
            
            new PluginTestingFramework.TestBuilderIt("localPackage.install.revert.norevert",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

                    /* create an update */
                    PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateNotifyApplicationReady, "Update 1 (good update)")
                        .then<void>((updatePath: string) => {
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.updatePackagePath = updatePath;
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                ServerUtil.TestMessage.CHECK_UPDATE_AVAILABLE,
                                ServerUtil.TestMessage.DOWNLOAD_SUCCEEDED,
                                ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
                            projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* run the app again to ensure it was not reverted */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .done(() => { done(); }, (e) => { done(e); });
                }, false)
        ], ScenarioInstallWithRevert),
    
    new PluginTestingFramework.TestBuilderDescribe("#localPackage.installOnNextResume",
    
        [
            new PluginTestingFramework.TestBuilderIt("localPackage.installOnNextResume.dorevert",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

                    PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateDeviceReady, "Update 1")
                        .then<void>((updatePath: string) => {
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.updatePackagePath = updatePath;
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                ServerUtil.TestMessage.CHECK_UPDATE_AVAILABLE,
                                ServerUtil.TestMessage.DOWNLOAD_SUCCEEDED,
                                ServerUtil.TestMessage.UPDATE_INSTALLED], deferred);
                            projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* resume the application */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
                            projectManager.resumeApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* restart to revert it */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.UPDATE_FAILED_PREVIOUSLY], deferred);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .done(() => { done(); }, (e) => { done(e); });
                }, true),
            
            new PluginTestingFramework.TestBuilderIt("localPackage.installOnNextResume.norevert",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

                    /* create an update */
                    PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateNotifyApplicationReady, "Update 1 (good update)")
                        .then<void>((updatePath: string) => {
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.updatePackagePath = updatePath;
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                ServerUtil.TestMessage.CHECK_UPDATE_AVAILABLE,
                                ServerUtil.TestMessage.DOWNLOAD_SUCCEEDED,
                                ServerUtil.TestMessage.UPDATE_INSTALLED], deferred);
                            projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* resume the application */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
                            projectManager.resumeApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* restart to make sure it did not revert */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .done(() => { done(); }, (e) => { done(e); });
                }, true)
        ], ScenarioInstallOnResumeWithRevert),
        
    new PluginTestingFramework.TestBuilderDescribe("localPackage installOnNextRestart",
    
        [
            new PluginTestingFramework.TestBuilderIt("localPackage.installOnNextRestart.dorevert",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

                    PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateDeviceReady, "Update 1")
                        .then<void>((updatePath: string) => {
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.updatePackagePath = updatePath;
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                ServerUtil.TestMessage.CHECK_UPDATE_AVAILABLE,
                                ServerUtil.TestMessage.DOWNLOAD_SUCCEEDED,
                                ServerUtil.TestMessage.UPDATE_INSTALLED], deferred);
                            projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* restart the application */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
                            console.log("Update hash: " + PluginTestingFramework.updateResponse.updateInfo.packageHash);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* restart the application */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.UPDATE_FAILED_PREVIOUSLY], deferred);
                            console.log("Update hash: " + PluginTestingFramework.updateResponse.updateInfo.packageHash);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .done(() => { done(); }, (e) => { done(e); });
                }, false),
            
            new PluginTestingFramework.TestBuilderIt("localPackage.installOnNextRestart.norevert",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

                    /* create an update */
                    PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateNotifyApplicationReady, "Update 1 (good update)")
                        .then<void>((updatePath: string) => {
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.updatePackagePath = updatePath;
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                ServerUtil.TestMessage.CHECK_UPDATE_AVAILABLE,
                                ServerUtil.TestMessage.DOWNLOAD_SUCCEEDED,
                                ServerUtil.TestMessage.UPDATE_INSTALLED], deferred);
                            projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* "resume" the application - run it again */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* run again to make sure it did not revert */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .done(() => { done(); }, (e) => { done(e); });
                }, true),
            
            new PluginTestingFramework.TestBuilderIt("localPackage.installOnNextRestart.revertToPrevious",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

                    /* create an update */
                    PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateNotifyApplicationReadyConditional, "Update 1 (good update)")
                        .then<void>((updatePath: string) => {
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.updatePackagePath = updatePath;
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                ServerUtil.TestMessage.CHECK_UPDATE_AVAILABLE,
                                ServerUtil.TestMessage.DOWNLOAD_SUCCEEDED,
                                ServerUtil.TestMessage.UPDATE_INSTALLED], deferred);
                            projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* run good update, set up another (bad) update */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE,
                                ServerUtil.TestMessage.CHECK_UPDATE_AVAILABLE,
                                ServerUtil.TestMessage.DOWNLOAD_SUCCEEDED,
                                ServerUtil.TestMessage.UPDATE_INSTALLED], deferred);
                            PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };
                            PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateDeviceReady, "Update 2 (bad update)")
                                .then(() => { return projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform); });
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* run the bad update without calling notifyApplicationReady */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* run the good update and don't call notifyApplicationReady - it should not revert */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageResponse = ServerUtil.TestMessageResponse.SKIP_NOTIFY_APPLICATION_READY;
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE, ServerUtil.TestMessage.SKIPPED_NOTIFY_APPLICATION_READY], deferred);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* run the application again */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageResponse = undefined;
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE, ServerUtil.TestMessage.UPDATE_FAILED_PREVIOUSLY], deferred);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .done(() => { done(); }, (e) => { done(e); });
                }, false)
        ], ScenarioInstallOnRestartWithRevert),
        
    new PluginTestingFramework.TestBuilderDescribe("#codePush.restartApplication",
    
        [
            new PluginTestingFramework.TestBuilderIt("codePush.restartApplication.checkPackages",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

                    PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateNotifyApplicationReady, "Update 1")
                        .then<void>((updatePath: string) => {
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.updatePackagePath = updatePath;
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                new ServerUtil.AppMessage(ServerUtil.TestMessage.PENDING_PACKAGE, [null]),
                                new ServerUtil.AppMessage(ServerUtil.TestMessage.CURRENT_PACKAGE, [null]),
                                new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UPDATE_INSTALLED]),
                                new ServerUtil.AppMessage(ServerUtil.TestMessage.PENDING_PACKAGE, [PluginTestingFramework.updateResponse.updateInfo.packageHash]),
                                new ServerUtil.AppMessage(ServerUtil.TestMessage.CURRENT_PACKAGE, [null]),
                                ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE
                            ], deferred);
                            projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* restart the application */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE
                            ], deferred);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .done(() => { done(); }, (e) => { done(e); });
                }, true)
        ], ScenarioRestart),
        
    new PluginTestingFramework.TestBuilderDescribe("#window.codePush.sync",
        [
            // We test the functionality with sync twice--first, with sync only called once,
            // then, with sync called again while the first sync is still running.
            new PluginTestingFramework.TestBuilderDescribe("#window.codePush.sync 1x",
                [
                    // Tests where sync is called just once
                    new PluginTestingFramework.TestBuilderIt("window.codePush.sync.noupdate",
                        (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                            var noUpdateResponse = PluginTestingFramework.createDefaultResponse();
                            noUpdateResponse.isAvailable = false;
                            noUpdateResponse.appVersion = "0.0.1";
                            PluginTestingFramework.updateResponse = { updateInfo: noUpdateResponse };

                            Q({})
                                .then<void>(p => {
                                    var deferred = Q.defer<void>();
                                    PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UP_TO_DATE])],
                                        deferred);
                                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform).done();
                                    return deferred.promise;
                                })
                                .done(() => { done(); }, (e) => { done(e); });
                        }, false),
                    
                    new PluginTestingFramework.TestBuilderIt("window.codePush.sync.checkerror",
                        (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                            PluginTestingFramework.updateResponse = "invalid {{ json";

                            Q({})
                                .then<void>(p => {
                                    var deferred = Q.defer<void>();
                                    PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_ERROR])],
                                        deferred);
                                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform).done();
                                    return deferred.promise;
                                })
                                .done(() => { done(); }, (e) => { done(e); });
                        }, false),
                    
                    new PluginTestingFramework.TestBuilderIt("window.codePush.sync.downloaderror",
                        (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                            var invalidUrlResponse = PluginTestingFramework.createMockResponse();
                            invalidUrlResponse.downloadURL = path.join(PluginTestingFramework.templatePath, "invalid_path.zip");
                            PluginTestingFramework.updateResponse = { updateInfo: invalidUrlResponse };

                            Q({})
                                .then<void>(p => {
                                    var deferred = Q.defer<void>();
                                    PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_ERROR])],
                                        deferred);
                                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform).done();
                                    return deferred.promise;
                                })
                                .done(() => { done(); }, (e) => { done(e); });
                        }, false),
                    
                    new PluginTestingFramework.TestBuilderIt("window.codePush.sync.dorevert",
                        (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                            PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };
                        
                            /* create an update */
                            PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateDeviceReady, "Update 1 (bad update)")
                                .then<void>((updatePath: string) => {
                                    var deferred = Q.defer<void>();
                                    PluginTestingFramework.updatePackagePath = updatePath;
                                    PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UPDATE_INSTALLED]),
                                        ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
                                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform).done();
                                    return deferred.promise;
                                })
                                .then<void>(() => {
                                    var deferred = Q.defer<void>();
                                    PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UP_TO_DATE])], deferred);
                                    projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform).done();
                                    return deferred.promise;
                                })
                                .done(() => { done(); }, (e) => { done(e); });
                        }, false),
                    
                    new PluginTestingFramework.TestBuilderIt("window.codePush.sync.update",
                        (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                            PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

                            /* create an update */
                            PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateSync, "Update 1 (good update)")
                                .then<void>((updatePath: string) => {
                                    var deferred = Q.defer<void>();
                                    PluginTestingFramework.updatePackagePath = updatePath;
                                    PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UPDATE_INSTALLED]),
                                        // the update is immediate so the update will install
                                        ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE,
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UP_TO_DATE])], deferred);
                                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform).done();
                                    return deferred.promise;
                                })
                                .then<void>(() => {
                                    // restart the app and make sure it didn't roll out!
                                    var deferred = Q.defer<void>();
                                    var noUpdateResponse = PluginTestingFramework.createDefaultResponse();
                                    noUpdateResponse.isAvailable = false;
                                    noUpdateResponse.appVersion = "0.0.1";
                                    PluginTestingFramework.updateResponse = { updateInfo: noUpdateResponse };
                                    PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                        ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE,
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UP_TO_DATE])], deferred);
                                    projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform).done();
                                    return deferred.promise;
                                })
                                .done(() => { done(); }, (e) => { done(e); });
                        }, false)
                    
                ], ScenarioSync1x),
                
            new PluginTestingFramework.TestBuilderDescribe("#window.codePush.sync 2x",
                [
                    // Tests where sync is called again before the first sync finishes
                    new PluginTestingFramework.TestBuilderIt("window.codePush.sync.2x.noupdate",
                        (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                            var noUpdateResponse = PluginTestingFramework.createDefaultResponse();
                            noUpdateResponse.isAvailable = false;
                            noUpdateResponse.appVersion = "0.0.1";
                            PluginTestingFramework.updateResponse = { updateInfo: noUpdateResponse };

                            Q({})
                                .then<void>(p => {
                                    var deferred = Q.defer<void>();
                                    PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_IN_PROGRESS]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UP_TO_DATE])],
                                        deferred);
                                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform).done();
                                    return deferred.promise;
                                })
                                .done(() => { done(); }, (e) => { done(e); });
                        }, false),
                    
                    new PluginTestingFramework.TestBuilderIt("window.codePush.sync.2x.checkerror",
                        (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                            PluginTestingFramework.updateResponse = "invalid {{ json";

                            Q({})
                                .then<void>(p => {
                                    var deferred = Q.defer<void>();
                                    PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_IN_PROGRESS]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_ERROR])],
                                        deferred);
                                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform).done();
                                    return deferred.promise;
                                })
                                .done(() => { done(); }, (e) => { done(e); });
                        }, false),
                    
                    new PluginTestingFramework.TestBuilderIt("window.codePush.sync.2x.downloaderror",
                        (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                            var invalidUrlResponse = PluginTestingFramework.createMockResponse();
                            invalidUrlResponse.downloadURL = path.join(PluginTestingFramework.templatePath, "invalid_path.zip");
                            PluginTestingFramework.updateResponse = { updateInfo: invalidUrlResponse };

                            Q({})
                                .then<void>(p => {
                                    var deferred = Q.defer<void>();
                                    PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_IN_PROGRESS]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_ERROR])],
                                        deferred);
                                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform).done();
                                    return deferred.promise;
                                })
                                .done(() => { done(); }, (e) => { done(e); });
                        }, false),
                    
                    new PluginTestingFramework.TestBuilderIt("window.codePush.sync.2x.dorevert",
                        (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                            PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };
                    
                            /* create an update */
                            PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateDeviceReady, "Update 1 (bad update)")
                                .then<void>((updatePath: string) => {
                                    var deferred = Q.defer<void>();
                                    PluginTestingFramework.updatePackagePath = updatePath;
                                    PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_IN_PROGRESS]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UPDATE_INSTALLED]),
                                        ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE],
                                        deferred);
                                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform).done();
                                    return deferred.promise;
                                })
                                .then<void>(() => {
                                    var deferred = Q.defer<void>();
                                    PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_IN_PROGRESS]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UP_TO_DATE])],
                                        deferred);
                                    projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform).done();
                                    return deferred.promise;
                                })
                                .done(() => { done(); }, (e) => { done(e); });
                        }, false),
                    
                    new PluginTestingFramework.TestBuilderIt("window.codePush.sync.2x.update",
                        (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                            PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

                            /* create an update */
                            PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateSync2x, "Update 1 (good update)")
                                .then<void>((updatePath: string) => {
                                    var deferred = Q.defer<void>();
                                    PluginTestingFramework.updatePackagePath = updatePath;
                                    PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_IN_PROGRESS]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UPDATE_INSTALLED]),
                                        // the update is immediate so the update will install
                                        ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE,
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_IN_PROGRESS]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UP_TO_DATE])],
                                        deferred);
                                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform).done();
                                    return deferred.promise;
                                })
                                .then<void>(() => {
                                    // restart the app and make sure it didn't roll out!
                                    var deferred = Q.defer<void>();
                                    var noUpdateResponse = PluginTestingFramework.createDefaultResponse();
                                    noUpdateResponse.isAvailable = false;
                                    noUpdateResponse.appVersion = "0.0.1";
                                    PluginTestingFramework.updateResponse = { updateInfo: noUpdateResponse };
                                    PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                        ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE,
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_IN_PROGRESS]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UP_TO_DATE])],
                                        deferred);
                                    projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform).done();
                                    return deferred.promise;
                                })
                                .done(() => { done(); }, (e) => { done(e); });
                        }, true)
                ], ScenarioSync2x)
        ]),
    
    new PluginTestingFramework.TestBuilderDescribe("#window.codePush.sync minimum background duration tests",
    
        [
            new PluginTestingFramework.TestBuilderIt("defaults to no minimum",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

                    PluginTestingFramework.setupScenario(projectManager, targetPlatform, ScenarioSyncResume).then<string>(() => {
                            return PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateSync, "Update 1 (good update)");
                        })
                        .then<void>((updatePath: string) => {
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.updatePackagePath = updatePath;
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UPDATE_INSTALLED])], deferred);
                            projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform).done();
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            var deferred = Q.defer<void>();
                            var noUpdateResponse = PluginTestingFramework.createDefaultResponse();
                            noUpdateResponse.isAvailable = false;
                            noUpdateResponse.appVersion = "0.0.1";
                            PluginTestingFramework.updateResponse = { updateInfo: noUpdateResponse };
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE,
                                new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UP_TO_DATE])], deferred);
                            projectManager.resumeApplication(PluginTestingFramework.TestNamespace, targetPlatform).done();
                            return deferred.promise;
                        })
                        .done(() => { done(); }, (e) => { done(e); });
                }, false),
            
            new PluginTestingFramework.TestBuilderIt("min background duration 5s",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

                    PluginTestingFramework.setupScenario(projectManager, targetPlatform, ScenarioSyncResumeDelay).then<string>(() => {
                            return PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateSync, "Update 1 (good update)");
                        })
                        .then<void>((updatePath: string) => {
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.updatePackagePath = updatePath;
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UPDATE_INSTALLED])], deferred);
                            projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform).done();
                            return deferred.promise;
                        })
                        .then<string>(() => {
                            var noUpdateResponse = PluginTestingFramework.createDefaultResponse();
                            noUpdateResponse.isAvailable = false;
                            noUpdateResponse.appVersion = "0.0.1";
                            PluginTestingFramework.updateResponse = { updateInfo: noUpdateResponse };
                            return projectManager.resumeApplication(PluginTestingFramework.TestNamespace, targetPlatform, 3 * 1000);
                        })
                        .then<void>(() => {
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE,
                                new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UP_TO_DATE])], deferred);
                            projectManager.resumeApplication(PluginTestingFramework.TestNamespace, targetPlatform, 6 * 1000).done();
                            return deferred.promise;
                        })
                        .done(() => { done(); }, (e) => { done(e); });
                }, false),
                
            new PluginTestingFramework.TestBuilderIt("has no effect on restart",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

                    PluginTestingFramework.setupScenario(projectManager, targetPlatform, ScenarioSyncRestartDelay).then<string>(() => {
                            return PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateSync, "Update 1 (good update)");
                        })
                        .then<void>((updatePath: string) => {
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.updatePackagePath = updatePath;
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UPDATE_INSTALLED])], deferred);
                            projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform).done();
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            var deferred = Q.defer<void>();
                            var noUpdateResponse = PluginTestingFramework.createDefaultResponse();
                            noUpdateResponse.isAvailable = false;
                            noUpdateResponse.appVersion = "0.0.1";
                            PluginTestingFramework.updateResponse = { updateInfo: noUpdateResponse };
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE,
                                new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UP_TO_DATE])], deferred);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform).done();
                            return deferred.promise;
                        })
                        .done(() => { done(); }, (e) => { done(e); });
                }, false)
        ]),
        
    new PluginTestingFramework.TestBuilderDescribe("#window.codePush.sync mandatory install mode tests",
    
        [
            new PluginTestingFramework.TestBuilderIt("defaults to IMMEDIATE",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform, true) };

                    PluginTestingFramework.setupScenario(projectManager, targetPlatform, ScenarioSyncMandatoryDefault).then<string>(() => {
                            return PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateDeviceReady, "Update 1 (good update)");
                        })
                        .then<void>((updatePath: string) => {
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.updatePackagePath = updatePath;
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UPDATE_INSTALLED]),
                                ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
                            projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform).done();
                            return deferred.promise;
                        })
                        .done(() => { done(); }, (e) => { done(e); });
                }, false),
                
            new PluginTestingFramework.TestBuilderIt("works correctly when update is mandatory and mandatory install mode is specified",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform, true) };

                    PluginTestingFramework.setupScenario(projectManager, targetPlatform, ScenarioSyncMandatoryResume).then<string>(() => {
                            return PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateDeviceReady, "Update 1 (good update)");
                        })
                        .then<void>((updatePath: string) => {
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.updatePackagePath = updatePath;
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UPDATE_INSTALLED])], deferred);
                            projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform).done();
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            var deferred = Q.defer<void>();
                            var noUpdateResponse = PluginTestingFramework.createDefaultResponse();
                            noUpdateResponse.isAvailable = false;
                            noUpdateResponse.appVersion = "0.0.1";
                            PluginTestingFramework.updateResponse = { updateInfo: noUpdateResponse };
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
                            projectManager.resumeApplication(PluginTestingFramework.TestNamespace, targetPlatform, 5 * 1000).done();
                            return deferred.promise;
                        })
                        .done(() => { done(); }, (e) => { done(e); });
                }, false),
                
            new PluginTestingFramework.TestBuilderIt("has no effect on updates that are not mandatory",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

                    PluginTestingFramework.setupScenario(projectManager, targetPlatform, ScenarioSyncMandatoryRestart).then<string>(() => {
                            return PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateDeviceReady, "Update 1 (good update)");
                        })
                        .then<void>((updatePath: string) => {
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.updatePackagePath = updatePath;
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UPDATE_INSTALLED]),
                                ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
                            projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform).done();
                            return deferred.promise;
                        })
                        .done(() => { done(); }, (e) => { done(e); });
                }, false)
        ])
];

// Create tests.
PluginTestingFramework.initializeTests(new RNProjectManager(), testBuilderDescribes);