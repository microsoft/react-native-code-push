"use strict";
var Q = require("q");
var testUtil_1 = require("./testUtil");
//////////////////////////////////////////////////////////////////////////////////////////
// PLATFORMS
/**
 * Android implementations of IPlatform.
 */
var Android = (function () {
    function Android(emulatorManager) {
        this.emulatorManager = emulatorManager;
    }
    /**
     * Gets the platform name. (e.g. "android" for the Android platform).
     */
    Android.prototype.getName = function () {
        return "android";
    };
    /**
     * The command line flag used to determine whether or not this platform should run.
     * Runs when the flag is present, doesn't run otherwise.
     */
    Android.prototype.getCommandLineFlagName = function () {
        return "--android";
    };
    /**
     * Gets the server url used for testing.
     */
    Android.prototype.getServerUrl = function () {
        if (!this.serverUrl)
            this.serverUrl = process.env.ANDROID_SERVER ? process.env.ANDROID_SERVER : Android.DEFAULT_ANDROID_SERVER_URL;
        return this.serverUrl;
    };
    /**
     * Gets an IEmulatorManager that is used to control the emulator during the tests.
     */
    Android.prototype.getEmulatorManager = function () {
        return this.emulatorManager;
    };
    /**
     * Gets the default deployment key.
     */
    Android.prototype.getDefaultDeploymentKey = function () {
        return "mock-android-deployment-key";
    };
    Android.DEFAULT_ANDROID_SERVER_URL = "http://10.0.2.2:3001";
    return Android;
}());
exports.Android = Android;
/**
 * IOS implementation of IPlatform.
 */
var IOS = (function () {
    function IOS(emulatorManager) {
        this.emulatorManager = emulatorManager;
    }
    /**
     * Gets the platform name. (e.g. "android" for the Android platform).
     */
    IOS.prototype.getName = function () {
        return "ios";
    };
    /**
     * The command line flag used to determine whether or not this platform should run.
     * Runs when the flag is present, doesn't run otherwise.
     */
    IOS.prototype.getCommandLineFlagName = function () {
        return "--ios";
    };
    /**
     * Gets the server url used for testing.
     */
    IOS.prototype.getServerUrl = function () {
        if (!this.serverUrl)
            this.serverUrl = process.env.IOS_SERVER ? process.env.IOS_SERVER : IOS.DEFAULT_IOS_SERVER_URL;

        return this.serverUrl;
    };
    /**
     * Gets an IEmulatorManager that is used to control the emulator during the tests.
     */
    IOS.prototype.getEmulatorManager = function () {
        return this.emulatorManager;
    };
    /**
     * Gets the default deployment key.
     */
    IOS.prototype.getDefaultDeploymentKey = function () {
        return "mock-ios-deployment-key";
    };
    IOS.DEFAULT_IOS_SERVER_URL = "http://127.0.0.1:3000";
    return IOS;
}());
exports.IOS = IOS;
//////////////////////////////////////////////////////////////////////////////////////////
// EMULATOR MANAGERS
// bootEmulatorInternal constants
var emulatorMaxReadyAttempts = 50;
var emulatorReadyCheckDelayMs = 5 * 1000;
/**
 * Helper function for EmulatorManager implementations to use to boot an emulator with a given platformName and check, start, and kill methods.
 */
