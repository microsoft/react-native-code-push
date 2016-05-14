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
	 * Creates a new test application at the specified path, and configures it
	 * with the given server URL, android and ios deployment keys.
	 */
    public setupProject(projectDirectory: string, templatePath: string, appName: string, appNamespace: string, version?: string): Q.Promise<string> {
        if (fs.existsSync(projectDirectory)) {
            del.sync([projectDirectory], { force: true });
        }
        mkdirp.sync(projectDirectory);
        
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

        // React-Native adds a "com." to the front of the name you provide, so provide the namespace with the "com." removed
        return ProjectManager.execChildProcess("react-native init " + appName + " --package " + appNamespace, { cwd: projectDirectory }, true)
            // Copy over the template
            // Overwrites existing files with the files that are provided in the template (MainActivity.java, AppDelegate.m)
            .then<void>(copyDirectoryRecursively.bind(this, templatePath, path.join(projectDirectory, PluginTestingFramework.TestAppName)))
            .then<string>(ProjectManager.execChildProcess.bind(undefined, "npm install " + PluginTestingFramework.thisPluginPath, { cwd: path.join(projectDirectory, PluginTestingFramework.TestAppName) }));
    }
    
    /**
     * Sets up the scenario for a test in an already existing project.
     */
    public setupScenario(projectDirectory: string, appId: string, templatePath: string, jsPath: string, targetPlatform: Platform.IPlatform, version?: string): Q.Promise<string> {
        var indexHtml = "index.js";
        var templateIndexPath = path.join(templatePath, indexHtml);
        var destinationIndexPath = path.join(projectDirectory, PluginTestingFramework.TestAppName, indexHtml);
        
        var scenarioJs = "scenarios/" + jsPath;
        
        // var packageFile = eval("(" + fs.readFileSync("./package.json", "utf8") + ")");
        // var pluginVersion = packageFile.version;
        
        console.log("Setting up scenario " + jsPath + " in " + projectDirectory);

        // Copy index html file and replace
        return ProjectManager.copyFile(templateIndexPath, destinationIndexPath, true)
            .then<void>(ProjectManager.replaceString.bind(undefined, destinationIndexPath, ProjectManager.SERVER_URL_PLACEHOLDER, targetPlatform.getServerUrl()))
            .then<void>(ProjectManager.replaceString.bind(undefined, destinationIndexPath, ProjectManager.INDEX_JS_PLACEHOLDER, scenarioJs))
            .then<void>(ProjectManager.replaceString.bind(undefined, destinationIndexPath, ProjectManager.CODE_PUSH_APP_VERSION_PLACEHOLDER, version))
            // Chain promise so that it maintains Q.Promise<string> type instead of Q.Promise<void>
            .then<string>(() => { return null; });
    }

    /**
     * Creates a CodePush update package zip for a project.
     */
    public createUpdateArchive(projectDirectory: string, targetPlatform: Platform.IPlatform, isDiff?: boolean): Q.Promise<string> {
        var bundleFolder: string = path.join(projectDirectory, PluginTestingFramework.TestAppName, "CodePush/");
        var bundlePath: string = path.join(bundleFolder, "./main.jsbundle");
        var deferred = Q.defer<string>();
        fs.exists(bundleFolder, (exists) => {
            if (exists) fs.mkdirSync(bundleFolder);
            deferred.resolve(undefined);
        });
        return deferred.promise
            .then(ProjectManager.execChildProcess.bind(undefined, "react-native bundle --platform " + targetPlatform.getName() + " --entry-file index." + targetPlatform.getName() + ".js --bundle-output " + bundlePath + " --assets-dest " + bundleFolder + " --dev false",
                { cwd: path.join(projectDirectory, PluginTestingFramework.TestAppName) }))
            .then<string>(ProjectManager.archiveFolder.bind(undefined, bundleFolder, path.join(projectDirectory, PluginTestingFramework.TestAppName, "update.zip"), isDiff));
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
            if (platformJSON[targetPlatform.getName()] === true) deferred.reject("Platform " + targetPlatform.getName() + " is already installed!");
            else {
                platformJSON[targetPlatform.getName()] = true;
                fs.writeFileSync(platformsJSONPath, JSON.stringify(platformJSON));
                deferred.resolve(undefined);
            }
        });
        
        var innerProjectFolder: string = path.join(projectFolder, PluginTestingFramework.TestAppName);
        
        // Install the CodePush plugin for the platform.
        return deferred.promise
            .then<string>(() => {
                if (targetPlatform === Platform.Android.getInstance()) {
                    // Add to android/app/build.gradle
                    var buildGradle = path.join(innerProjectFolder, "android", "app", "build.gradle");
                    ProjectManager.replaceString(buildGradle,
                        "apply from: \"../../node_modules/react-native/react.gradle\"",
                        "apply from: \"../../node_modules/react-native/react.gradle\"\napply from: \"" + path.join(innerProjectFolder, "node_modules", "react-native-code-push", "android", "codepush.gradle") + "\"");
                    ProjectManager.replaceString(buildGradle,
                        "// From node_modules",
                        "\n    compile project(':react-native-code-push') // From node_modules");
                    // Add to android/settings.gradle
                    ProjectManager.replaceString(path.join(innerProjectFolder, "android", "settings.gradle"),
                        "include ':app'",
                        "include ':app', ':react-native-code-push'\nproject(':react-native-code-push').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-code-push/android/app')");
                    // Replace the MainActivity.java with the correct server url and deployment key
                    var mainActivity = path.join(innerProjectFolder, "android", "app", "src", "main", "java", "com", "microsoft", "codepush", "test", "MainActivity.java");
                    ProjectManager.replaceString(mainActivity, ProjectManager.SERVER_URL_PLACEHOLDER, Platform.Android.getInstance().getServerUrl());
                    ProjectManager.replaceString(mainActivity, ProjectManager.ANDROID_KEY_PLACEHOLDER, Platform.Android.getInstance().getDefaultDeploymentKey());
                } else if (targetPlatform === Platform.IOS.getInstance()) {
                    var iOSProject: string = path.join(innerProjectFolder, "iOS");
                    // Create and install the Podfile
                    return ProjectManager.execChildProcess("pod init", { cwd: iOSProject })
                        .then(ProjectManager.replaceString.bind(undefined, path.join(iOSProject, "Podfile"), "# use_frameworks!",
                            "use_frameworks!\n  pod 'React', :path => '../node_modules/react-native', :subspecs => [ 'Core', 'RCTImage', 'RCTNetwork', 'RCTText', 'RCTWebSocket', ]\n  pod 'CodePush', :path => '../node_modules/react-native-code-push'"))
                        // Put the IOS deployment key in the Info.plist
                        .then(ProjectManager.replaceString.bind(undefined, path.join(iOSProject, PluginTestingFramework.TestAppName, "Info.plist"),
                            "</dict>\n</plist>",
                            "<key>CodePushDeploymentKey</key>\n\t<string>" + Platform.IOS.getInstance().getDefaultDeploymentKey() + "</string>\n\t<key>CodePushServerURL</key>\n\t<string>" + Platform.IOS.getInstance().getServerUrl() + "</string>\n\t</dict>\n</plist>"))
                        // Install the Pod
                        .then(ProjectManager.execChildProcess.bind(undefined, "pod install", { cwd: iOSProject }));
                }
            }, (error) => { /* The platform is already installed! */ console.log(error); return null; });
    }
    
    /**
     * Cleans up a specific platform after tests.
     */
    public cleanupAfterPlatform(projectFolder: string, targetPlatform: Platform.IPlatform): Q.Promise<string> {
        // Can't uninstall from command line, so noop.
        return null;
    }

    /**
     * Runs the test app on the given target / platform.
     */
    public runPlatform(projectFolder: string, targetPlatform: Platform.IPlatform): Q.Promise<string> {
        console.log("Running project in " + projectFolder + " on " + targetPlatform.getName());
        // Don't log the build output because iOS's build output is too verbose and overflows the buffer!
        return ProjectManager.execChildProcess("react-native run-" + targetPlatform.getName(), { cwd: path.join(projectFolder, PluginTestingFramework.TestAppName) });
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
                            assert.equal(ServerUtil.TestMessage.CHECK_UP_TO_DATE, requestBody.message);
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
                                assert.equal(ServerUtil.TestMessage.CHECK_UP_TO_DATE, requestBody.message);
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
                            assert.equal(ServerUtil.TestMessage.CHECK_UP_TO_DATE, requestBody.message);
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
                            assert.equal(ServerUtil.TestMessage.CHECK_UPDATE_AVAILABLE, requestBody.message);
                            assert.notEqual(null, requestBody.args[0]);
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
                            assert.equal(ServerUtil.TestMessage.CHECK_ERROR, requestBody.message);
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

                    /* pass the path to any file for download (here, config.xml) to make sure the download completed callback is invoked */
                    PluginTestingFramework.updatePackagePath = path.join(PluginTestingFramework.templatePath, "config.xml");

                    PluginTestingFramework.testMessageCallback = (requestBody: any) => {
                        try {
                            assert.equal(ServerUtil.TestMessage.DOWNLOAD_SUCCEEDED, requestBody.message);
                            done();
                        } catch (e) {
                            done(e);
                        }
                    };

                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                }, false),
            
            new PluginTestingFramework.TestBuilderIt("remotePackage.download.error",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

                    /* pass an invalid path */
                    PluginTestingFramework.updatePackagePath = path.join(PluginTestingFramework.templatePath, "invalid_path.zip");

                    PluginTestingFramework.testMessageCallback = (requestBody: any) => {
                        try {
                            assert.equal(ServerUtil.TestMessage.DOWNLOAD_ERROR, requestBody.message);
                            done();
                        } catch (e) {
                            done(e);
                        }
                    };

                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                }, false)
        ], ScenarioDownloadUpdate),
        
    new PluginTestingFramework.TestBuilderDescribe("#localPackage.install",
    
        [
            new PluginTestingFramework.TestBuilderIt("localPackage.install.unzip.error",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

                    /* pass an invalid zip file, here, config.xml */
                    PluginTestingFramework.updatePackagePath = path.join(PluginTestingFramework.templatePath, "config.xml");

                    PluginTestingFramework.testMessageCallback = (requestBody: any) => {
                        try {
                            assert.equal(ServerUtil.TestMessage.INSTALL_ERROR, requestBody.message);
                            done();
                        } catch (e) {
                            done(e);
                        }
                    };

                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                }, false),
            
            new PluginTestingFramework.TestBuilderIt("localPackage.install.handlesDiff.againstBinary",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

                    /* create an update */
                    PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateNotifyApplicationReady, "Diff Update 1")
                        .then<void>((updatePath: string) => {
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.updatePackagePath = updatePath;
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.UPDATE_INSTALLED, ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE, ServerUtil.TestMessage.NOTIFY_APP_READY_SUCCESS], deferred);
                            projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* run the app again to ensure it was not reverted */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE, ServerUtil.TestMessage.NOTIFY_APP_READY_SUCCESS], deferred);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .done(() => { done(); }, () => { done(); });
                }, false),
            
            new PluginTestingFramework.TestBuilderIt("localPackage.install.immediately",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

                    /* create an update */
                    PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateNotifyApplicationReady, "Update 1")
                        .then<void>((updatePath: string) => {
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.updatePackagePath = updatePath;
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.UPDATE_INSTALLED, ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE, ServerUtil.TestMessage.NOTIFY_APP_READY_SUCCESS], deferred);
                            projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* run the app again to ensure it was not reverted */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE, ServerUtil.TestMessage.NOTIFY_APP_READY_SUCCESS], deferred);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .done(() => { done(); }, () => { done(); });
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
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.UPDATE_INSTALLED, ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
                            projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* run the app again to ensure it was reverted */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.UPDATE_FAILED_PREVIOUSLY], deferred);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* create a second failed update */
                            console.log("Creating a second failed update.");
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.UPDATE_INSTALLED, ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* run the app again to ensure it was reverted */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.UPDATE_FAILED_PREVIOUSLY], deferred);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .done(() => { done(); }, () => { done(); });
                }, false),
            
            new PluginTestingFramework.TestBuilderIt("localPackage.install.revert.norevert",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

                    /* create an update */
                    PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateNotifyApplicationReady, "Update 1 (good update)")
                        .then<void>((updatePath: string) => {
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.updatePackagePath = updatePath;
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.UPDATE_INSTALLED, ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE, ServerUtil.TestMessage.NOTIFY_APP_READY_SUCCESS], deferred);
                            projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* run the app again to ensure it was not reverted */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE, ServerUtil.TestMessage.NOTIFY_APP_READY_SUCCESS], deferred);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .done(() => { done(); }, () => { done(); });
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
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.UPDATE_INSTALLED], deferred);
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
                        .done(() => { done(); }, () => { done(); });
                }, true),
            
            new PluginTestingFramework.TestBuilderIt("localPackage.installOnNextResume.norevert",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

                    /* create an update */
                    PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateNotifyApplicationReady, "Update 1 (good update)")
                        .then<void>((updatePath: string) => {
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.updatePackagePath = updatePath;
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.UPDATE_INSTALLED], deferred);
                            projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* resume the application */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE, ServerUtil.TestMessage.NOTIFY_APP_READY_SUCCESS], deferred);
                            projectManager.resumeApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* restart to make sure it did not revert */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE, ServerUtil.TestMessage.NOTIFY_APP_READY_SUCCESS], deferred);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .done(() => { done(); }, () => { done(); });
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
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.UPDATE_INSTALLED], deferred);
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
                        .done(() => { done(); }, () => { done(); });
                }, false),
            
            new PluginTestingFramework.TestBuilderIt("localPackage.installOnNextRestart.norevert",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

                    /* create an update */
                    PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateNotifyApplicationReady, "Update 1 (good update)")
                        .then<void>((updatePath: string) => {
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.updatePackagePath = updatePath;
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.UPDATE_INSTALLED], deferred);
                            projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* "resume" the application - run it again */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE, ServerUtil.TestMessage.NOTIFY_APP_READY_SUCCESS], deferred);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* run again to make sure it did not revert */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE, ServerUtil.TestMessage.NOTIFY_APP_READY_SUCCESS], deferred);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .done(() => { done(); }, () => { done(); });
                }, true),
            
            new PluginTestingFramework.TestBuilderIt("localPackage.installOnNextRestart.revertToPrevious",
                (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                    PluginTestingFramework.updateResponse = { updateInfo: PluginTestingFramework.getMockResponse(targetPlatform) };

                    /* create an update */
                    PluginTestingFramework.createUpdate(projectManager, targetPlatform, UpdateNotifyApplicationReadyConditional, "Update 1 (good update)")
                        .then<void>((updatePath: string) => {
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.updatePackagePath = updatePath;
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.UPDATE_INSTALLED], deferred);
                            projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* run good update, set up another (bad) update */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE, ServerUtil.TestMessage.NOTIFY_APP_READY_SUCCESS, ServerUtil.TestMessage.UPDATE_INSTALLED], deferred);
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
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE, ServerUtil.TestMessage.NOTIFY_APP_READY_SUCCESS, ServerUtil.TestMessage.UPDATE_FAILED_PREVIOUSLY], deferred);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .done(() => { done(); }, () => { done(); });
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
                                new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_CHECKING_FOR_UPDATE]),
                                new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_DOWNLOADING_PACKAGE]),
                                new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_INSTALLING_UPDATE]),
                                new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UPDATE_INSTALLED]),
                                new ServerUtil.AppMessage(ServerUtil.TestMessage.PENDING_PACKAGE, [PluginTestingFramework.updateResponse.updateInfo.packageHash]),
                                new ServerUtil.AppMessage(ServerUtil.TestMessage.CURRENT_PACKAGE, [null]),
                                ServerUtil.TestMessage.RESTART_SUCCEEDED,
                                ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE,
                                ServerUtil.TestMessage.NOTIFY_APP_READY_SUCCESS
                            ], deferred);
                            projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform);
                            return deferred.promise;
                        })
                        .then<void>(() => {
                            /* restart the application */
                            var deferred = Q.defer<void>();
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE, ServerUtil.TestMessage.NOTIFY_APP_READY_SUCCESS
                            ], deferred);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform);
                            return deferred.promise;
                        })
                        .done(() => { done(); }, () => { done(); });
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
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_CHECKING_FOR_UPDATE]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UP_TO_DATE])],
                                        deferred);
                                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform).done();
                                    return deferred.promise;
                                })
                                .done(() => { done(); }, () => { done(); });
                        }, false),
                    
                    new PluginTestingFramework.TestBuilderIt("window.codePush.sync.checkerror",
                        (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                            PluginTestingFramework.updateResponse = "invalid {{ json";

                            Q({})
                                .then<void>(p => {
                                    var deferred = Q.defer<void>();
                                    PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_CHECKING_FOR_UPDATE]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_ERROR])],
                                        deferred);
                                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform).done();
                                    return deferred.promise;
                                })
                                .done(() => { done(); }, () => { done(); });
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
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_CHECKING_FOR_UPDATE]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_DOWNLOADING_PACKAGE]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_ERROR])],
                                        deferred);
                                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform).done();
                                    return deferred.promise;
                                })
                                .done(() => { done(); }, () => { done(); });
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
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_CHECKING_FOR_UPDATE]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_DOWNLOADING_PACKAGE]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_INSTALLING_UPDATE]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UPDATE_INSTALLED]),
                                        ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
                                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform).done();
                                    return deferred.promise;
                                })
                                .then<void>(() => {
                                    var deferred = Q.defer<void>();
                                    PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_CHECKING_FOR_UPDATE]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UP_TO_DATE])], deferred);
                                    projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform).done();
                                    return deferred.promise;
                                })
                                .done(() => { done(); }, () => { done(); });
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
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_CHECKING_FOR_UPDATE]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_DOWNLOADING_PACKAGE]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_INSTALLING_UPDATE]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UPDATE_INSTALLED]),
                                        // the update is immediate so the update will install
                                        ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
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
                                    PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
                                    projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform).done();
                                    return deferred.promise;
                                })
                                .done(() => { done(); }, () => { done(); });
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
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_CHECKING_FOR_UPDATE]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_IN_PROGRESS]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UP_TO_DATE])],
                                        deferred);
                                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform).done();
                                    return deferred.promise;
                                })
                                .done(() => { done(); }, () => { done(); });
                        }, false),
                    
                    new PluginTestingFramework.TestBuilderIt("window.codePush.sync.2x.checkerror",
                        (projectManager: ProjectManager, targetPlatform: Platform.IPlatform, done: MochaDone) => {
                            PluginTestingFramework.updateResponse = "invalid {{ json";

                            Q({})
                                .then<void>(p => {
                                    var deferred = Q.defer<void>();
                                    PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_CHECKING_FOR_UPDATE]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_IN_PROGRESS]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_ERROR])],
                                        deferred);
                                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform).done();
                                    return deferred.promise;
                                })
                                .done(() => { done(); }, () => { done(); });
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
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_CHECKING_FOR_UPDATE]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_IN_PROGRESS]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_DOWNLOADING_PACKAGE]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_ERROR])],
                                        deferred);
                                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform).done();
                                    return deferred.promise;
                                })
                                .done(() => { done(); }, () => { done(); });
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
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_CHECKING_FOR_UPDATE]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_IN_PROGRESS]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_DOWNLOADING_PACKAGE]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_INSTALLING_UPDATE]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UPDATE_INSTALLED]),
                                        ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE],
                                        deferred);
                                    projectManager.runPlatform(PluginTestingFramework.testRunDirectory, targetPlatform).done();
                                    return deferred.promise;
                                })
                                .then<void>(() => {
                                    var deferred = Q.defer<void>();
                                    PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_CHECKING_FOR_UPDATE]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_IN_PROGRESS]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UP_TO_DATE])],
                                        deferred);
                                    projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform).done();
                                    return deferred.promise;
                                })
                                .done(() => { done(); }, () => { done(); });
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
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_CHECKING_FOR_UPDATE]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_IN_PROGRESS]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_DOWNLOADING_PACKAGE]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_INSTALLING_UPDATE]),
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_UPDATE_INSTALLED]),
                                        // the update is immediate so the update will install
                                        ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE,
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_IN_PROGRESS])],
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
                                        new ServerUtil.AppMessage(ServerUtil.TestMessage.SYNC_STATUS, [ServerUtil.TestMessage.SYNC_IN_PROGRESS])],
                                        deferred);
                                    projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform).done();
                                    return deferred.promise;
                                })
                                .done(() => { done(); }, () => { done(); });
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
                                ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
                            projectManager.resumeApplication(PluginTestingFramework.TestNamespace, targetPlatform).done();
                            return deferred.promise;
                        })
                        .done(() => { done(); }, () => { done(); });
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
                                ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
                            projectManager.resumeApplication(PluginTestingFramework.TestNamespace, targetPlatform, 6 * 1000).done();
                            return deferred.promise;
                        })
                        .done(() => { done(); }, () => { done(); });
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
                            PluginTestingFramework.testMessageCallback = PluginTestingFramework.verifyMessages([ServerUtil.TestMessage.DEVICE_READY_AFTER_UPDATE], deferred);
                            projectManager.restartApplication(PluginTestingFramework.TestNamespace, targetPlatform).done();
                            return deferred.promise;
                        })
                        .done(() => { done(); }, () => { done(); });
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
                        .done(() => { done(); }, () => { done(); });
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
                        .done(() => { done(); }, () => { done(); });
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
                        .done(() => { done(); }, () => { done(); });
                }, false)
        ])
];

// Create tests.
PluginTestingFramework.initializeTests(new RNProjectManager(), testBuilderDescribes);