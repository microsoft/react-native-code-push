var fs = require("fs");
var glob = require("glob");
var inquirer = require('inquirer');
var path = require("path");
var plist = require("plist");
var package = require('../../../../../package.json');

var ignoreNodeModules = { ignore: "node_modules/**" };
var appDelegatePaths = glob.sync("**/AppDelegate.m", ignoreNodeModules);

// Fix for https://github.com/Microsoft/react-native-code-push/issues/477
// Typical location of AppDelegate.m for newer RN versions: $PROJECT_ROOT/ios/<project_name>/AppDelegate.m
// Let's try to find that path by filtering the whole array for any path containing <project_name>
// If we can't find it there, play dumb and pray it is the first path we find.
var appDelegatePath = findFileByAppName(appDelegatePaths, package ? package.name : null) || appDelegatePaths[0];

// Glob only allows foward slashes in patterns: https://www.npmjs.com/package/glob#windows
var plistPath = glob.sync(path.join(path.dirname(appDelegatePath), "*Info.plist").replace(/\\/g, "/"), ignoreNodeModules)[0];

var appDelegateContents = fs.readFileSync(appDelegatePath, "utf8");
var plistContents = fs.readFileSync(plistPath, "utf8");

// 1. Add the header import statement
var codePushHeaderImportStatement = `#import "CodePush.h"`;
if (~appDelegateContents.indexOf(codePushHeaderImportStatement)) {
    console.log(`"CodePush.h" header already imported.`);
} else {
    var appDelegateHeaderImportStatement = `#import "AppDelegate.h"`;
    appDelegateContents = appDelegateContents.replace(appDelegateHeaderImportStatement,
        `${appDelegateHeaderImportStatement}\n${codePushHeaderImportStatement}`);
}

// 2. Modify jsCodeLocation value assignment
var oldJsCodeLocationAssignmentStatement = appDelegateContents.match(/(jsCodeLocation = .*)/)[1];
var newJsCodeLocationAssignmentStatement = "jsCodeLocation = [CodePush bundleURL];";
if (~appDelegateContents.indexOf(newJsCodeLocationAssignmentStatement)) {
    console.log(`"jsCodeLocation" already pointing to "[CodePush bundleURL]".`);
} else {
    var jsCodeLocationPatch = `
#ifdef DEBUG
    ${oldJsCodeLocationAssignmentStatement}
#else
    ${newJsCodeLocationAssignmentStatement}
#endif`;
    appDelegateContents = appDelegateContents.replace(oldJsCodeLocationAssignmentStatement,
        jsCodeLocationPatch);
}

// 3. Add CodePushDeploymentKey to plist file
var parsedInfoPlist = plist.parse(plistContents);
if (parsedInfoPlist.CodePushDeploymentKey) {
    console.log(`"CodePushDeploymentKey" already specified in the plist file.`);
    writePatches();
} else {
    inquirer.prompt({
        "type": "input",
        "name": "iosDeploymentKey",
        "message": "What is your CodePush deployment key for iOS (hit <ENTER> to ignore)"
    }).then(function(answer) {
        parsedInfoPlist.CodePushDeploymentKey = answer.iosDeploymentKey || "deployment-key-here";
        plistContents = plist.build(parsedInfoPlist);

        writePatches();
    });
}

function writePatches() {
    fs.writeFileSync(appDelegatePath, appDelegateContents);
    fs.writeFileSync(plistPath, plistContents);
}

// Helper that filters an array with AppDelegate.m paths for a path with the app name inside it
// Should cover nearly all cases
function findFileByAppName(array, appName) {
    if (array.length === 0 || !appName) return null;

    for (var i = 0; i < array.length; i++) {
        var path = array[i];
        if (path && path.indexOf(appName) !== -1) {
            return path;
        }
    }

    return null;
}
