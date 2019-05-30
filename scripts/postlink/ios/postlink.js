
var linkTools = require('../../tools/linkToolsIos');
var fs = require("fs");
var inquirer = require('inquirer');
var plist = require("plist");
var semver = require('semver');

var package = require('../../../../../package.json');

module.exports = () => {

    console.log("Running ios postlink script");

    var appDelegatePath = linkTools.getAppDeletePath();

    if (!appDelegatePath) {
        return Promise.reject(`Couldn't find AppDelegate. You might need to update it manually \
    Please refer to plugin configuration section for iOS at \
    https://github.com/microsoft/react-native-code-push#plugin-configuration-ios`);
    }

    var appDelegateContents = fs.readFileSync(appDelegatePath, "utf8");

    // 1. Add the header import statement
    if (~appDelegateContents.indexOf(linkTools.codePushHeaderImportStatement)) {
        console.log(`"CodePush.h" header already imported.`);
    } else {
        var appDelegateHeaderImportStatement = `#import "AppDelegate.h"`;
        appDelegateContents = appDelegateContents.replace(appDelegateHeaderImportStatement,
            `${appDelegateHeaderImportStatement}${linkTools.codePushHeaderImportStatementFormatted}`);
    }

    // 2. Modify jsCodeLocation value assignment
    var reactNativeVersion = package && package.dependencies && package.dependencies["react-native"];

    if (!reactNativeVersion) {
        console.log(`Can't take react-native version from package.json`);
    } else if (semver.gte(semver.coerce(reactNativeVersion), "0.59.0")) {
        var oldBundleUrl = linkTools.oldBundleUrl;
        var codePushBundleUrl = linkTools.codePushBundleUrl;

        if (~appDelegateContents.indexOf(codePushBundleUrl)) {
            console.log(`"BundleUrl" already pointing to "[CodePush bundleURL]".`);
        } else {
            if (~appDelegateContents.indexOf(oldBundleUrl)) {
                appDelegateContents = appDelegateContents.replace(oldBundleUrl, codePushBundleUrl);
            } else {
                console.log(`AppDelegate isn't compatible for linking`);
            }
        }
    } else {
        var jsCodeLocations = appDelegateContents.match(/(jsCodeLocation = .*)/g);

        if (!jsCodeLocations) {
            console.log('Couldn\'t find jsCodeLocation setting in AppDelegate.');
        }

        var newJsCodeLocationAssignmentStatement = linkTools.codePushGradleLink;
        if (~appDelegateContents.indexOf(newJsCodeLocationAssignmentStatement)) {
            console.log(`"jsCodeLocation" already pointing to "[CodePush bundleURL]".`);
        } else {
            if (jsCodeLocations.length === 1) {
                // If there is one `jsCodeLocation` it means that react-native app version is not the 0.57.8 or 0.57.0 and lower than 0.59 
                // and we should replace this line with DEBUG ifdef statement and add CodePush call for Release case

                var oldJsCodeLocationAssignmentStatement = jsCodeLocations[0];
                var jsCodeLocationPatch = linkTools.getJsCodeLocationPatch(oldJsCodeLocationAssignmentStatement);
                appDelegateContents = appDelegateContents.replace(oldJsCodeLocationAssignmentStatement,
                    jsCodeLocationPatch);
            } else if (jsCodeLocations.length === 2) {
                // If there are two `jsCodeLocation` it means that react-native app version is higher than 0.57.8 or equal
                // and we should replace the second one(Release case) with CodePush call

                appDelegateContents = appDelegateContents.replace(jsCodeLocations[1],
                    newJsCodeLocationAssignmentStatement);
            } else {
                console.log(`AppDelegate isn't compatible for linking`);
            }
        }
    }

    var plistPath = linkTools.getPlistPath();

    if (!plistPath) {
        return Promise.reject(`Couldn't find .plist file. You might need to update it manually \
    Please refer to plugin configuration section for iOS at \
    https://github.com/microsoft/react-native-code-push#plugin-configuration-ios`);
    }

    var plistContents = fs.readFileSync(plistPath, "utf8");

    // 3. Add CodePushDeploymentKey to plist file
    var parsedInfoPlist = plist.parse(plistContents);
    if (parsedInfoPlist.CodePushDeploymentKey) {
        console.log(`"CodePushDeploymentKey" already specified in the plist file.`);
        writePatches();
        return Promise.resolve();
    } else {
        return inquirer.prompt({
            "type": "input",
            "name": "iosDeploymentKey",
            "message": "What is your CodePush deployment key for iOS (hit <ENTER> to ignore)"
        }).then(function(answer) {
            parsedInfoPlist.CodePushDeploymentKey = answer.iosDeploymentKey || "deployment-key-here";
            plistContents = plist.build(parsedInfoPlist);

            writePatches();
            return Promise.resolve();
        });
    }

    function writePatches() {
        fs.writeFileSync(appDelegatePath, appDelegateContents);
        fs.writeFileSync(plistPath, plistContents);
    }
}