function bootEmulatorInternal(platformName, restartEmulators, targetEmulator, checkEmulator, startEmulator, killEmulator) {
    var deferred = Q.defer();
    console.log("Setting up " + platformName + " emulator.");
    function onEmulatorReady() {
        console.log(platformName + " emulator is ready!");
        deferred.resolve(undefined);
        return deferred.promise;
    }
    // Called to check if the emulator for the platform is initialized.
    function checkEmulatorReady() {
        var checkDeferred = Q.defer();
        console.log("Checking if " + platformName + " emulator is ready yet...");
        // Dummy command that succeeds if emulator is ready and fails otherwise.
        checkEmulator(targetEmulator)
            .then(function () {
                checkDeferred.resolve(undefined);
            }, function (error) {
                console.info(error);
                console.log(platformName + " emulator is not ready yet!");
                checkDeferred.reject(error);
            });
        return checkDeferred.promise;
    }
    var emulatorReadyAttempts = 0;
    // Loops checks to see if the emulator is ready and eventually fails after surpassing emulatorMaxReadyAttempts.
    function checkEmulatorReadyLooper() {
        var looperDeferred = Q.defer();
        emulatorReadyAttempts++;
        if (emulatorReadyAttempts > emulatorMaxReadyAttempts) {
            console.log(platformName + " emulator is not ready after " + emulatorMaxReadyAttempts + " attempts, abort.");
            deferred.reject(platformName + " emulator failed to boot.");
            looperDeferred.resolve(undefined);
        }
        setTimeout(function () {
            checkEmulatorReady()
                .then(function () {
                    looperDeferred.resolve(undefined);
                    onEmulatorReady();
                }, function () {
                    return checkEmulatorReadyLooper().then(function () { looperDeferred.resolve(undefined); }, function () { looperDeferred.reject(undefined); });
                });
        }, emulatorReadyCheckDelayMs);
        return looperDeferred.promise;
    }
    // Starts and loops the emulator.
    function startEmulatorAndLoop() {
        console.log("Booting " + platformName + " emulator named " + targetEmulator + ".");
        startEmulator(targetEmulator).catch(function (error) { console.log(error); deferred.reject(error); });
        return checkEmulatorReadyLooper();
    }
    var promise;
    if (restartEmulators) {
        console.log("Killing " + platformName + " emulator.");
        promise = killEmulator().catch(function () { return null; }).then(startEmulatorAndLoop);
    }
    else {
        promise = checkEmulatorReady().then(onEmulatorReady, startEmulatorAndLoop);
    }
    return deferred.promise;
}
var AndroidEmulatorManager = (function () {
    function AndroidEmulatorManager() {
    }
    /**
     * Returns the target emulator, which is specified through the command line.
     */
    AndroidEmulatorManager.prototype.getTargetEmulator = function () {
        let _this = this;
        if (this.targetEmulator)
            return Q(this.targetEmulator);
        else {
            const deferred = Q.defer();
            const targetAndroidEmulator = process.env.ANDROID_EMU;
            if (!targetAndroidEmulator) {
                // If no Android simulator is specified, get the most recent Android simulator to run tests on.
                testUtil_1.TestUtil.getProcessOutput("emulator -list-avds", { noLogCommand: true, noLogStdOut: true, noLogStdErr: true })
                    .then((Devices) => {
                        const listOfDevices = Devices.trim().split("\n");
                        deferred.resolve(listOfDevices[listOfDevices.length - 1]);
                    }, (error) => {
                        deferred.reject(error);
                    });
            }
            else {
                // Use the simulator specified on the command line.
                deferred.resolve(targetAndroidEmulator);
            }
            return deferred.promise
                .then((targetEmulator) => {
                    _this.targetEmulator = targetEmulator;
                    console.log("Using Android simulator named " + _this.targetEmulator);
                    return _this.targetEmulator;
                });
        }
    };
    /**
     * Boots the target emulator.
     */
    AndroidEmulatorManager.prototype.bootEmulator = function (restartEmulators) {
        function checkAndroidEmulator(androidEmulatorName) {
            // A command that does nothing but only succeeds if the emulator is running.
            // List all of the packages on the device.
            return testUtil_1.TestUtil.getProcessOutput("adb shell pm list packages", { noLogCommand: true, noLogStdOut: true, noLogStdErr: true }).then(function () { return null; });
        }
        function startAndroidEmulator(androidEmulatorName) {
            const androidEmulatorCommand = `emulator @${androidEmulatorName}`;
            let osSpecificCommand = "";
            if (process.platform === "darwin") {
                osSpecificCommand = `${androidEmulatorCommand} &`;
            } else {
                osSpecificCommand = `START /B ${androidEmulatorCommand}`;
            }
            return testUtil_1.TestUtil.getProcessOutput(osSpecificCommand, { noLogStdErr: true, timeout: 5000 });
        }
        function killAndroidEmulator() {
            return testUtil_1.TestUtil.getProcessOutput("adb emu kill").then(function () { return null; });
        }
        return this.getTargetEmulator()
            .then(function (targetEmulator) {
                return bootEmulatorInternal("Android", restartEmulators, targetEmulator, checkAndroidEmulator, startAndroidEmulator, killAndroidEmulator);
            });
    };
    /**
     * Launches an already installed application by app id.
     */
    AndroidEmulatorManager.prototype.launchInstalledApplication = function (appId) {
        return testUtil_1.TestUtil.getProcessOutput("adb shell monkey -p " + appId + " -c android.intent.category.LAUNCHER 1").then(function () { return null; });
    };
    /**
     * Ends a running application given its app id.
     */
    AndroidEmulatorManager.prototype.endRunningApplication = function (appId) {
        return testUtil_1.TestUtil.getProcessOutput("adb shell am force-stop " + appId).then(function () { return Q.delay(10000); });
    };
    /**
     * Restarts an already installed application by app id.
     */
    AndroidEmulatorManager.prototype.restartApplication = function (appId) {
        var _this = this;
        return this.endRunningApplication(appId)
            .then(function () {
                // Wait for a 1 second before restarting.
                return Q.delay(1000);
            })
            .then(function () {
                return _this.launchInstalledApplication(appId);
            });
    };
    /**
     * Navigates away from the current app, waits for a delay (defaults to 1 second), then navigates to the specified app.
     */
    AndroidEmulatorManager.prototype.resumeApplication = function (appId, delayBeforeResumingMs) {
        var _this = this;
        if (delayBeforeResumingMs === void 0) { delayBeforeResumingMs = 1000; }
        // Open a default Android app (for example, settings).
        return this.launchInstalledApplication("com.android.settings")
            .then(function () {
                console.log("Waiting for " + delayBeforeResumingMs + "ms before resuming the test application.");
                return Q.delay(delayBeforeResumingMs);
            })
            .then(function () {
                // Reopen the app.
                return _this.launchInstalledApplication(appId);
            });
    };
    /**
     * Prepares the emulator for a test.
     */
    AndroidEmulatorManager.prototype.prepareEmulatorForTest = function (appId) {
        return this.endRunningApplication(appId)
            .then(function () {
                return commandWithCheckAppExistence("adb shell pm clear", appId);
            });
    };
    /**
     * Uninstalls the app from the emulator.
     */
    AndroidEmulatorManager.prototype.uninstallApplication = function (appId) {
        return commandWithCheckAppExistence("adb uninstall", appId);
    };
    return AndroidEmulatorManager;
}());
exports.AndroidEmulatorManager = AndroidEmulatorManager;
var IOSEmulatorManager = (function () {
    function IOSEmulatorManager() {
    }
    /**
     * Returns the target emulator, which is specified through the command line.
     */
    IOSEmulatorManager.prototype.getTargetEmulator = function () {
        let _this = this;
        if (this.targetEmulator)
            return Q(this.targetEmulator);
        else {
            let deferred = Q.defer();
            let targetIOSEmulator = process.env.IOS_EMU;
            if (!targetIOSEmulator) {
                // If no iOS simulator is specified, get the most recent iOS simulator to run tests on.
                testUtil_1.TestUtil.getProcessOutput("xcrun simctl list", { noLogCommand: true, noLogStdOut: true, noLogStdErr: true })
                    .then((listOfDevicesWithDevicePairs) => {
                        let listOfDevices = listOfDevicesWithDevicePairs.slice(listOfDevicesWithDevicePairs.indexOf("-- iOS"), listOfDevicesWithDevicePairs.indexOf("-- tvOS"));
                        let phoneDevice = /iPhone\ \S*\ ?.*?\(([0-9A-Z-]*)\)/g;
                        let match = phoneDevice.exec(listOfDevices);
                        deferred.resolve(match[1]);
                    }, (error) => {
                        deferred.reject(error);
                    });
            }
            else {
                // Use the simulator specified on the command line.
                deferred.resolve(targetIOSEmulator);
            }
            return deferred.promise
                .then((targetEmulator) => {
                    _this.targetEmulator = targetEmulator;
                    console.log("Using iOS simulator named " + _this.targetEmulator);
                    return _this.targetEmulator;
                });
        }
    };
    /**
     * Boots the target emulator.
     */
    IOSEmulatorManager.prototype.bootEmulator = function (restartEmulators) {
        function checkIOSEmulator(iOSEmulatorId) {
            // A command that does nothing but only succeeds if the emulator is running.
            return testUtil_1.TestUtil.getProcessOutput("xcrun simctl getenv booted SIMULATOR_UDID", { noLogCommand: true, noLogStdOut: true, noLogStdErr: true }).then(function (simUdid) {
                return simUdid.trim() == iOSEmulatorId.trim() ? true : Promise.reject(new Error('Waiting for device to boot')); 
            });
        }
        function startIOSEmulator(iOSEmulatorId) {
            return testUtil_1.TestUtil.getProcessOutput("xcrun simctl boot " + iOSEmulatorId, { noLogStdErr: true })
                .catch(function (error) { return undefined; /* Always fails because we do not specify a template, which is not necessary to just start the emulator */ }).then(function () { return null; });
        }
        function killIOSEmulator() {
            return testUtil_1.TestUtil.getProcessOutput("xcrun simctl shutdown all").then(function () { return null; });
        }
        return this.getTargetEmulator()
            .then(function (targetEmulator) {
                return bootEmulatorInternal("iOS", restartEmulators, targetEmulator, checkIOSEmulator, startIOSEmulator, killIOSEmulator);
            });
    };
    /**
     * Launches an already installed application by app id.
     */
    IOSEmulatorManager.prototype.launchInstalledApplication = function (appId) {
        return testUtil_1.TestUtil.getProcessOutput("xcrun simctl launch booted " + appId, undefined).then(function () { return null; });
    };
    /**
     * Ends a running application given its app id.
     */
    IOSEmulatorManager.prototype.endRunningApplication = function (appId) {
        return testUtil_1.TestUtil.getProcessOutput("xcrun simctl terminate booted " + appId, undefined).then(function () { return null; })
    };
    /**
     * Restarts an already installed application by app id.
     */
    IOSEmulatorManager.prototype.restartApplication = function (appId) {
        var _this = this;
        return this.endRunningApplication(appId)
            .then(function () {
                // Wait for a second before restarting.
                return Q.delay(1000);
            })
            .then(function () { return _this.launchInstalledApplication(appId); });
    };
    /**
     * Navigates away from the current app, waits for a delay (defaults to 1 second), then navigates to the specified app.
     */
    IOSEmulatorManager.prototype.resumeApplication = function (appId, delayBeforeResumingMs) {
        var _this = this;
        if (delayBeforeResumingMs === void 0) { delayBeforeResumingMs = 1000; }
        // Open a default iOS app (for example, settings).
        return this.launchInstalledApplication("com.apple.Preferences")
            .then(function () {
                console.log("Waiting for " + delayBeforeResumingMs + "ms before resuming the test application.");
                return Q.delay(delayBeforeResumingMs);
            })
            .then(function () {
                // Reopen the app.
                return _this.launchInstalledApplication(appId);
            });
    };
    /**
     * Prepares the emulator for a test.
     */
    IOSEmulatorManager.prototype.prepareEmulatorForTest = function (appId) {
        return this.endRunningApplication(appId);
    };
    /**
     * Uninstalls the app from the emulator.
     */
    IOSEmulatorManager.prototype.uninstallApplication = function (appId) {
        return testUtil_1.TestUtil.getProcessOutput("xcrun simctl uninstall booted " + appId).then(function () { return null; });
    };
    return IOSEmulatorManager;
}());
exports.IOSEmulatorManager = IOSEmulatorManager;

function commandWithCheckAppExistence(command, appId) {
    return testUtil_1.TestUtil.getProcessOutput("adb shell pm list packages", { noLogCommand: true, noLogStdOut: true, noLogStdErr: true })
        .then((output) => {
            return output.includes(appId);
        }).then((isAppExist) => {
            if (isAppExist) {
                return testUtil_1.TestUtil.getProcessOutput(`${command} ${appId}`).then(function () { return null; });
            }
            console.log(`Command "${command}" is skipped because the application has not yet been installed`)
            return null;
        });
}
